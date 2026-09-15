package kr.co.donghyun.flamelauncher.presentation.crashreport

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kr.co.donghyun.flamelauncher.data.instance.InstanceManager
import kr.co.donghyun.flamelauncher.data.jvm.JvmSettingsManager
import kr.co.donghyun.flamelauncher.domain.repository.InstanceDetailRepository
import kr.co.donghyun.flamelauncher.domain.usecase.GetAuthStateUseCase
import kr.co.donghyun.flamelauncher.presentation.util.crash.CrashLogParser
import kr.co.donghyun.flamelauncher.presentation.util.mods.FlameCommunityApi
import java.io.File
import javax.inject.Inject

/**
 * CrashReportScreen ViewModel.
 * ⚠️ 소규모 진단 화면이라 domain.repository 계층 없이 CrashLogParser 를 직접 사용한다
 * (재사용/테스트 대체 필요성이 낮은 화면에는 Google 가이드도 이 정도 단순화를 허용).
 */
@HiltViewModel
class CrashReportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAuthStateUseCase: GetAuthStateUseCase,
    private val instanceDetailRepository: InstanceDetailRepository,
) : ViewModel() {

    private val _logPath = MutableStateFlow("")
    val logPath: StateFlow<String> = _logPath.asStateFlow()

    private val _logContent = MutableStateFlow("")
    val logContent: StateFlow<String> = _logContent.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _suspects = MutableStateFlow<List<CrashLogParser.SuspectMod>>(emptyList())
    val suspects: StateFlow<List<CrashLogParser.SuspectMod>> = _suspects.asStateFlow()

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _shareResultEvents = Channel<String>(Channel.BUFFERED)
    val shareResultEvents = _shareResultEvents.receiveAsFlow()

    private lateinit var modsDir: File
    private lateinit var instanceDir: File
    private var initialized = false

    fun initialize(instanceDir: File) {
        if (initialized) return
        initialized = true
        this.instanceDir = instanceDir
        modsDir = File(instanceDir, "mods")
        viewModelScope.launch(Dispatchers.IO) { loadLatestLog(instanceDir) }
    }

    private fun loadLatestLog(instanceDir: File) {
        val crashDir = File(instanceDir, "crash-reports")
        val latestCrash = crashDir.listFiles()
            ?.filter { it.extension == "txt" }
            ?.maxByOrNull { it.lastModified() }

        if (latestCrash == null) {
            _logPath.value = ""
            _logContent.value = ""
            _suspects.value = emptyList()
            _isLoading.value = false
            return
        }

        _logPath.value = latestCrash.absolutePath
        val content = try {
            latestCrash.readText()
        } catch (e: Exception) {
            "로그 파일을 읽는 중 오류가 발생했습니다: ${e.message}"
        }
        _logContent.value = content
        _suspects.value = try {
            CrashLogParser.parseSuspects(content, modsDir)
        } catch (_: Exception) {
            emptyList()
        }
        _isLoading.value = false
    }

    fun toggleMod(jarName: String, enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = CrashLogParser.toggleMod(modsDir, jarName, enable)
            if (ok) {
                _suspects.value = try {
                    CrashLogParser.parseSuspects(_logContent.value, modsDir)
                } catch (_: Exception) {
                    _suspects.value
                }
            }
        }
    }

    /**
     * 이 크래시 로그를 FlameShares 커뮤니티에 공유한다 — 요청사항대로 로그뿐 아니라
     * 이 모드팩(인스턴스)을 실행한 환경 전체(기기/렌더러/MC버전/로더/Java/힙 등)도
     * 함께 올려서, 다른 사람이 같은 문제를 겪었는지·원인이 뭔지 판단하기 쉽게 한다.
     */
    fun shareLogToCommunity(title: String) {
        val session = getAuthStateUseCase()
        val author = session?.username ?: "익명"
        val avatarUrl = if (session != null) {
            "https://crafatar.com/avatars/${session.uuid}?size=64&overlay"
        } else {
            "https://crafatar.com/avatars/MHF_Steve?size=64"
        }

        _isSharing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val instanceIdGuess = instanceDir.name
                val meta = runCatching {
                    InstanceManager.loadMeta(InstanceManager.instanceDir(context, instanceIdGuess))
                }.getOrNull()
                val rendererId = runCatching {
                    instanceDetailRepository.getRendererId(instanceIdGuess)
                }.getOrNull()
                val jvm = JvmSettingsManager.load(context)

                val environment = mapOf(
                    "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "android" to "Android ${Build.VERSION.RELEASE}",
                    "cpu_abi" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"),
                    "renderer" to (rendererId ?: "unknown"),
                    "mc_version" to (meta?.mcVersion ?: "unknown"),
                    "loader" to (meta?.loaderType ?: "vanilla"),
                    "heap_mb" to "${jvm.maxHeapMb}",
                )

                val ok = FlameCommunityApi.createPost(
                    author = author,
                    authorUuid = session?.uuid,
                    avatarUrl = avatarUrl,
                    title = title.ifBlank { "크래시 로그 공유" },
                    content = null,
                    logContent = _logContent.value,
                    environment = environment,
                )
                _isSharing.value = false
                _shareResultEvents.send(
                    if (ok) "커뮤니티에 공유했어요!" else "공유에 실패했어요. 네트워크 상태를 확인해 주세요."
                )
            } catch (e: Exception) {
                _isSharing.value = false
                _shareResultEvents.send("공유 실패: ${e.message}")
            }
        }
    }
}
