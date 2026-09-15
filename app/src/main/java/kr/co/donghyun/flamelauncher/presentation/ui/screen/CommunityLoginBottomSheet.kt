package kr.co.donghyun.flamelauncher.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.donghyun.flamelauncher.presentation.ui.theme.BgSurface
import kr.co.donghyun.flamelauncher.presentation.ui.theme.Flame
import kr.co.donghyun.flamelauncher.presentation.ui.theme.TextPrimary
import kr.co.donghyun.flamelauncher.presentation.ui.theme.TextSecondary
import kr.co.donghyun.flamelauncher.presentation.util.mods.FlameCommunityAuthApi

/**
 * FlameShares(웹) 커뮤니티 로그인 — 웹에서 발급한 6자리 코드를 여기서 입력하면,
 * 앱이 이미 갖고 있는 진짜 Minecraft 로그인 정보(정식 런처 클라이언트 ID로 인증됨)로
 * 그 코드를 인증 완료 처리한다. 별도 화면 전환 없이 바텀시트로 뜬다(앱 UI 통합).
 *
 * ⚠️ 왜 이 방식인가 — Xbox Live/Minecraft Services API를 웹 브라우저에서 직접
 * 호출하면 (1) 새로 만든 Azure 앱은 Minecraft API 사용 권한을 별도 신청해야 하고
 * 즉시 승인도 안 되고, (2) 이 API들이 브라우저 CORS를 지원한다는 근거가 없어
 * fetch() 자체가 막힐 수 있다. 이미 정식 클라이언트 ID로 인증된 이 앱의 세션을
 * 그대로 재사용하는 게 훨씬 안전하고 확실하다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityLoginBottomSheet(
    sheetState: SheetState,
    username: String?,
    uuid: String?,
    onDismiss: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BgSurface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                "🔥 FlameShares 커뮤니티 로그인",
                color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                if (username != null) "$username 계정으로 인증합니다" else "먼저 Minecraft 계정으로 로그인해 주세요",
                color = TextSecondary, fontSize = 11.sp
            )

            OtpCodeInput(
                value = code,
                onValueChange = { code = it },
                length = 6,
                enabled = username != null && uuid != null && !busy,
            )

            resultMessage?.let {
                Text(it, color = TextSecondary, fontSize = 11.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss, enabled = !busy) {
                    Text("닫기", color = TextSecondary)
                }
                if (busy) {
                    CircularProgressIndicator(color = Flame, modifier = Modifier.padding(8.dp).height(20.dp))
                } else {
                    TextButton(
                        enabled = code.length == 6 && username != null && uuid != null,
                        onClick = {
                            busy = true
                            resultMessage = null
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    FlameCommunityAuthApi.verifyLoginCode(
                                        code = code, username = username!!, uuid = uuid!!,
                                    )
                                }
                                busy = false
                                if (ok) {
                                    resultMessage = "✅ 인증 완료! 웹사이트로 돌아가 주세요."
                                } else {
                                    resultMessage = "인증에 실패했어요 — 코드를 다시 확인해 주세요."
                                }
                            }
                        }
                    ) {
                        Text("인증하기", color = Flame, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
