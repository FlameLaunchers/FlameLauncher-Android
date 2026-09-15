package kr.co.donghyun.flamelauncher.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.donghyun.flamelauncher.presentation.ui.theme.BgBorder
import kr.co.donghyun.flamelauncher.presentation.ui.theme.BgSurface
import kr.co.donghyun.flamelauncher.presentation.ui.theme.Flame
import kr.co.donghyun.flamelauncher.presentation.ui.theme.TextPrimary

/**
 * 이메일/문자 인증코드 입력에서 흔히 보는 "칸 하나에 숫자 하나" 스타일 입력.
 * - 숫자를 입력하면 자동으로 다음 칸으로 포커스 이동
 * - 백스페이스로 빈 칸에서 지우면 이전 칸으로 포커스 이동 + 그 칸도 지움
 * - 어느 칸에든 6자리 전체를 붙여넣으면 자동으로 모든 칸에 나눠 채워짐
 *
 * 부모는 이 컴포넌트가 조립한 전체 문자열(String) 하나만 알면 되고, 칸별 포커스/
 * 붙여넣기 분배 같은 세부사항은 전부 이 컴포넌트 내부에서 처리한다.
 */
@Composable
fun OtpCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    length: Int = 6,
    enabled: Boolean = true,
) {
    val focusRequesters = remember { List(length) { FocusRequester() } }
    val scope = rememberCoroutineScope()
    // 마지막 칸까지 다 채워졌을 때 포커스를 어디로 보낼지 알기 위해 필요.
    var lastFocusedIndex by remember { mutableStateOf(0) }

    fun digitAt(index: Int): String = value.getOrNull(index)?.toString() ?: ""

    fun focusIndex(index: Int) {
        if (index in focusRequesters.indices) {
            scope.launch {
                // ⚠️ 같은 프레임에서 바로 requestFocus 하면 이전 포커스 변경/키보드
                //   전환과 겹쳐서 씹히는 경우가 있어 한 틱 미룬다.
                delay(1)
                focusRequesters[index].requestFocus()
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(length) { index ->
            val digit = digitAt(index)
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgSurface)
                    .border(
                        width = 1.5.dp,
                        // 이 칸에 숫자가 채워지면 primary(Flame) 로 확실히 강조,
                        //   비어있을 땐 은은한 기본 테두리색으로 대비를 준다.
                        color = if (digit.isNotEmpty()) Flame else BgBorder,
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BasicTextField(
                    value = digit,
                    onValueChange = { newText ->
                        val digitsOnly = newText.filter(Char::isDigit)
                        when {
                            // 붙여넣기 등으로 여러 글자가 한 번에 들어온 경우 — 이 칸부터
                            //   시작해서 전체 코드를 재조립하고, 채워진 마지막 칸으로 이동.
                            digitsOnly.length > 1 -> {
                                val before = value.take(index)
                                val combined = (before + digitsOnly).take(length)
                                onValueChange(combined)
                                focusIndex((combined.length - 1).coerceIn(0, length - 1))
                            }
                            // 한 글자 입력 — 이 칸을 채우고 다음 칸으로 이동.
                            digitsOnly.length == 1 -> {
                                val chars = value.padEnd(length, ' ').toCharArray()
                                chars[index] = digitsOnly[0]
                                onValueChange(String(chars).trimEnd())
                                if (index < length - 1) focusIndex(index + 1)
                            }
                            // 지움 — 이 칸만 비움(백스페이스로 "이미 빈 칸"을 지우는 경우는
                            //   아래 onKeyEvent 에서 이전 칸 이동까지 함께 처리).
                            else -> {
                                if (index < value.length) {
                                    val chars = value.padEnd(length, ' ').toCharArray()
                                    chars[index] = ' '
                                    onValueChange(String(chars).trimEnd())
                                }
                            }
                        }
                    },
                    enabled = enabled,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary, fontSize = 20.sp, textAlign = TextAlign.Center,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    ),
                    cursorBrush = SolidColor(Flame),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .onFocusChanged { if (it.isFocused) lastFocusedIndex = index }
                        .onKeyEvent { keyEvent ->
                            // 이미 빈 칸에서 백스페이스 → 이전 칸으로 이동해서 그 칸도 지움.
                            if (keyEvent.key == Key.Backspace && digit.isEmpty() && index > 0) {
                                val chars = value.padEnd(length, ' ').toCharArray()
                                if (index - 1 < chars.size) chars[index - 1] = ' '
                                onValueChange(String(chars).trimEnd())
                                focusIndex(index - 1)
                                true
                            } else {
                                false
                            }
                        },
                )
            }
        }
    }
}
