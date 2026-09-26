package kr.co.donghyun.flamelauncher.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.co.donghyun.flamelauncher.presentation.ui.components.HtmlWebView
import kr.co.donghyun.flamelauncher.presentation.ui.theme.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * 릴리스 목록을 띄우는 업데이트 노트 창 — 깃허브 릴리스 탭과 같은 내용이다.
 *
 * ⚠️ 예전에는 저장소 README 를 띄웠는데, 그 저장소(FlameLaunchers/FlameLauncher)가 비공개로
 *    바뀌면서 익명 요청이 404 를 받아 창이 비었다. 애초에 "업데이트 노트" 에 어울리는 건
 *    README 가 아니라 릴리스 노트다.
 *
 * ⚠️ 마크다운은 직접 렌더링하지 않는다. GitHub 의 마크다운 API 에 본문을 넘기면 렌더링된
 *    HTML 을 주므로 표·코드블록까지 같은 모양으로 나온다. 앱에 파서를 들고 다닐 이유가 없다.
 */
private const val RELEASES_API =
    "https://api.github.com/repos/FlameLaunchers/FlameLauncher-Android/releases?per_page=10"
private const val MARKDOWN_API = "https://api.github.com/markdown/raw"

/** 앱이 살아있는 동안 한 번만 받는다. 화면을 오갈 때마다 다시 받을 이유가 없다. */
private var cachedHtml: String? = null

@Composable
fun ReleaseNotesPane(modifier: Modifier = Modifier) {
    var html by remember { mutableStateOf(cachedHtml) }
    var failed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        if (html != null) return@LaunchedEffect
        failed = false
        val loaded = withContext(Dispatchers.IO) { fetchReadmeHtml() }
        if (loaded != null) {
            cachedHtml = loaded
            html = loaded
        } else {
            failed = true
        }
    }

    Box(modifier = modifier.fillMaxSize().background(BgDark)) {
        when {
            html != null -> Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                HtmlWebView(
                    html = html!!,
                    // 상대 경로 이미지(README 의 배지·스크린샷)를 풀어줄 기준.
                    baseUrl = "https://raw.githubusercontent.com/FlameLaunchers/FlameLauncher/main/",
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                )
            }

            failed -> Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📡", fontSize = 40.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "업데이트 노트를 불러오지 못했어요",
                    color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "네트워크를 확인하고 다시 시도해 주세요.",
                    color = TextSecondary, fontSize = 11.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "다시 시도",
                    color = FlamePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .background(BgItem)
                        .clickable { reloadKey++ }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }

            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FlamePrimary)
            }
        }
    }
}

/** 릴리스 10개를 받아 "버전 · 날짜 · 본문" 을 이어붙인 마크다운을 HTML 로 만든다. */
private fun fetchReadmeHtml(): String? = try {
    val json = httpGet(RELEASES_API, "application/vnd.github+json")
    val releases = com.google.gson.JsonParser.parseString(json).asJsonArray
    val markdown = buildString {
        for (el in releases) {
            val o = el.asJsonObject
            if (o["draft"]?.asBoolean == true) continue
            val tag = o["tag_name"]?.asString ?: continue
            val name = o["name"]?.asString?.takeIf { it.isNotBlank() } ?: tag
            val date = o["published_at"]?.asString?.take(10) ?: ""
            val pre = if (o["prerelease"]?.asBoolean == true) " · pre-release" else ""
            append("## ").append(name).append('\n')
            append('`').append(tag).append("` · ").append(date).append(pre).append("\n\n")
            append(o["body"]?.asString?.trim().orEmpty()).append("\n\n---\n\n")
        }
    }
    if (markdown.isBlank()) null else renderMarkdown(markdown)
} catch (_: Exception) {
    null
}

/** GitHub 의 마크다운 렌더러. 실패하면 원문을 <pre> 로라도 보여준다. */
private fun renderMarkdown(markdown: String): String = try {
    (URL(MARKDOWN_API).openConnection() as HttpURLConnection).run {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "text/x-markdown")
        setRequestProperty("User-Agent", "FlameLauncher")
        connectTimeout = 10_000
        readTimeout = 10_000
        outputStream.use { it.write(markdown.toByteArray(Charsets.UTF_8)) }
        if (responseCode == 200) inputStream.bufferedReader().use { it.readText() }
        else "<pre>" + markdown.replace("<", "&lt;") + "</pre>"
    }
} catch (_: Exception) {
    "<pre>" + markdown.replace("<", "&lt;") + "</pre>"
}

private fun httpGet(url: String, accept: String): String =
    (URL(url).openConnection() as HttpURLConnection).run {
        setRequestProperty("Accept", accept)
        setRequestProperty("User-Agent", "FlameLauncher")
        connectTimeout = 10_000
        readTimeout = 10_000
        inputStream.bufferedReader().use { it.readText() }
    }
