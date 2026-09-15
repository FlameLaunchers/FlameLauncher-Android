package kr.co.donghyun.flamelauncher.presentation.instancesettings

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kr.co.donghyun.flamelauncher.data.renderer.Renderer
import kr.co.donghyun.flamelauncher.domain.model.InstalledMod
import kr.co.donghyun.flamelauncher.domain.repository.InstanceDetailRepository
import kr.co.donghyun.flamelauncher.domain.usecase.GetAuthStateUseCase
import kr.co.donghyun.flamelauncher.presentation.util.maps.MapImporter
import kr.co.donghyun.flamelauncher.presentation.util.mods.ModImporter
import kr.co.donghyun.flamelauncher.presentation.util.mods.FlameSharesApi
import kr.co.donghyun.flamelauncher.presentation.util.mods.ModpackExporter
import kr.co.donghyun.flamelauncher.presentation.util.mods.ModpackImporter
import java.io.File
import javax.inject.Inject

/**
 * InstanceSettingsScreen ViewModel.
 * ⚠️ 이 화면은 도메인 UseCase 계층을 따로 만들지 않고 InstanceDetailRepository 를 직접
 * 주입받는다 — 여러 리포지토리를 조합하는 비즈니스 로직이 없어서(단순 CRUD성 위임)
 * UseCase 를 하나씩 만드는 비용 대비 실익이 낮다고 판단(Google 가이드도 이 경우 선택사항).
 * SAF 피커(ActivityResultLauncher) 자체는 여전히 Activity 몫이라 ViewModel엔 없다.
 *
 * ⚠️ instanceId 는 SavedStateHandle 대신 명시적 initialize() 로 받는다 — 일반
 * ComponentActivity 의 Intent extra 는 SavedStateHandle 에 자동으로 안 들어오기 때문
 * (Nav Graph 인자가 아니라서). Activity 가 onCreated() 에서 딱 한 번 호출한다.
 */
@HiltViewModel
class InstanceSettingsViewModel @Inject constructor(
    private val repository: InstanceDetailRepository,
    private val getAuthStateUseCase: GetAuthStateUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        /** FlameShares(Supabase 무료 티어) 업로드 파일 크기 제한. */
        const val MAX_UPLOAD_SIZE_MB = 50
    }

    var instanceId: String = ""
        private set
    var instanceName: String = ""
        private set
    private var initialized = false

    private val _installedMods = MutableStateFlow<List<InstalledMod>>(emptyList())
    val installedMods: StateFlow<List<InstalledMod>> = _installedMods.asStateFlow()

    private val _rendererId = MutableStateFlow<String?>(null)
    val rendererId: StateFlow<String?> = _rendererId.asStateFlow()

    private val _loaderLabel = MutableStateFlow<String?>(null)
    val loaderLabel: StateFlow<String?> = _loaderLabel.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    // 가져오기/내보내기 "완료" 결과 메시지 — 1회성 이벤트(Toast 용).
    // statusMessage(StateFlow) 는 진행 다이얼로그 문구도 겸하다 보니 "완료됐는지"를
    // 문자열로 구분하기 애매해서, 완료 시점에만 별도로 흘려보낸다.
    private val _resultEvents = Channel<String>(Channel.BUFFERED)
    val resultEvents = _resultEvents.receiveAsFlow()

    /** Activity.onCreated() 에서 인텐트 extra 를 받은 직후 딱 한 번 호출. */
    fun initialize(instanceId: String, instanceName: String) {
        if (initialized) return
        initialized = true
        this.instanceId = instanceId
        this.instanceName = instanceName
        _loaderLabel.value = repository.detectLoaderLabel(instanceId)
        _rendererId.value = repository.getRendererId(instanceId)
        refreshMods()
    }

    fun refreshMods() {
        _installedMods.value = repository.getInstalledMods(instanceId)
    }

    fun deleteMod(fileName: String) {
        repository.deleteMod(instanceId, fileName)
        refreshMods()
    }

    fun selectRenderer(id: String?): String {
        repository.setRendererId(instanceId, id)
        _rendererId.value = id
        return id?.let { Renderer.fromId(it).displayName } ?: "전역 기본"
    }

    fun hasExportableMods(): Boolean = repository.hasExportableMods(instanceId)

    fun importMap(zipUri: Uri) {
        _statusMessage.value = "맵을 가져오는 중…"
        _importing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.importMap(instanceId, zipUri)
            _importing.value = false
            _resultEvents.send(when (result) {
                is MapImporter.Result.Success -> "'${result.worldName}' 맵을 가져왔습니다 (${result.fileCount}개 파일)"
                is MapImporter.Result.Failure -> result.reason
            })
        }
    }

    fun importMods(jarUris: List<Uri>) {
        _statusMessage.value = "모드를 추가하는 중…"
        _importing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.importMods(instanceId, jarUris)
            _importing.value = false
            _resultEvents.send(when (result) {
                is ModImporter.Result.Success -> buildString {
                    append("모드 ${result.added.size}개 추가됨")
                    if (result.skipped.isNotEmpty()) append(" · ${result.skipped.size}개 건너뜀")
                }
                is ModImporter.Result.Failure -> result.reason
            })
            refreshMods()
        }
    }

    fun importModpack(zipUri: Uri) {
        _statusMessage.value = "모드팩을 가져오는 중…"
        _importing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.importModpack(instanceId, zipUri)
            _importing.value = false
            _resultEvents.send(when (result) {
                is ModpackImporter.Result.Success -> buildString {
                    append("모드 ${result.modCount}개")
                    if (result.configCount > 0) append(" · 설정 ${result.configCount}개")
                    append(" 가져옴")
                    if (result.failedDownloads > 0) {
                        append("\n⚠️ ${result.failedDownloads}개 모드는 다운로드에 실패했어요(네트워크 상태를 확인하고 다시 시도해 주세요).")
                    }
                    if (result.mcMismatch) {
                        val packMc = result.manifest?.mcVersion ?: "?"
                        append("\n주의: 모드팩 버전($packMc)이 이 인스턴스와 달라 작동하지 않을 수 있어요.")
                    }
                }
                is ModpackImporter.Result.Failure -> result.reason
            })
            refreshMods()
        }
    }

    fun exportModpack(outputUri: Uri) {
        _statusMessage.value = "모드팩으로 추출하는 중…"
        _importing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.exportModpack(instanceId, instanceName, outputUri)
            _importing.value = false
            _resultEvents.send(when (result) {
                is ModpackExporter.Result.Success -> buildString {
                    append("모드 ${result.modCount}개")
                    if (result.configCount > 0) append(" · 설정 ${result.configCount}개")
                    append("를 모드팩으로 추출했습니다.")
                }
                is ModpackExporter.Result.Failure -> result.reason
            })
        }
    }

    /**
     * FlameShares(웹)에 공유하기 — 모드팩을 Supabase(Storage + Table)에 직접 업로드한다.
     * 작성자는 현재 로그인된 Minecraft(Microsoft) 계정의 닉네임을 쓰고, 아바타는
     * Crafatar(공개 스킨 렌더링 API)로 UUID 기반 얼굴 이미지를 생성한다.
     *
     * ⚠️ GitHub 대신 Supabase 를 쓰는 이유 — GitHub API 는 "쓰기 권한이 있는 토큰"
     * 없이는 아무것도 못 하는데, 그 토큰을 APK 안에 넣으면 디컴파일로 누구나 추출해서
     * 저장소 전체를 망가뜨릴 수 있다. Supabase 의 anon(공개) key 는 애초에 "클라이언트에
     * 그대로 노출해도 되는" 용도로 설계됐고, 실제 권한은 서버 쪽 RLS 정책으로 제한한다
     * (여기선 "익명은 삽입(insert)만 가능, 수정/삭제 불가"로 걸어뒀음).
     */
    fun shareToWeb(description: String) {
        val session = getAuthStateUseCase()
        if (session == null) {
            viewModelScope.launch { _resultEvents.send("공유하려면 먼저 Minecraft 계정으로 로그인해야 해요.") }
            return
        }
        _statusMessage.value = "FlameShares에 업로드하는 중…"
        _importing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                val cacheDir = File(context.cacheDir, "share").apply { mkdirs() }
                val safeFileName = instanceName.replace(Regex("[^\\w가-힣 .-]"), "_")
                val outFile = File(cacheDir, "$safeFileName.zip")
                if (outFile.exists()) outFile.delete()
                tempFile = outFile
                val outUri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", outFile
                )
                val exportResult = repository.exportModpack(instanceId, instanceName, outUri)
                if (exportResult is ModpackExporter.Result.Failure) {
                    _importing.value = false
                    _resultEvents.send(exportResult.reason)
                    return@launch
                }

                // ⚠️ FlameShares(Supabase 무료 티어)는 파일당 50MB 제한이 있다.
                //   업로드를 시도한 뒤 서버에서 거부당하는 것보다, 여기서 먼저 확인해서
                //   명확한 이유와 함께 바로 알려주는 게 훨씬 낫다.
                val sizeMb = outFile.length() / 1024.0 / 1024.0
                if (sizeMb > MAX_UPLOAD_SIZE_MB) {
                    _importing.value = false
                    _resultEvents.send(
                        "모드팩이 너무 커요(${"%.1f".format(sizeMb)}MB) — " +
                            "FlameShares는 ${MAX_UPLOAD_SIZE_MB}MB 이하만 업로드할 수 있어요."
                    )
                    return@launch
                }

                val meta = runCatching {
                    kr.co.donghyun.flamelauncher.data.instance.InstanceManager.loadMeta(
                        kr.co.donghyun.flamelauncher.data.instance.InstanceManager.instanceDir(context, instanceId)
                    )
                }.getOrNull()

                // 포함된 모드 목록 — 이름별로 Modrinth 검색 링크를 만들어서 같이 올린다.
                //   (설치 경로가 CurseForge/Modrinth/직접 가져오기 등 다양해서 모드별
                //   정확한 원본 링크를 추적하는 대신, 이름 기반 검색 링크로 "찾아가기"
                //   가 가능하게 하는 실용적인 선택.)
                // 포함된 모드 목록 — 파일명 그대로(예: "jei-1.20.1-forge-15.2.0.27")를 검색어로
                //   쓰면 버전/로더 이름이 섞여서 검색이 거의 안 됐다. jar 안의 실제 메타데이터
                //   (모드가 스스로 선언한 진짜 이름)를 최우선으로 읽어서 검색어로 쓴다.
                val meta2 = runCatching {
                    kr.co.donghyun.flamelauncher.data.instance.InstanceManager.loadMeta(
                        kr.co.donghyun.flamelauncher.data.instance.InstanceManager.instanceDir(context, instanceId)
                    )
                }.getOrNull()
                val isLegacy = meta2?.mcVersion?.let {
                    kr.co.donghyun.flamelauncher.data.jvm.isLegacyVersion(it)
                } ?: false
                val instanceBaseDir = kr.co.donghyun.flamelauncher.data.instance.InstanceManager.instanceDir(context, instanceId)
                val modsDir = File(
                    if (isLegacy) File(instanceBaseDir, ".minecraft") else instanceBaseDir,
                    "mods"
                )
                // 포함된 모드 목록 — 이름만으로 검색 페이지 링크를 만들면 버전/로더가 다른
                //   동명이인 모드로 잘못 연결될 수 있다. Modrinth 검색 API에 같은 MC버전+
                //   로더로 필터링해서 물어보고, 결과가 있으면 그 모드로 바로 연결되는
                //   정확한 링크(project 페이지)를 쓰고, 없으면 기존처럼 검색 페이지로 폴백.
                //   모드 개수만큼 순차 호출하면 느려지니 병렬로 처리한다.
                val modrinthApi = kr.co.donghyun.flamelauncher.presentation.util.mods.ModrinthAPI()
                val enabledMods = repository.getInstalledMods(instanceId).filter { it.enabled }
                val modList = coroutineScope {
                    enabledMods.map { mod ->
                        async(Dispatchers.IO) {
                            val jarFile = File(modsDir, mod.fileName)
                            val realName = extractModDisplayName(jarFile) ?: cleanFileNameForSearch(mod.displayName)
                            val directUrl = runCatching {
                                modrinthApi.search(
                                    query = realName,
                                    projectType = "mod",
                                    gameVersion = meta2?.mcVersion ?: "",
                                    loader = meta2?.loaderType ?: "",
                                    limit = 1,
                                ).firstOrNull()?.slug?.let { slug -> "https://modrinth.com/mod/$slug" }
                            }.getOrNull()
                            mapOf(
                                "name" to realName,
                                "searchUrl" to (directUrl
                                    ?: "https://modrinth.com/mods?q=${java.net.URLEncoder.encode(realName, "UTF-8")}"),
                            )
                        }
                    }.map { it.await() }
                }

                val uploaded = FlameSharesApi.upload(
                    file = outFile,
                    name = instanceName,
                    author = session.username,
                    authorUuid = session.uuid,
                    avatarUrl = "https://crafatar.com/avatars/${session.uuid}?size=128&overlay",
                    description = description.ifBlank { null },
                    mcVersion = meta?.mcVersion ?: "알 수 없음",
                    loader = meta?.loaderType ?: "vanilla",
                    mods = modList,
                )
                _importing.value = false
                _resultEvents.send(
                    if (uploaded) "FlameShares에 업로드했어요! 웹사이트에서 바로 확인할 수 있어요."
                    else "업로드에 실패했어요. 네트워크 상태를 확인해 주세요."
                )
            } catch (e: Exception) {
                _importing.value = false
                _resultEvents.send("웹 공유 실패: ${e.message}")
            } finally {
                tempFile?.delete()
            }
        }
    }

    fun deleteInstance() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInstance(instanceId)
            _deleted.value = true
        }
    }

    /**
     * jar 안의 메타데이터(fabric.mod.json/quilt.mod.json 의 name, mods.toml 의
     * displayName, 구버전 mcmod.info 의 name)에서 모드가 스스로 선언한 진짜 이름을
     * 읽는다 — 파일명보다 훨씬 정확해서 검색(Modrinth 등)이 실제로 된다.
     */
    private fun extractModDisplayName(jarFile: File): String? {
        if (!jarFile.exists()) return null
        return try {
            java.util.zip.ZipFile(jarFile).use { zip ->
                for (entryName in listOf("fabric.mod.json", "quilt.mod.json")) {
                    zip.getEntry(entryName)?.let { entry ->
                        try {
                            val obj = com.google.gson.JsonParser.parseString(
                                zip.getInputStream(entry).bufferedReader().readText()
                            ).asJsonObject
                            val name = obj.get("name")?.asString
                                ?: obj.getAsJsonObject("quilt_loader")?.get("name")?.asString
                            if (!name.isNullOrBlank()) return name
                        } catch (_: Exception) { /* 다음 후보로 */ }
                    }
                }
                zip.getEntry("META-INF/mods.toml")?.let { entry ->
                    try {
                        val toml = zip.getInputStream(entry).bufferedReader().readText()
                        Regex("""displayName\s*=\s*"([^"]+)"""").find(toml)?.groupValues?.get(1)
                            ?.let { if (it.isNotBlank()) return it }
                    } catch (_: Exception) { /* 다음 후보로 */ }
                }
                zip.getEntry("mcmod.info")?.let { entry ->
                    try {
                        val arr = com.google.gson.JsonParser.parseString(
                            zip.getInputStream(entry).bufferedReader().readText()
                        ).asJsonArray
                        val name = arr.firstOrNull()?.asJsonObject?.get("name")?.asString
                        if (!name.isNullOrBlank()) return name
                    } catch (_: Exception) { /* 폴백으로 */ }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * jar 메타데이터를 못 읽었을 때의 최후 폴백 — 파일명에서 버전 번호·로더 이름·
     * 구분자를 최대한 제거해서 검색어로 쓸 만하게 정리한다(완벽하지는 않음).
     */
    private fun cleanFileNameForSearch(rawName: String): String {
        var name = rawName
        // 버전 번호 패턴 제거: -1.20.1, _v2.0.3, -15.2.0.27 등
        name = name.replace(Regex("""[-_]v?\d+(\.\d+){1,4}[a-zA-Z0-9.\-+]*"""), "")
        // 로더/마인크래프트 관련 키워드 제거
        name = name.replace(Regex("""(?i)\b(forge|neoforge|fabric|quilt|mc|minecraft)\b"""), "")
        // 남은 구분자를 공백으로, 중복 공백 정리
        name = name.replace(Regex("""[-_]+"""), " ").trim().replace(Regex("""\s+"""), " ")
        return name.ifBlank { rawName }
    }
}
