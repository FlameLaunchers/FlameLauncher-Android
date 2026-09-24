package kr.co.donghyun.flamelauncher.presentation.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.annotation.StringRes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.co.donghyun.flamelauncher.R
import kr.co.donghyun.flamelauncher.BuildConfig
import kr.co.donghyun.flamelauncher.data.instance.InstanceManager
import kr.co.donghyun.flamelauncher.data.instance.InstanceMeta
import kr.co.donghyun.flamelauncher.data.mojang.DownloadPhase
import kr.co.donghyun.flamelauncher.data.mojang.DownloadProgress
import kr.co.donghyun.flamelauncher.data.mojang.VersionEntry
import kr.co.donghyun.flamelauncher.presentation.ui.components.LoaderSelectDialog
import kr.co.donghyun.flamelauncher.presentation.ui.components.TerracottaEntry
import kr.co.donghyun.flamelauncher.presentation.ui.theme.*
import kr.co.donghyun.flamelauncher.presentation.util.isVersionSupported
import kr.co.donghyun.flamelauncher.presentation.util.window.isTablet
import kr.co.donghyun.flamelauncher.presentation.util.window.isCompact
import java.net.URL

/// 아이콘은 iOS 판(MainTab.icon)과 같은 모양이다:
/// internaldrive.fill · checkmark.seal.fill · square.grid.2x2.fill
enum class MainTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    INSTALLED(R.string.installed_label, HardDriveIcon),
    RELEASE(R.string.release_label, Icons.Filled.Verified),
    ALL(R.string.all_label, Icons.Filled.GridView),
}

/// Material Symbols "hard_drive"(Apache 2.0) — iOS 의 internaldrive.fill 과 같은 모양.
/// material-icons-extended 에는 없다(Storage 는 서버 랙 모양이라 다르다).
private val HardDriveIcon: ImageVector =
    ImageVector.Builder("HardDrive", 24.dp, 24.dp, 960f, 960f)
        .addGroup(translationY = 960f)   // 원본 viewBox 가 0 -960 960 960 이다
        .addPath(
            addPathNodes(
                "M680-320q25 0 42.5-17t17.5-43q0-25-17.5-42.5T680-440q-26 0-43 17.5T620-380q0 26 17 43t43 17Z" +
                "M80-600l136-136q11-11 25.5-17.5T273-760h413q17 0 31.5 6.5T743-736l137 136H80Z" +
                "m80 400q-34 0-57-23t-23-57v-240h800v240q0 34-23.5 57T800-200H160Z"
            ),
            fill = SolidColor(Color.Black),
        )
        .build()

/// 왼쪽 메뉴 항목. iOS 판과 같은 구성이다.
///
/// ⚠️ 예전에는 상단 배너에 아이콘 버튼을 늘어놓았다. 같은 곳으로 가는 입구가 두 개가 되고
///    (배너 + 메뉴) 화면이 좁아질수록 이름이 잘려서, 왼쪽 목록 하나로 합쳤다.
enum class MainSection(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    /// 오른쪽 본문에서 다루는가(아니면 다른 화면으로 넘어가는가).
    val showsInDetail: Boolean,
) {
    INSTANCES(R.string.section_instances_title, R.string.section_instances_subtitle,
        androidx.compose.material.icons.Icons.Filled.Dashboard, true),
    MODPACKS(R.string.section_modpacks_title, R.string.section_modpacks_subtitle,
        androidx.compose.material.icons.Icons.Filled.Inventory2, false),
    SETTINGS(R.string.section_settings_title, R.string.section_settings_subtitle,
        androidx.compose.material.icons.Icons.Filled.Tune, false),
    KEYBOARD(R.string.section_keyboard_title, R.string.section_keyboard_subtitle,
        androidx.compose.material.icons.Icons.Filled.Keyboard, false),
    NOTES(R.string.section_notes_title, R.string.section_notes_subtitle,
        androidx.compose.material.icons.Icons.AutoMirrored.Filled.Article, true),
}

@Composable
fun MainScreen(
    versions: List<VersionEntry>,
    instances: List<InstanceMeta>,
    progress: DownloadProgress,
    selectedVersion: VersionEntry?,
    isLoading: Boolean,
    onVersionSelect: (VersionEntry) -> Unit,
    onDownloadAndPlay: (VersionEntry) -> Unit,
    onLaunchFabric: (VersionEntry, String) -> Unit,
    onLaunchForge: (VersionEntry, String) -> Unit,
    onLaunchNeoForge: (VersionEntry, String) -> Unit,
    onLaunchInstance: (InstanceMeta) -> Unit,
    onOpenInstanceSettings: (InstanceMeta) -> Unit,
    onOpenContents: () -> Unit,
    onOpenNetworkSettings: () -> Unit,
    onOpenKeySettings: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRendererSettings: () -> Unit,
    uuid: String?,
    isLoggedIn: Boolean,
    username: String?,
    onLogin: () -> Unit,
    loginError: String? = null,
    launchingInstance: InstanceMeta? = null,
) {
    val isDownloading = progress.phase != DownloadPhase.IDLE &&
            progress.phase != DownloadPhase.DONE &&
            progress.phase != DownloadPhase.ERROR

    // 인스턴스가 있으면 INSTALLED 가 기본, 없으면 RELEASE
    var selectedTab by remember(instances.isEmpty()) {
        mutableStateOf(if (instances.isNotEmpty()) MainTab.INSTALLED else MainTab.RELEASE)
    }

    val filteredVersions = when (selectedTab) {
        MainTab.RELEASE -> versions.filter { it.type == "release" }
        MainTab.ALL     -> versions
        MainTab.INSTALLED -> emptyList()
    }

    var showLoaderDialog by remember { mutableStateOf(false) }
    var showTerracotta by remember { mutableStateOf(false) }
    val tablet = isTablet()
    val compact = isCompact()

    // INSTALLED 탭에서 사용자가 고른 인스턴스. 항목을 탭하면 선택되고,
    // 실행은 (태블릿) 우측 패널 / (폰) 하단 실행 바의 ▶ 버튼으로만 한다.
    var selectedSection by remember { mutableStateOf(MainSection.INSTANCES) }
    var selectedInstanceId by remember { mutableStateOf<String?>(null) }
    // 목록이 갱신돼 선택했던 인스턴스가 사라지면 선택 해제, 후보가 있으면 첫 항목을 기본 선택.
    LaunchedEffect(instances) {
        if (instances.none { it.id == selectedInstanceId }) {
            selectedInstanceId = instances.firstOrNull()?.id
        }
    }
    val selectedInstance = instances.firstOrNull { it.id == selectedInstanceId }

    // ⚠️ 하단바를 **그리는 조건과 자리를 비우는 조건이 같아야 한다.**
    //    예전에는 폰이면 무조건 56dp 를 비워 뒀는데, 설치됨 탭과 업데이트 노트에서는
    //    그 바를 그리지 않는다 — 실행 줄 아래에 빈 띠가 남아 버튼이 허공에 뜬 것처럼 보였다.
    // ⚠️ 태블릿도 이 바를 쓴다. 예전 태블릿 전용 SidePlayPanel 이 빠지면서 `!tablet` 만 남아,
    //    태블릿에서는 정식·전체 탭에 Play/진행 표시가 하나도 없어 설치 자체를 못 했다.
    val showBottomBar =
        selectedTab != MainTab.INSTALLED &&
        selectedSection != MainSection.NOTES

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = if (showBottomBar) 56.dp else 0.dp)) {
            if (tablet) {
                ProfileHeader(
                    isLoggedIn, username, uuid, onLogin,
                    onOpenContents, onOpenKeySettings, onOpenNetworkSettings, onOpenSettings, onOpenRendererSettings,
                    onOpenTerracotta = { showTerracotta = true },
                )
            } else {
                // ⚠️ 태블릿용 배너(그라데이션+아바타+텍스트버튼 여러 줄)를 그대로 축소해서
                //   쓰던 걸 완전히 버리고, 폰 전용으로 새로 설계한 컴팩트 앱바.
                //   리스트에 세로 공간을 최대한 내주는 게 목표라 56dp 로 고정.
                MobileTopBar(
                    isLoggedIn = isLoggedIn,
                    username = username,
                    uuid = uuid,
                    onLogin = onLogin,
                )
            }

            // ── 본문: 왼쪽 메뉴 | 오른쪽 내용 (iOS 판과 같은 2:4 분할) ──
            //
            // ⚠️ 앱 전체가 가로 고정이라 이 비율이 성립한다. 왼쪽이 1/6 이던 시절에는
            //    "정식 출…" 처럼 메뉴 이름이 전부 잘렸다.
            BoxWithConstraints(modifier = Modifier.fillMaxSize().weight(1f)) {
                val sidebarWidth = maxOf(maxWidth / 3, 210.dp)
                Row(modifier = Modifier.fillMaxSize()) {
                    SectionSidebar(
                        selected = selectedSection,
                        modifier = Modifier.width(sidebarWidth).fillMaxHeight(),
                        onSelect = { section ->
                            when (section) {
                                MainSection.MODPACKS -> onOpenContents()
                                MainSection.SETTINGS -> onOpenSettings()
                                MainSection.KEYBOARD -> onOpenKeySettings()
                                else -> selectedSection = section
                            }
                        },
                    )
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(BgBorder))

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (selectedSection == MainSection.NOTES) {
                            ReleaseNotesPane()
                        } else {
                            MainTabBar(
                                selected = selectedTab,
                                installedCount = instances.size,
                                releaseCount = versions.count { it.type == "release" },
                                allCount = versions.size,
                                onSelect = { selectedTab = it },
                            )
                            when {
                                isLoading && selectedTab != MainTab.INSTALLED ->
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = FlamePrimary)
                                    }
                                else -> Column(modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (selectedTab == MainTab.INSTALLED) {
                                            InstancesList(
                                                isLoggedIn = isLoggedIn,
                                                instances = instances,
                                                selectedInstanceId = selectedInstanceId,
                                                onSelect = { selectedInstanceId = it.id },
                                                onOpenSettings = onOpenInstanceSettings,
                                            )
                                        } else {
                                            VersionsList(filteredVersions, selectedVersion, onVersionSelect)
                                        }
                                    }
                                    // ⚠️ 정식·전체 탭에는 아래 띠를 두지 않는다. 거기서 고를 게
                                    //    없기 때문이다 — 실행 대상은 설치됨 탭이 갖는다.
                                    if (selectedTab == MainTab.INSTALLED) {
                                        InstalledPanel(
                                            selected = selectedInstance,
                                            isLoggedIn = isLoggedIn,
                                            onLaunch = { selectedInstance?.let { onLaunchInstance(it) } },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 폰: 태블릿의 BottomPanel/InstanceLaunchBar(각각 다른 모양)를 버리고,
        //   버전 탭에서 쓰는 얇은 단일 하단바. 설치됨 탭은 InstalledPanel 이 맡는다.
        if (showBottomBar) {
            MobileBottomBar(
                selectedTab = selectedTab,
                selectedVersion = selectedVersion,
                selectedInstance = selectedInstance,
                progress = progress,
                isDownloading = isDownloading,
                isLoggedIn = isLoggedIn,
                onPlayVersion = { showLoaderDialog = true },
                onPlayInstance = { selectedInstance?.let { onLaunchInstance(it) } },
                onLogin = onLogin,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (showLoaderDialog && selectedVersion != null) {
            LoaderSelectDialog(
                versionId = selectedVersion.id,
                onDismiss = { showLoaderDialog = false },
                onLaunchVanilla = { showLoaderDialog = false; onDownloadAndPlay(selectedVersion) },
                onLaunchFabric  = { v -> showLoaderDialog = false; onLaunchFabric(selectedVersion, v) },
                onLaunchForge   = { v -> showLoaderDialog = false; onLaunchForge(selectedVersion, v) },
                onLaunchNeoForge= { v -> showLoaderDialog = false; onLaunchNeoForge(selectedVersion, v) },
            )
        }

        // 온라인 LAN(Terracotta) — Activity 가 필요(VpnService.prepare/동의 런처)하므로
        //   LocalContext 에서 Activity 를 얻어 진입 Composable 을 띄운다.
        if (showTerracotta) {
            val ctx = LocalContext.current
            val activity = remember(ctx) {
                generateSequence(ctx) { (it as? android.content.ContextWrapper)?.baseContext }
                    .filterIsInstance<Activity>()
                    .firstOrNull()
            }
            if (activity != null) {
                TerracottaEntry(
                    activity = activity,
                    userName = username,
                    onClose = { showTerracotta = false }
                )
            } else {
                // Activity 를 못 찾는 비정상 상황 — 조용히 닫음
                showTerracotta = false
            }
        }

        // ── 게임 실행 중 팝업 ──
        // 실행을 누르면 표시되고, MinecraftActivity 로 전환돼 이 화면이 onPause 되면
        // Activity 가 launchingInstance 를 null 로 바꿔 자동으로 사라진다. (사용자 조작 불필요)
        if (launchingInstance != null) {
            LaunchingDialog(meta = launchingInstance, tablet = tablet)
        }
    }
}

/// 왼쪽 메뉴. 항목 하나가 한 줄을 다 쓰므로 이름이 잘리지 않는다.
@Composable
private fun SectionSidebar(
    selected: MainSection,
    modifier: Modifier = Modifier,
    onSelect: (MainSection) -> Unit,
) {
    Column(
        modifier = modifier
            .background(BgSurface)
            // ⚠️ 위쪽 여백은 두지 않는다 — 상단바 바로 아래라 이중으로 떠 보인다.
            .padding(horizontal = 10.dp)
            .padding(bottom = 10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MainSection.entries.forEach { section ->
            SectionRow(section = section, selected = section == selected) { onSelect(section) }
        }
    }
}

@Composable
private fun SectionRow(section: MainSection, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) FlameDark else BgItem)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) FlamePrimary else BgBorder,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            tint = if (selected) Color.White else FlamePrimary,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(section.titleRes),
                color = if (selected) Color.White else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(section.subtitleRes),
                color = if (selected) Color.White.copy(alpha = 0.75f) else TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/// 오른쪽 본문 위의 탭 — iOS 판과 같이 아이콘 · 이름 · 개수를 세로로 쌓는다.
@Composable
private fun MainTabBar(
    selected: MainTab,
    installedCount: Int,
    releaseCount: Int,
    allCount: Int,
    onSelect: (MainTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = selected == tab
            val count = when (tab) {
                MainTab.INSTALLED -> installedCount
                MainTab.RELEASE -> releaseCount
                MainTab.ALL -> allCount
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) FlameDark else BgSurface)
                    .border(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) FlamePrimary else BgBorder,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(tab) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    tab.icon, contentDescription = null,
                    tint = if (isSelected) Color.White else TextSecondary,
                    modifier = Modifier.padding(bottom = 2.dp).size(16.dp),
                )
                Text(
                    text = stringResource(tab.labelRes),
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                )
                Text(
                    text = if (count > 0) "$count" else " ",
                    color = if (isSelected) Color.White.copy(alpha = 0.75f)
                            else TextSecondary.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun VersionsList(
    versions: List<VersionEntry>,
    selectedVersion: VersionEntry?,
    onVersionSelect: (VersionEntry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(versions) { v ->
            VersionItem(v, selectedVersion?.id == v.id) { onVersionSelect(v) }
        }
        item { Box(modifier = Modifier.height(64.dp)) }
    }
}

@Composable
private fun InstancesList(
    isLoggedIn: Boolean,
    instances: List<InstanceMeta>,
    selectedInstanceId: String?,
    onSelect: (InstanceMeta) -> Unit,
    onOpenSettings: (InstanceMeta) -> Unit,
) {
    val context = LocalContext.current
    if (instances.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📭", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    context.getString(R.string.no_installed_instances),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    context.getString(R.string.download_from_version_tab_hint),
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(instances, key = { it.id }) { meta ->
            InstanceItem(
                meta,
                selected = meta.id == selectedInstanceId,
                onSelect = { onSelect(meta) },
                onOpenSettings = { onOpenSettings(meta) },
            )
        }
        item { Box(modifier = Modifier.height(96.dp)) }   // 하단 실행 바에 가리지 않도록 여백
    }
}

/**
 * 인스턴스 목록/실행 바에서 쓰는 아이콘.
 * 우선순위:
 *   1) iconPath(다운로드한 콘텐츠 로고 파일) 가 있으면 그 이미지를 보여준다.
 *   2) 없고 모드팩/모드 출처(sourceModId != null)면 CurseForge 기본 아이콘.
 *   3) 그 외엔 로더 기본 아이콘(fabric/forge/neoforge) 또는 바닐라(마인크래프트) 아이콘.
 */
@Composable
private fun InstanceIcon(meta: InstanceMeta, size: androidx.compose.ui.unit.Dp) {
    val iconFile = meta.iconPath?.let { java.io.File(it) }?.takeIf { it.exists() && it.length() > 0 }
    when {
        iconFile != null -> {
            AsyncImage(
                model = iconFile,
                contentDescription = meta.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(RoundedCornerShape(6.dp)),
            )
        }
        else -> {
            // 폴백 아이콘 리소스 결정
            val fallbackRes = when {
                // 모드팩/모드로 만든 인스턴스인데 아이콘 다운로드가 실패한 경우 → CurseForge 기본 아이콘
                meta.sourceModId != null -> R.drawable.img_curseforge_icon
                else -> when (meta.loaderType?.lowercase()) {
                    "fabric"   -> R.drawable.img_loader_fabric
                    "forge"    -> R.drawable.img_anvil
                    "neoforge" -> R.drawable.img_loader_neoforge
                    else       -> R.drawable.img_minecraft   // 바닐라(로더 없음)
                }
            }
            Image(
                painter = painterResource(fallbackRes),
                contentDescription = meta.loaderType ?: "Vanilla",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size),
            )
        }
    }
}

@Composable
private fun InstanceItem(
    meta: InstanceMeta,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val tablet = isTablet()
    val compact = isCompact()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) FlameDark else BgSurface)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) FlamePrimary else BgBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { onSelect() }
            .padding(
                horizontal = if (tablet) 14.dp else if (compact) 8.dp else 10.dp,
                vertical = if (tablet) 12.dp else if (compact) 7.dp else 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
    ) {
        // 인스턴스 아이콘 — 다운로드한 콘텐츠 아이콘(iconPath)이 있으면 그것을,
        //   없으면 로더 기본 아이콘(또는 CurseForge 기본 아이콘)으로 폴백.
        InstanceIcon(meta = meta, size = if (tablet) 28.dp else if (compact) 18.dp else 22.dp)

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                meta.name,
                color = TextPrimary,
                fontSize = if (tablet) 14.sp else if (compact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val loaderLabel = when (meta.loaderType?.lowercase()) {
                "fabric"   -> "Fabric ${meta.loaderVersion ?: ""}"
                "forge"    -> "Forge ${meta.loaderVersion ?: ""}"
                "neoforge" -> "NeoForge ${meta.loaderVersion ?: ""}"
                "quilt"    -> "Quilt ${meta.loaderVersion ?: ""}"
                else       -> "Vanilla"
            }
            Text(
                // ⚠️ Fold 커버화면처럼 좁을 때는 "MC {버전} · {로더}" 전체가 한 줄에
                //   안 들어가서 줄임표로 잘리기 쉬웠다 — 로더 이름만 남겨서 항상 여유있게.
                text = if (compact) loaderLabel else "MC ${meta.mcVersion} · $loaderLabel",
                color = TextSecondary,
                fontSize = if (tablet) 11.sp else if (compact) 8.sp else 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 선택 표시 체크
        if (selected) {
            Text("✓", color = FlamePrimary, fontSize = if (tablet) 18.sp else if (compact) 13.sp else 15.sp, fontWeight = FontWeight.Bold)
        }

        // 설정 (맵/리소스팩 가져오기, 삭제 등)
        Box(
            modifier = Modifier
                .size(if (tablet) 32.dp else if (compact) 24.dp else 28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(BgDark)
                .border(1.dp, BgBorder, RoundedCornerShape(6.dp))
                .clickable { onOpenSettings() },
            contentAlignment = Alignment.Center,
        ) {
            Text("⚙️", fontSize = if (tablet) 14.sp else if (compact) 10.sp else 12.sp)
        }
    }
}

/**
 * 설치됨 탭의 아래 띠 — 버전 카드 · 실행 버튼을 **한 줄**에 담는다.
 *
 * ⚠️ 버전 카드는 **표시 전용**이다. 예전에는 눌러서 인스턴스 설정으로 갔는데, 목록 각
 *    행에 이미 설정 아이콘이 있어서 같은 입구가 둘이었다. 렌더러 선택도 여기 있었지만
 *    인스턴스 설정으로 되돌렸다 — 이 줄은 "무엇을 · 실행" 두 가지만 말한다.
 */
@Composable
private fun InstalledPanel(
    selected: InstanceMeta?,
    isLoggedIn: Boolean,
    onLaunch: () -> Unit,
) {
    val context = LocalContext.current
    val tablet = isTablet()
    // 한 줄에 놓이는 카드 셋(버전 · 실행)의 공통 높이.
    // 안쪽 여백으로 높이를 정하면 줄 수가 다른 카드끼리 몇 dp 씩 어긋난다.
    // 태블릿은 두 줄 글자 위아래에 숨 쉴 틈을 준다(46dp 는 너무 빽빽해 보였다).
    val rowHeight = if (tablet) 56.dp else 40.dp

    if (selected == null) {
        Text(
            context.getString(R.string.select_instance_left_list_hint),
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (tablet) 16.dp else 10.dp, vertical = if (tablet) 12.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── 실행 대상 (표시 전용) ──
        Row(
            modifier = Modifier
                .weight(1f)
                .height(rowHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(BgDark)
                .border(1.dp, BgBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    selected.name, color = TextPrimary, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "MC ${selected.mcVersion} · ${selected.loaderLabel()}",
                    color = TextSecondary, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Button(
            onClick = onLaunch,
            enabled = (isLoggedIn || BuildConfig.DEBUG),
            colors = ButtonDefaults.buttonColors(
                containerColor = FlamePrimary,
                disabledContainerColor = BgBorder,
            ),
            contentPadding = PaddingValues(horizontal = 4.dp),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.width(if (tablet) 130.dp else 96.dp).height(rowHeight),
        ) {
            Text(
                if (isLoggedIn || BuildConfig.DEBUG) context.getString(R.string.launch_button) else stringResource(R.string.login_short),
                color = Color.White, fontSize = if (tablet) 15.sp else 13.sp,
                fontWeight = FontWeight.Bold, maxLines = 1,
            )
        }
    }
}

private fun InstanceMeta.loaderLabel(): String = when (loaderType?.lowercase()) {
    "fabric"   -> "Fabric ${loaderVersion ?: ""}"
    "forge"    -> "Forge ${loaderVersion ?: ""}"
    "neoforge" -> "NeoForge ${loaderVersion ?: ""}"
    "quilt"    -> "Quilt ${loaderVersion ?: ""}"
    else       -> "Vanilla"
}

/**
 * 게임 실행 중 모달 팝업. 실행을 누른 직후 표시되고,
 * MinecraftActivity 로 전환돼 호스트 화면이 onPause 되면 자동으로 사라진다.
 */
@Composable
private fun LaunchingDialog(meta: InstanceMeta, tablet: Boolean) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { /* 사용자가 닫지 못함 — 화면 전환 시 자동 제거 */ },
        containerColor = BgSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = FlamePrimary, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                Text(context.getString(R.string.game_running), color = TextPrimary, fontSize = if (tablet) 16.sp else 14.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    meta.name,
                    color = TextPrimary,
                    fontSize = if (tablet) 14.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    context.getString(R.string.mc_preparing_wait),
                    color = TextSecondary,
                    fontSize = if (tablet) 12.sp else 10.sp,
                )
            }
        },
        confirmButton = {},
    )
}

// ProfileHeader, VersionItem, BottomPanel, loadSkinFace 는 기존 코드 그대로 유지
// (위에서 import만 추가했고 컴포저블 본문은 손대지 않았습니다)

/**
 * 폰 전용 상단바 — 기존 ProfileHeader(그라데이션 배너 + 텍스트 버튼 여러 줄)를
 * 그대로 축소해서 쓰던 걸 버리고 완전히 새로 설계했다. Material 앱바 스타일로
 * 높이를 56dp 고정해서, 리스트가 쓸 수 있는 세로 공간을 최대한 확보하는 게 목표.
 *
 * - 왼쪽: 앱 아이콘 + 이름(짧게)
 * - 오른쪽: 아이콘 전용 빠른 실행 버튼들(콘텐츠/키설정/렌더러) + 프로필 아바타
 * - 프로필 아바타를 탭하면 드롭다운(로그인 상태/커뮤니티 로그인/로그인)이 뜬다
 *   — 기존엔 이 모든 액션이 가로 스크롤 버튼 줄로 상시 노출돼 공간을 잡아먹었다.
 */
@Composable
private fun MobileTopBar(
    isLoggedIn: Boolean,
    username: String?,
    uuid: String?,
    onLogin: () -> Unit,
) {
    var skinFace by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uuid) { skinFace = loadSkinFace(uuid) }
    var showProfileMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(BgSurface)
            .border(width = 1.dp, color = BgBorder, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.img_logo),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "FlameLauncher", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )

        // ⚠️ 콘텐츠·설정·키보드 버튼은 전부 왼쪽 메뉴(SectionSidebar)로 옮겼다 —
        //    상단에 남겨 두면 같은 곳으로 가는 입구가 두 개가 된다.
        Box {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A0A14))
                    .border(1.dp, if (isLoggedIn) FlamePrimary else BgBorder, RoundedCornerShape(8.dp))
                    .clickable { showProfileMenu = true },
                contentAlignment = Alignment.Center,
            ) {
                if (skinFace != null) {
                    Image(bitmap = skinFace!!, contentDescription = stringResource(R.string.profile_label), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                } else {
                    Text(
                        if (isLoggedIn) username?.take(1)?.uppercase() ?: "?" else "👤",
                        color = FlameLight, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                    )
                }
            }
            DropdownMenu(expanded = showProfileMenu, onDismissRequest = { showProfileMenu = false }) {
                if (isLoggedIn) {
                    Text(
                        username ?: "", color = TextSecondary, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.login_short), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FlamePrimary) },
                        onClick = { showProfileMenu = false; onLogin() },
                    )
                }
            }
        }
    }
}

/**
 * 폰 전용 통합 하단바 — 기존 BottomPanel(버전용)/InstanceLaunchBar(인스턴스용)로
 * 나뉘어있던 걸 하나로 합쳤다. 56dp 고정 높이, 왼쪽엔 선택된 항목 이름 한 줄,
 * 오른쪽엔 Play 버튼 하나만 — 로그인 안내문 같은 부가 텍스트 줄을 없애고,
 * 로그인 안 된 상태에서 누르면 onLogin 이 호출되도록 해서 별도 안내 줄 없이도
 * 자연스럽게 로그인 유도가 되게 했다.
 */
@Composable
private fun MobileBottomBar(
    selectedTab: MainTab,
    selectedVersion: VersionEntry?,
    selectedInstance: InstanceMeta?,
    progress: DownloadProgress,
    isDownloading: Boolean,
    isLoggedIn: Boolean,
    onPlayVersion: () -> Unit,
    onPlayInstance: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isInstalledTab = selectedTab == MainTab.INSTALLED
    val isSelectedSupported = selectedVersion?.let { isVersionSupported(it.id) } == true

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BgSurface)
            .border(1.dp, BgBorder, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .navigationBarsPadding(),
    ) {
        // 다운로드 진행 중일 때만 아주 얇은 진행 바(라벨 없이 % 숫자만).
        if (isInstalledTab.not() && (isDownloading || progress.phase == DownloadPhase.ERROR)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (progress.phase == DownloadPhase.ERROR) "❌ ${progress.error ?: stringResource(R.string.error_label)}" else progress.fileName.ifEmpty { stringResource(R.string.downloading_label) },
                    color = if (progress.phase == DownloadPhase.ERROR) Color(0xFFFF6B6B) else TextSecondary,
                    fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isDownloading) Text("${progress.percent}%", color = FlamePrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = FlamePrimary, trackColor = BgBorder,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically) {
                if (isInstalledTab) {
                    selectedInstance?.let { InstanceIcon(meta = it, size = 22.dp) }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        selectedInstance?.name ?: stringResource(R.string.pick_instance_hint),
                        color = if (selectedInstance != null) TextPrimary else TextSecondary,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        if (selectedVersion != null) {
                            val typeLabel = if (isSelectedSupported) selectedVersion.type else context.getString(R.string.version_unsupported_label, selectedVersion.type)
                            "${selectedVersion.id} · $typeLabel"
                        } else context.getString(R.string.select_version_prompt),
                        color = when {
                            selectedVersion == null -> TextSecondary
                            !isSelectedSupported -> Color(0xFFFF6B6B)
                            else -> TextPrimary
                        },
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = {
                    if (!isLoggedIn && !BuildConfig.DEBUG) { onLogin(); return@Button }
                    if (isInstalledTab) onPlayInstance() else onPlayVersion()
                },
                enabled = if (isInstalledTab) selectedInstance != null else (selectedVersion != null && !isDownloading && isSelectedSupported),
                colors = ButtonDefaults.buttonColors(containerColor = FlamePrimary, disabledContainerColor = BgBorder),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp),
            ) {
                if (isDownloading && !isInstalledTab) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        if (!isLoggedIn && !BuildConfig.DEBUG) stringResource(R.string.login_short) else "▶  Play",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    isLoggedIn: Boolean,
    username: String?,
    uuid: String?,
    onLogin: () -> Unit,
    onOpenContents: () -> Unit,
    onOpenKeySettings: () -> Unit,
    onOpenNetworkSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRendererSettings: () -> Unit,
    onOpenTerracotta: () -> Unit = {},
) {
    val context = LocalContext.current
    // ── 이하 기존 ProfileHeader 본문 그대로 ──
    var skinFace by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uuid) { skinFace = loadSkinFace(uuid) }
    val tablet = isTablet()
    val compact = isCompact()

    Box(
        modifier = Modifier.fillMaxWidth().background(
            Brush.verticalGradient(colors = listOf(Color(0xFF2D0A20), BgDark))
        ).padding(
            top = if (tablet) 48.dp else if (compact) 12.dp else 16.dp,
            bottom = if (tablet) 12.dp else if (compact) 6.dp else 8.dp,
            start = if (compact) 10.dp else 16.dp,
            end = if (compact) 10.dp else 16.dp,
        )
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
                ) {
                    Box(
                        modifier = Modifier.size(if (tablet) 48.dp else if (compact) 28.dp else 30.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A0A14))
                            .border(1.5.dp, FlameDark, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (skinFace != null) {
                            Image(bitmap = skinFace!!, contentDescription = context.getString(R.string.skin_label),
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                        } else {
                            Text(if (isLoggedIn) username?.take(1)?.uppercase() ?: "?" else "?",
                                color = FlameLight, fontSize = if (compact) 15.sp else 17.sp, fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace)
                        }
                    }
                    Image(
                        painter = painterResource(R.drawable.img_logo),
                        contentDescription = null,
                        modifier = Modifier.size(if (tablet) 32.dp else if (compact) 20.dp else 24.dp),
                    )
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            // ⚠️ Fold 커버화면(sw ~280~320dp)에서는 로고+유저명+로그인 버튼이 한 줄에
                            //   다 들어가기 빠듯해서, 이름을 짧게 줄인다.
                            text = if (compact) "Flame" else "FlameLauncher",
                            color = FlameLight, fontSize = if (tablet) 18.sp else if (compact) 12.sp else 13.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isLoggedIn && username != null) {
                            Text(username, color = FlamePrimary, fontSize = if (tablet) 13.sp else if (compact) 9.sp else 10.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else {
                            Text(context.getString(R.string.login_required_title), color = TextSecondary, fontSize = if (tablet) 12.sp else if (compact) 7.sp else 8.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (!isLoggedIn) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(FlameDark)
                            .border(1.dp, FlamePrimary, RoundedCornerShape(8.dp))
                            .clickable { onLogin() }
                            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 4.dp else 5.dp)
                    ) { Text(context.getString(R.string.login_label), color = Color.White, fontSize = if (tablet) 12.sp else if (compact) 7.sp else 8.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
            // ⚠️ 추가 콘텐츠·키 설정·렌더링 설정 버튼 줄은 없앴다. 전부 왼쪽 메뉴
            //    (SectionSidebar)에 있어서 같은 곳으로 가는 입구가 두 개였다.
        }
    }
}

private suspend fun loadSkinFace(uuid: String?, size: Int = 64): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        val cleanUuid = if (!uuid.isNullOrBlank()) uuid.replace("-", "") else "MHF_Alex"
        val url = URL("https://mc-heads.net/avatar/$cleanUuid/$size")
        BitmapFactory.decodeStream(url.openStream())?.asImageBitmap()
    } catch (e: Exception) { e.printStackTrace(); null }
}

@Composable
fun VersionItem(version: VersionEntry, isSelected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val tablet = isTablet()
    val typeColor = when (version.type) {
        "release" -> TagRelease
        "snapshot" -> TagSnapshot
        else -> TagOld
    }
    val typeLabel = when (version.type) {
        "release" -> "Release"
        "snapshot" -> "Snapshot"
        "old_beta" -> "Beta"
        else -> "Alpha"
    }
    val isSupported = remember(version.id) { isVersionSupported(version.id) }

    // ── 폰/태블릿 별 디멘션 ─────────────────────────────────
    val idSize       = if (tablet) 14.sp else 10.sp
    val subSize      = if (tablet)  11.sp else 7.sp
    val typeBadgeSize= if (tablet)  11.sp else 7.sp
    val checkSize    = if (tablet) 18.sp else 14.sp
    val padH         = if (tablet) 14.dp else 10.dp
    val padV         = if (tablet)  10.dp else 6.dp
    val rowGap       = if (tablet)  12.dp else 8.dp
    val badgePadH    = if (tablet)  10.dp else  8.dp
    val badgePadV    = if (tablet)  3.dp else  0.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Color(0xFF2D0A20)
                else BgSurface.copy(alpha = if (isSupported) 1f else 0.6f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) FlamePrimary else BgBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = padH, vertical = padV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(rowGap)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(typeColor.copy(alpha = 0.15f))
                    .border(1.dp, typeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = badgePadH, vertical = badgePadV)
            ) {
                Text(typeLabel, color = typeColor,
                    fontSize = typeBadgeSize, fontWeight = FontWeight.Medium)
            }
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
                Text(
                    text = version.id,
                    color = if (isSupported) TextPrimary else TextSecondary,
                    fontSize = idSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(version.releaseTime.take(10),
                        color = TextSecondary, fontSize = subSize)
                    if (!isSupported) {
                        Text(context.getString(R.string.unsupported_short), color = Color(0xFFFF6B6B), fontSize = subSize)
                    }
                }
            }
        }
        if (isSelected) {
            Text("✓", color = FlamePrimary, fontSize = checkSize, fontWeight = FontWeight.Bold)
        }
    }
}

