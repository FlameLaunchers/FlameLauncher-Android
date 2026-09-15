<div align="center">

# 🔥 FlameLauncher

**Minecraft: Java Edition on Android** — a real OpenJDK booted inside the app process.

[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)](#)
[![ABI](https://img.shields.io/badge/ABI-arm64--v8a-orange)](#)
[![Version](https://img.shields.io/badge/version-2.0.0-red)](https://github.com/FlameLaunchers/FlameLauncher/releases)
[![License](https://img.shields.io/badge/license-AGPL--3.0-blue)](LICENSE)

### 🌐 언어 · 言語 · Language

**[🇰🇷 한국어](#-한국어)** · **[🇯🇵 日本語](#-日本語)** · **[🇺🇸 English](#-english)**

</div>

---
---

# 🇰🇷 한국어

> 안드로이드에서 **마인크래프트 자바 에디션**을 실행하는 런처.
> PojavLauncher 코어 위에 ZalithLauncher 2(ZL2)의 실행 로직을 더하고, 터치 우선·한국어 친화 Jetpack Compose UI를 올렸습니다.

## 목차

- [이 프로젝트는 무엇인가](#이-프로젝트는-무엇인가)
- [주요 기능](#주요-기능)
- [지원 범위](#지원-범위)
- [입력](#입력)
- [프로젝트 구조](#프로젝트-구조)
- [실행 흐름](#실행-흐름)
- [빌드](#빌드)
- [알려진 제약](#알려진-제약)
- [라이선스 및 출처](#-라이선스-및-출처)

---

## 이 프로젝트는 무엇인가

안드로이드 앱 프로세스 안에서 **진짜 OpenJDK를 부팅해** 마인크래프트 자바 에디션을 그대로 돌립니다. 에뮬레이터가 아니라, 데스크톱용 자바 게임을 모바일 환경에 맞게 중계하는 구조입니다.

핵심은 세 개의 중계 계층입니다:

| 계층 | 하는 일 |
|---|---|
| **JVM 부팅** | `JNI_CreateJavaVM` 으로 번들 JRE를 띄우고 `mainClass.main()` 호출 |
| **그래픽 중계** | 데스크톱 OpenGL 호출을 안드로이드 GPU가 이해하는 형태로 번역 (Zink/GL4ES 등) |
| **입출력 중계** | GLFW 윈도잉/입력 API를 안드로이드 `Surface`·터치·키 이벤트에 연결 |

### 파생 관계

- **PojavLauncher** — 검증된 네이티브 브릿지를 재사용합니다 (JNI·EGL·OSMesa 컨텍스트 브릿지, LWJGL/exec 후킹, Caciocavallo AWT 스텁)
- **ZalithLauncher 2 (ZL2)** — 실행 흐름 처리 일부를 차용했습니다 (NeoForge 클래스패스 처리, Forge 프로세서 별도 프로세스 실행, GLFW 3.4 메서드 스텁)
- **이 프로젝트가 더한 것** — Compose UI, CurseForge/Modrinth 통합, 자동 로더 설치, 가상 키패드 편집기, 물리 입력 지원, P2P 멀티플레이, 크래시 복구, 한국 네트워크 보조

---

## 주요 기능

### 게임 실행
- 🧱 **로더 자동 설치** — Vanilla / Fabric / Forge / NeoForge (+ 모드팩 경로에서 Quilt)
- 🔑 **Microsoft 로그인** — Xbox Live → XSTS → Minecraft Services 전체 흐름
- ☕ **JRE 자동 선택** — MC 버전에 맞는 Java 8/17/21/25를 골라 추출
- ⚙️ **JVM 튜닝** — 힙 크기, G1GC 파라미터, FPS 언락, 커스텀 인자

### 콘텐츠
- 📦 **CurseForge + Modrinth 통합** — 모드팩 · 모드 · 데이터팩 · 텍스처팩 · 셰이더팩 · 월드 검색/설치
- 🔗 **의존성 자동 해결** — Fabric API 자동 설치, Sodium 감지 시 Podium 호환 패치 동봉
- 🧪 **모드 호환성 사전 검사** — jar 안 `.so` 의 ELF 헤더를 읽어 데스크톱 전용 네이티브를 판별하고, 크래시 대신 해당 모드만 비활성화

### 플레이 환경
- 🎮 **터치 + 물리 입력** — [입력](#입력) 참고
- 🌍 **Terracotta P2P 멀티플레이** — 제3자 중계 서버 없이 참여자끼리 직접 연결
- 🎤 **마이크 브릿지** — 표준 자바 오디오 API를 AAudio로 연결해 Simple Voice Chat 계열 음성채팅 지원
- 🌐 **네트워크 보조** — 한국 환경 친화 DNS, `_minecraft._tcp` SRV 조회, Hamachi/LAN용 커스텀 hosts 매핑

### 운영
- 💥 **크래시 복구 센터** — 새 크래시 리포트를 감지해 전체 로그를 보여주고, **원인으로 의심되는 모드를 짚어 토글로 끄게** 함
- 🔄 **인앱 업데이트 알림** — GitHub 릴리스와 현재 버전을 비교, "이 버전 건너뛰기" 지원

---

## 지원 범위

### 렌더러

| 렌더러 | 방식 | 권장 대상 |
|---|---|---|
| **Zink** 🌋 | Vulkan → OpenGL (OSMesa/Mesa) | 모던 GPU, 1.17+ |
| **GL4ES** 🕹️ | OpenGL → GLES 2.0 | 구버전(1.12 이하), Zink 미지원 기기 |
| **Krypton (NG-GL4ES)** ⚛️ | OpenGL → GLES 3.x | 셰이더 + 폭넓은 버전, 내장 |
| **MobileGlues** 🚀 | OpenGL 4.x → GLES 3.2 | 셰이더 성능 최상, **별도 APK 설치 필요** |

Zink 경로는 실패 시 네이티브에서 **Freedreno(Adreno) → Panfrost(Mali) → GL4ES** 순으로 자동 폴백합니다.
사용자가 직접 받은 **커스텀 Vulkan 드라이버(Turnip 등)** 를 zip으로 가져와 쓸 수도 있습니다(libadrenotools, AdrenoTools 호환 규격).

### JRE 선택 규칙

| 마인크래프트 버전 | Java |
|---|---|
| 알파/베타/클래식 (`a1.` `b1.` `c0.` `inf-` `rd-`) | 8 |
| ~1.16 | 8 |
| 1.17 | 16 |
| 1.18 ~ 1.20.4 | 17 |
| 1.20.5 ~ | 21 |
| 26.x 이상 | 25 |
| 스냅샷 `YYwWWx` | 26년~ → 25, 24년~ → 21, 그 이전 → 17 |

요청한 major가 없으면 `25 → 21 → 17` 순으로 폴백합니다.

### 플랫폼

- **ABI: `arm64-v8a` 단일** (32비트 기기 미지원)
- **minSdk 26** (Android 8.0) / **targetSdk 34** / **compileSdk 36**
- 현재 버전: **2.0.0** (`versionCode 2`)

---

## 입력

### 터치 (기본 경로)

화면 위 가상 키패드와 터치 카메라/공격/핫바 조작이 기본입니다.

- 키패드 배치는 **드래그 앤 드롭으로 편집** 가능하고 폰/태블릿에 맞춰 자동 스케일
- **전투 / 일반 모드 토글** — 상황에 따라 탭과 롱프레스에 공격/사용을 바꿔 배정
- 핫바 영역을 직접 터치해 슬롯 선택, 그 밖을 드래그하면 시점 회전

### 물리 키보드 · 마우스 · 게임패드

| 장치 | 동작 |
|---|---|
| **키보드** | GLFW 전체 키 매핑 (A~Z, 0~9, F1~F12, 넘패드, 좌우 수식키 구분). IME 한글 조합 지원 |
| **마우스** | **Pointer Capture** 로 화면 경계와 무관한 무한 시점 회전. 메뉴에서는 절대좌표 커서로 자동 전환, 휠 = 핫바 |
| **게임패드** | 콘솔판 배치로 키보드/마우스 변환 |

게임패드 매핑 (마인크래프트 자바판은 컨트롤러를 네이티브 지원하지 않아 변환이 유일한 방법입니다):

```
좌스틱 → WASD          우스틱 → 시점 (아날로그)
RT → 좌클릭(공격)       LT → 우클릭(사용)
A  → 점프              B  → 웅크리기
X  → 버리기            Y  → 인벤토리
LB/RB → 핫바 이전/다음  L3 → 달리기    R3 → 휠클릭
Start → ESC           Select → Tab   D-pad → 방향키
```

---

## 프로젝트 구조

```
app/src/main/
├── cpp/                              # 네이티브 코어 (C/C++)
│   ├── flamejvm.cpp                  # JVM 부팅 진입점
│   ├── pojav_jni/                    # PojavLauncher 코어
│   │   ├── egl_bridge.c              # 렌더러 선택 + 폴백 체인 + Vulkan 로딩
│   │   ├── input_bridge_v3.c         # 입력 이벤트 → 게임 JVM 전달
│   │   ├── ctxbridges/               # GL / EGL / OSMesa 컨텍스트 브릿지
│   │   ├── jvm_hooks/                # LWJGL dlopen · forkAndExec 후킹
│   │   ├── native_hooks/             # exit · chmod · DNS 후킹 (bytehook)
│   │   └── driver_helper/            # Turnip 로더 · 네임스페이스 우회
│   └── libadrenotools/               # 커스텀 Vulkan 드라이버 로딩 (BSD-2)
│
├── java/kr/co/donghyun/flamelauncher/
│   ├── data/                         # 도메인 · 저장소
│   │   ├── renderer/                 # 렌더러 정의 · 플러그인 · 커스텀 Vulkan 드라이버
│   │   ├── instance/  jvm/  auth/  setting/  update/
│   │   └── repository/               # 콘텐츠 검색/설치, 바이트코드 패치
│   ├── presentation/
│   │   ├── MainActivity.kt           # 인스턴스 목록 + 실행 진입
│   │   ├── MinecraftActivity.kt      # JVM 부팅 · 클래스패스 조립 · 입력 라우팅
│   │   ├── input/                    # GLFW 키 매핑 · 게임패드 핸들러
│   │   ├── ui/                       # Compose 화면 · 컴포넌트 · 테마
│   │   └── util/
│   │       ├── forge/                # Forge/NeoForge 설치 + 별도 빌더 프로세스
│   │       ├── fabric/  quilt/       # Fabric · Quilt 설치
│   │       ├── mods/                 # 모드팩 임포터 (CurseForge/mrpack)
│   │       ├── minecraft/            # Mojang 다운로더 · JRE 추출
│   │       └── crash/                # 크래시 로그 파서 (원인 모드 추정)
│   └── forge/ProcessorLauncher.java  # 게임 JVM 안에서 Forge 프로세서 실행
│
├── java/org/lwjgl/glfw/CallbackBridge.java   # 네이티브 ↔ 안드로이드 입력 브릿지
├── java/kr/co/donghyun/terracota/            # P2P 멀티플레이 (Terracotta)
└── assets/
    ├── jre/jre{8,17,21,25}.zip       # 번들 JRE
    ├── caciocavallo{,17}/            # AWT 대체 구현 (헤드리스 자바 GUI)
    ├── lwjgl3/lwjgl-glfw-classes.jar # Pojav 패치 LWJGL
    ├── forge-runtime/                # Forge 프로세서 러너
    └── flame-mic-bridge.jar          # 자바 오디오 → AAudio 마이크 브릿지
```

---

## 실행 흐름

```
1. 버전 선택        Mojang version_manifest.json 조회
2. 로더 선택        Fabric / Forge / NeoForge 메타 API 조회
3. 다운로드         client jar + 라이브러리 + 에셋 → 인스턴스 폴더
4. 로더 설치        인스톨러가 라이브러리 병합 → InstanceMeta 로 평탄화
                   (모던 Forge 는 프로세서를 직렬화해 첫 실행 때 처리)
5. JRE 추출         assets/jre/jreN.zip → filesDir/jreN_runtime/
6. (Forge 첫 실행)  :forgebuilder 별도 프로세스에서 client jar 패치 후 종료
7. 렌더러 로드      선택된 .so 로드 + 환경변수 구성
8. JVM 부팅        flamejvm.cpp::bootMinecraftJVM → JNI_CreateJavaVM
9. 게임 시작        mainClass.main(args)
```

> **6번이 별도 프로세스인 이유:** `JNI_CreateJavaVM` 은 프로세스당 한 번만 가능합니다. 게임 JVM 안에서 Forge 프로세서를 돌리면 패치된 jar 가 완성되기 전에 클래스패스가 굳어버려 부팅이 깨집니다. 그래서 프로세서 전용 프로세스에서 jar 를 만들고 그 프로세스를 통째로 끝낸 뒤, 깨끗한 상태에서 게임을 띄웁니다.

---

## 빌드

### 요구 사항

- Android Studio Hedgehog 이상
- Android SDK 36, **NDK r27** (27.0.12077973)
- CMake 3.22.1
- JDK 11+

### `local.properties`

```properties
sdk.dir=/path/to/Android/Sdk
curseforge.api.key="YOUR_CURSEFORGE_API_KEY"
```

CurseForge API 키는 [console.curseforge.com](https://console.curseforge.com/) 에서 발급받습니다. `BuildConfig.CURSEFORGE_API_KEY` 로 주입됩니다.

### 직접 넣어야 하는 에셋

저장소에 포함되지 않는 큰 바이너리는 `app/src/main/assets/` 아래에 직접 넣어야 합니다:

- `jre/jre8.zip`, `jre/jre17.zip`, `jre/jre21.zip`, `jre/jre25.zip`
- `caciocavallo/`, `caciocavallo17/` — 레거시 MC 용 AWT 대체
- `lwjgl3/lwjgl-glfw-classes.jar` — PojavLauncher 패치 LWJGL
- `jniLibs/arm64-v8a/` — 렌더러 `.so` (libOSMesa, libgl4es_114, libng_gl4es 등)

### 빌드 명령

```bash
./gradlew :app:assembleDebug      # 디버그
./gradlew :app:assembleRelease    # 릴리스
./gradlew :app:testDebugUnitTest  # 단위 테스트
```

`arm64-v8a` 만 빌드됩니다.

> **버전 올릴 때:** GitHub 릴리스 태그를 새로 찍기 전에 `app/build.gradle.kts` 의 `versionCode` / `versionName` 을 함께 올려야 합니다. 인앱 업데이트 팝업이 `BuildConfig.VERSION_NAME` 을 최신 태그와 비교하기 때문입니다.

---

## 알려진 제약

정직하게 적어둡니다.

| 제약 | 내용 |
|---|---|
| **MC 26.3 스냅샷 미동작** | 26.3-snapshot-4 부터 마인크래프트가 윈도잉 라이브러리를 **GLFW → SDL3** 로 교체했습니다. 이 런처(및 PojavLauncher 계열 전반)는 자체 `libglfw.so` 스텁으로 GLFW 를 가로채는 구조라, 게임이 GLFW 를 호출하지 않으면 그 계층이 통째로 무의미해집니다. 대응하려면 SDL3 안드로이드 빌드 + LWJGL `lwjgl-sdl` 의 안드로이드 네이티브가 필요합니다. **최신 정식 릴리스(26.2)는 GLFW 를 쓰므로 영향 없습니다.** |
| **Vulkan 네이티브 백엔드 미지원** | 26.2+ 의 실험적 Vulkan 렌더러는 `VkSurfaceKHR` 생성 경로가 없어 사용할 수 없습니다. OpenGL 백엔드로 실행해야 합니다. |
| **데스크톱 전용 네이티브 모드** | `.dll`/x86 `.so` 를 번들하는 모드(Axiom, Controllable, Flashback 등)는 원리적으로 실행 불가라 자동 비활성화됩니다. 삭제가 아니라 `.jar.pingdisabled` 로 이름만 바꿔 모드팩 무결성은 보존합니다. |
| **32비트 기기** | `arm64-v8a` 전용이라 지원하지 않습니다. |
| **Quilt 반쪽 지원** | 모드팩 경로에서는 설치되지만 로더 직접 선택 UI에는 없습니다. |

---

## 🔖 라이선스 및 출처

**이 프로젝트는 [AGPL-3.0](LICENSE) 입니다.**

선택이 아니라 의무입니다. 결합 저작물의 라이선스는 **가장 강한 카피레프트**가 정하는데,
여기서는 번들하는 Terracotta 가 AGPL-3.0 입니다.

| 구성 요소 | 라이선스 | 쓰이는 방식 |
|---|---|---|
| **Terracotta** | **AGPL-3.0** | `jniLibs/arm64-v8a/libterracotta.so` + JNI 바인딩 |
| PojavLauncher 코어 | GPL-3.0 | JNI · EGL · OSMesa 브릿지, LWJGL/exec 후킹, AWT 스텁 |
| ZalithLauncher 2 | GPL-3.0 | 실행 흐름 처리 (NeoForge 클래스패스, Forge 프로세서, GLFW 3.4 스텁) |
| EasyTier | LGPL-3.0 | Terracotta 내부 |
| libadrenotools | BSD-2-Clause | 커스텀 드라이버 로딩 |
| LWJGL (Pojav 패치) | BSD-3-Clause | `lwjgl3/` |
| OpenJDK (Temurin) | GPL-2.0 + Classpath Exception | 번들 JRE |

GPL-3.0 코드는 AGPL-3.0 저작물에 결합할 수 있고(GPLv3 13조가 명시적으로 허용),
결과물은 AGPL-3.0 으로 배포해야 합니다.

> **이전 배포분 정정.** 예전에는 LICENSE 에 LGPL-3.0 이 들어 있었고 이 프로젝트의
> 코드는 "라이선스 미선언" 이었습니다. 둘 다 틀렸습니다 — 이미 GPL-3.0(PojavLauncher)과
> AGPL-3.0(Terracotta)을 품고 있었으니 LGPL-3.0 은 허용 범위보다 약했고, 자체 코드를
> 미선언으로 두면 저작물 전체가 모호해집니다. 자세한 내용은 [NOTICE](NOTICE) 참조.

커스텀 Vulkan 드라이버(Turnip 등)는 이 프로젝트가 배포하지 않습니다. 사용자가 신뢰할 수 있는 출처(Mesa 공식, K11MCH1/AdrenoToolsDrivers 등)에서 직접 받아 가져와야 합니다.

<div align="right"><a href="#-언어--言語--language">⬆ 맨 위로</a></div>

---
---

# 🇯🇵 日本語

> Android で **Minecraft: Java Edition** を動かすランチャー。
> PojavLauncher のコアの上に ZalithLauncher 2 (ZL2) の起動ロジックを取り込み、タッチ優先の Jetpack Compose UI を載せています。

## 目次

- [このプロジェクトについて](#このプロジェクトについて)
- [主な機能](#主な機能)
- [対応範囲](#対応範囲)
- [入力デバイス](#入力デバイス)
- [プロジェクト構成](#プロジェクト構成)
- [起動フロー](#起動フロー)
- [ビルド](#ビルド)
- [既知の制限](#既知の制限)
- [ライセンスと出典](#-ライセンスと出典)

---

## このプロジェクトについて

Android アプリのプロセス内で **本物の OpenJDK を起動して**、Minecraft: Java Edition をそのまま実行します。エミュレーターではなく、デスクトップ向け Java ゲームをモバイル環境へ橋渡しする構造です。

中核となるのは 3 つのブリッジ層です:

| 層 | 役割 |
|---|---|
| **JVM 起動** | `JNI_CreateJavaVM` でバンドル JRE を起動し `mainClass.main()` を呼び出す |
| **グラフィックス変換** | デスクトップ OpenGL の呼び出しを Android GPU が解釈できる形へ翻訳 (Zink / GL4ES など) |
| **入出力変換** | GLFW のウィンドウ・入力 API を Android の `Surface`・タッチ・キーイベントへ接続 |

### 派生関係

- **PojavLauncher** — 実績のあるネイティブブリッジを再利用 (JNI・EGL・OSMesa コンテキストブリッジ、LWJGL / exec フック、Caciocavallo AWT スタブ)
- **ZalithLauncher 2 (ZL2)** — 起動処理の一部を参考にしています (NeoForge のクラスパス処理、Forge プロセッサーの別プロセス実行、GLFW 3.4 メソッドスタブ)
- **本プロジェクトの追加分** — Compose UI、CurseForge / Modrinth 連携、ローダー自動インストール、仮想キーパッドエディター、物理入力対応、P2P マルチプレイ、クラッシュ復旧、ネットワーク補助

---

## 主な機能

### ゲーム起動
- 🧱 **ローダー自動インストール** — Vanilla / Fabric / Forge / NeoForge (モドパック経由で Quilt)
- 🔑 **Microsoft ログイン** — Xbox Live → XSTS → Minecraft Services の全フロー
- ☕ **JRE 自動選択** — MC のバージョンに合わせて Java 8/17/21/25 を選び展開
- ⚙️ **JVM チューニング** — ヒープサイズ、G1GC パラメーター、FPS アンロック、カスタム引数

### コンテンツ
- 📦 **CurseForge + Modrinth 連携** — モドパック・MOD・データパック・テクスチャパック・シェーダーパック・ワールドの検索/導入
- 🔗 **依存関係の自動解決** — Fabric API を自動導入、Sodium 検出時は Podium 互換パッチを同梱
- 🧪 **MOD 互換性の事前チェック** — jar 内の `.so` の ELF ヘッダーを読み、デスクトップ専用ネイティブを判別。クラッシュさせずに該当 MOD だけ無効化

### プレイ環境
- 🎮 **タッチ + 物理入力** — [入力デバイス](#入力デバイス) を参照
- 🌍 **Terracotta P2P マルチプレイ** — 第三者の中継サーバーなしで参加者同士が直接接続
- 🎤 **マイクブリッジ** — 標準 Java オーディオ API を AAudio へ接続し、Simple Voice Chat 系のボイスチャットに対応
- 🌐 **ネットワーク補助** — DNS 指定、`_minecraft._tcp` SRV 参照、Hamachi / LAN 向けカスタム hosts マッピング

### 運用
- 💥 **クラッシュ復旧センター** — 新しいクラッシュレポートを検出して全ログを表示し、**原因と疑われる MOD を指摘してトグルで無効化**
- 🔄 **アプリ内アップデート通知** — GitHub リリースと現在のバージョンを比較、「このバージョンをスキップ」に対応

---

## 対応範囲

### レンダラー

| レンダラー | 方式 | 推奨環境 |
|---|---|---|
| **Zink** 🌋 | Vulkan → OpenGL (OSMesa/Mesa) | モダン GPU、1.17 以降 |
| **GL4ES** 🕹️ | OpenGL → GLES 2.0 | 旧バージョン (1.12 以下)、Zink 非対応端末 |
| **Krypton (NG-GL4ES)** ⚛️ | OpenGL → GLES 3.x | シェーダー + 幅広いバージョン、内蔵 |
| **MobileGlues** 🚀 | OpenGL 4.x → GLES 3.2 | シェーダー性能が最良、**別途 APK のインストールが必要** |

Zink 経路は失敗時にネイティブ側で **Freedreno (Adreno) → Panfrost (Mali) → GL4ES** の順に自動フォールバックします。
ユーザーが自分で入手した **カスタム Vulkan ドライバー (Turnip など)** を zip で読み込んで使うこともできます (libadrenotools / AdrenoTools 互換形式)。

### JRE の選択ルール

| Minecraft バージョン | Java |
|---|---|
| アルファ/ベータ/クラシック (`a1.` `b1.` `c0.` `inf-` `rd-`) | 8 |
| ～1.16 | 8 |
| 1.17 | 16 |
| 1.18 ～ 1.20.4 | 17 |
| 1.20.5 ～ | 21 |
| 26.x 以降 | 25 |
| スナップショット `YYwWWx` | 26 年～ → 25、24 年～ → 21、それ以前 → 17 |

要求された major が無い場合は `25 → 21 → 17` の順にフォールバックします。

### プラットフォーム

- **ABI: `arm64-v8a` のみ** (32bit 端末は非対応)
- **minSdk 26** (Android 8.0) / **targetSdk 34** / **compileSdk 36**
- 現在のバージョン: **2.0.0** (`versionCode 2`)

---

## 入力デバイス

### タッチ (標準)

画面上の仮想キーパッドと、タッチによる視点・攻撃・ホットバー操作が基本です。

- キーパッドの配置は **ドラッグ&ドロップで編集可能**、スマホ / タブレットに合わせて自動スケール
- **戦闘 / 通常モードの切り替え** — 状況に応じてタップと長押しに攻撃 / 使用を割り当て直す
- ホットバー領域を直接タップしてスロット選択、それ以外をドラッグすると視点回転

### 物理キーボード・マウス・ゲームパッド

| デバイス | 動作 |
|---|---|
| **キーボード** | GLFW の全キーマッピング (A〜Z, 0〜9, F1〜F12, テンキー, 左右修飾キーの区別)。IME による日本語/韓国語入力に対応 |
| **マウス** | **Pointer Capture** により画面端に制限されない無限の視点回転。メニューでは絶対座標カーソルへ自動切替、ホイール = ホットバー |
| **ゲームパッド** | コンソール版の配置でキーボード / マウスへ変換 |

ゲームパッドのマッピング (Minecraft Java 版はコントローラーをネイティブ対応していないため、変換が唯一の方法です):

```
左スティック → WASD        右スティック → 視点 (アナログ)
RT → 左クリック(攻撃)       LT → 右クリック(使用)
A  → ジャンプ              B  → スニーク
X  → アイテムを捨てる        Y  → インベントリ
LB/RB → ホットバー 前/次    L3 → ダッシュ   R3 → ホイールクリック
Start → ESC              Select → Tab   D-pad → 方向キー
```

---

## プロジェクト構成

```
app/src/main/
├── cpp/                              # ネイティブコア (C/C++)
│   ├── flamejvm.cpp                  # JVM 起動のエントリポイント
│   ├── pojav_jni/                    # PojavLauncher コア
│   │   ├── egl_bridge.c              # レンダラー選択 + フォールバック連鎖 + Vulkan ロード
│   │   ├── input_bridge_v3.c         # 入力イベント → ゲーム JVM へ伝達
│   │   ├── ctxbridges/               # GL / EGL / OSMesa コンテキストブリッジ
│   │   ├── jvm_hooks/                # LWJGL dlopen・forkAndExec のフック
│   │   ├── native_hooks/             # exit・chmod・DNS のフック (bytehook)
│   │   └── driver_helper/            # Turnip ローダー・名前空間の回避
│   └── libadrenotools/               # カスタム Vulkan ドライバーのロード (BSD-2)
│
├── java/kr/co/donghyun/flamelauncher/
│   ├── data/                         # ドメイン・リポジトリ
│   │   ├── renderer/                 # レンダラー定義・プラグイン・カスタム Vulkan ドライバー
│   │   ├── instance/  jvm/  auth/  setting/  update/
│   │   └── repository/               # コンテンツ検索/導入、バイトコードパッチ
│   ├── presentation/
│   │   ├── MainActivity.kt           # インスタンス一覧 + 起動の入口
│   │   ├── MinecraftActivity.kt      # JVM 起動・クラスパス構築・入力ルーティング
│   │   ├── input/                    # GLFW キーマッピング・ゲームパッドハンドラー
│   │   ├── ui/                       # Compose 画面・コンポーネント・テーマ
│   │   └── util/
│   │       ├── forge/                # Forge/NeoForge の導入 + 別ビルダープロセス
│   │       ├── fabric/  quilt/       # Fabric・Quilt の導入
│   │       ├── mods/                 # モドパックインポーター (CurseForge/mrpack)
│   │       ├── minecraft/            # Mojang ダウンローダー・JRE 展開
│   │       └── crash/                # クラッシュログ解析 (原因 MOD の推定)
│   └── forge/ProcessorLauncher.java  # ゲーム JVM 内で Forge プロセッサーを実行
│
├── java/org/lwjgl/glfw/CallbackBridge.java   # ネイティブ ↔ Android 入力ブリッジ
├── java/kr/co/donghyun/terracota/            # P2P マルチプレイ (Terracotta)
└── assets/
    ├── jre/jre{8,17,21,25}.zip       # バンドル JRE
    ├── caciocavallo{,17}/            # AWT 代替実装 (ヘッドレス Java GUI)
    ├── lwjgl3/lwjgl-glfw-classes.jar # Pojav パッチ版 LWJGL
    ├── forge-runtime/                # Forge プロセッサーランナー
    └── flame-mic-bridge.jar          # Java オーディオ → AAudio マイクブリッジ
```

---

## 起動フロー

```
1. バージョン選択    Mojang version_manifest.json を取得
2. ローダー選択      Fabric / Forge / NeoForge のメタ API を取得
3. ダウンロード      client jar + ライブラリ + アセット → インスタンスフォルダー
4. ローダー導入      インストーラーがライブラリを統合 → InstanceMeta へ平坦化
                   (モダン Forge はプロセッサーを直列化し初回起動時に処理)
5. JRE 展開         assets/jre/jreN.zip → filesDir/jreN_runtime/
6. (Forge 初回)     :forgebuilder 別プロセスで client jar をパッチして終了
7. レンダラー読込    選択された .so をロード + 環境変数を構成
8. JVM 起動         flamejvm.cpp::bootMinecraftJVM → JNI_CreateJavaVM
9. ゲーム開始       mainClass.main(args)
```

> **6 が別プロセスな理由:** `JNI_CreateJavaVM` はプロセスにつき 1 回しか呼べません。ゲーム JVM の中で Forge プロセッサーを走らせると、パッチ済み jar が完成する前にクラスパスが確定してしまい起動が壊れます。そのためプロセッサー専用プロセスで jar を作り、そのプロセスを完全に終了させてから、きれいな状態でゲームを起動します。

---

## ビルド

### 必要環境

- Android Studio Hedgehog 以降
- Android SDK 36、**NDK r27** (27.0.12077973)
- CMake 3.22.1
- JDK 11 以上

### `local.properties`

```properties
sdk.dir=/path/to/Android/Sdk
curseforge.api.key="YOUR_CURSEFORGE_API_KEY"
```

CurseForge API キーは [console.curseforge.com](https://console.curseforge.com/) で取得します。`BuildConfig.CURSEFORGE_API_KEY` として注入されます。

### 自分で用意するアセット

リポジトリに含まれない大きなバイナリは `app/src/main/assets/` 配下へ自分で配置します:

- `jre/jre8.zip`, `jre/jre17.zip`, `jre/jre21.zip`, `jre/jre25.zip`
- `caciocavallo/`, `caciocavallo17/` — レガシー MC 用の AWT 代替
- `lwjgl3/lwjgl-glfw-classes.jar` — PojavLauncher パッチ版 LWJGL
- `jniLibs/arm64-v8a/` — レンダラーの `.so` (libOSMesa, libgl4es_114, libng_gl4es など)

### ビルドコマンド

```bash
./gradlew :app:assembleDebug      # デバッグ
./gradlew :app:assembleRelease    # リリース
./gradlew :app:testDebugUnitTest  # ユニットテスト
```

ビルドされるのは `arm64-v8a` のみです。

> **バージョンを上げるとき:** GitHub リリースタグを新しく打つ前に `app/build.gradle.kts` の `versionCode` / `versionName` も上げてください。アプリ内アップデート通知が `BuildConfig.VERSION_NAME` を最新タグと比較するためです。

---

## 既知の制限

正直に書いておきます。

| 制限 | 内容 |
|---|---|
| **MC 26.3 スナップショットは動作せず** | 26.3-snapshot-4 から Minecraft はウィンドウライブラリを **GLFW → SDL3** に置き換えました。本ランチャー (および PojavLauncher 系全般) は独自の `libglfw.so` スタブで GLFW を横取りする構造のため、ゲームが GLFW を呼ばないとその層ごと無意味になります。対応するには SDL3 の Android ビルドと LWJGL `lwjgl-sdl` の Android ネイティブが必要です。**最新の正式リリース (26.2) は GLFW を使うため影響ありません。** |
| **Vulkan ネイティブバックエンド非対応** | 26.2 以降の実験的な Vulkan レンダラーは `VkSurfaceKHR` の生成経路が無いため使用できません。OpenGL バックエンドで起動してください。 |
| **デスクトップ専用ネイティブを含む MOD** | `.dll` / x86 `.so` を同梱する MOD (Axiom, Controllable, Flashback など) は原理的に実行できないため自動的に無効化されます。削除ではなく `.jar.pingdisabled` へリネームするだけなので、モドパックの整合性は保たれます。 |
| **32bit 端末** | `arm64-v8a` 専用のため非対応です。 |
| **Quilt は部分対応** | モドパック経由では導入されますが、ローダーを直接選ぶ UI にはありません。 |

---

## 🔖 ライセンスと出典

**本プロジェクトは [AGPL-3.0](LICENSE) です。**

選択ではなく義務です。結合著作物のライセンスは**最も強いコピーレフト**が決めます。
ここではバンドルしている Terracotta が AGPL-3.0 です。

| 構成要素 | ライセンス | 使われ方 |
|---|---|---|
| **Terracotta** | **AGPL-3.0** | `jniLibs/arm64-v8a/libterracotta.so` + JNI バインディング |
| PojavLauncher コア | GPL-3.0 | JNI・EGL・OSMesa ブリッジ、LWJGL/exec フック、AWT スタブ |
| ZalithLauncher 2 | GPL-3.0 | 起動処理 (NeoForge クラスパス、Forge プロセッサー、GLFW 3.4 スタブ) |
| EasyTier | LGPL-3.0 | Terracotta 内部 |
| libadrenotools | BSD-2-Clause | カスタムドライバの読み込み |
| LWJGL (Pojav パッチ版) | BSD-3-Clause | `lwjgl3/` |
| OpenJDK (Temurin) | GPL-2.0 + Classpath Exception | バンドル JRE |

GPL-3.0 のコードは AGPL-3.0 の著作物に結合でき (GPLv3 第13条が明示的に許可)、
結果物は AGPL-3.0 で配布する必要があります。

> **過去の配布分の訂正。** 以前は LICENSE が LGPL-3.0 で、本プロジェクトのコードは
> 「ライセンス未宣言」でした。どちらも誤りです — すでに GPL-3.0 (PojavLauncher) と
> AGPL-3.0 (Terracotta) を含んでいたため LGPL-3.0 は許容範囲より弱く、自前コードを
> 未宣言のままにすると著作物全体が曖昧になります。詳細は [NOTICE](NOTICE) を参照。

カスタム Vulkan ドライバー (Turnip など) は本プロジェクトでは配布していません。信頼できる入手元 (Mesa 公式、K11MCH1/AdrenoToolsDrivers など) からユーザー自身が入手して読み込む必要があります。

<div align="right"><a href="#-언어--言語--language">⬆ トップへ</a></div>

---
---

# 🇺🇸 English

> An Android launcher for **Minecraft: Java Edition**.
> Built on the PojavLauncher native core, with launch orchestration adapted from ZalithLauncher 2 (ZL2), wrapped in a touch-first Jetpack Compose UI.

## Table of contents

- [What this project is](#what-this-project-is)
- [Features](#features)
- [What is supported](#what-is-supported)
- [Input](#input)
- [Project layout](#project-layout)
- [Launch flow](#launch-flow)
- [Building](#building)
- [Known limitations](#known-limitations)
- [License and credits](#-license-and-credits)

---

## What this project is

It runs Minecraft: Java Edition as-is by **booting a real OpenJDK inside the Android app process**. This is not an emulator — it is a set of bridging layers between a desktop Java game and mobile hardware.

Three bridges do the heavy lifting:

| Layer | What it does |
|---|---|
| **JVM boot** | Starts the bundled JRE via `JNI_CreateJavaVM` and calls `mainClass.main()` |
| **Graphics bridge** | Translates desktop OpenGL calls into something an Android GPU understands (Zink / GL4ES / …) |
| **I/O bridge** | Wires the GLFW windowing and input API to Android `Surface`, touch and key events |

### Lineage

- **PojavLauncher** — proven native bridges are reused (JNI / EGL / OSMesa context bridges, LWJGL and exec hooks, Caciocavallo AWT stubs)
- **ZalithLauncher 2 (ZL2)** — parts of the launch orchestration are adapted (NeoForge classpath handling, running Forge processors in a separate process, GLFW 3.4 method stubs)
- **What this project adds** — the Compose UI, CurseForge/Modrinth integration, automatic loader installation, an editable virtual keypad, physical input support, P2P multiplayer, crash recovery, and network helpers

---

## Features

### Launching the game
- 🧱 **Automatic loader installation** — Vanilla / Fabric / Forge / NeoForge (plus Quilt through the modpack path)
- 🔑 **Microsoft sign-in** — the full Xbox Live → XSTS → Minecraft Services flow
- ☕ **JRE auto-selection** — picks and extracts Java 8/17/21/25 to match the MC version
- ⚙️ **JVM tuning** — heap size, G1GC parameters, FPS unlock, custom arguments

### Content
- 📦 **CurseForge + Modrinth integration** — search and install modpacks, mods, datapacks, resource packs, shader packs and worlds
- 🔗 **Dependency resolution** — installs Fabric API automatically; ships a Podium compatibility patch when Sodium is detected
- 🧪 **Mod compatibility pre-flight** — reads the ELF header of every `.so` inside a jar to detect desktop-only natives, and disables just that mod instead of crashing

### Playing
- 🎮 **Touch and physical input** — see [Input](#input)
- 🌍 **Terracotta P2P multiplayer** — players connect directly to each other, with no third-party relay server
- 🎤 **Microphone bridge** — routes the standard Java audio API through AAudio, so Simple Voice Chat and friends work
- 🌐 **Network helpers** — configurable DNS, `_minecraft._tcp` SRV lookup, and custom hosts mapping for Hamachi / LAN

### Operations
- 💥 **Crash recovery centre** — detects new crash reports, shows the full log, and **points at the mod it suspects so you can toggle it off**
- 🔄 **In-app update notice** — compares the current build against GitHub releases, with a "skip this version" option

---

## What is supported

### Renderers

| Renderer | How | Best for |
|---|---|---|
| **Zink** 🌋 | Vulkan → OpenGL (OSMesa/Mesa) | Modern GPUs, 1.17+ |
| **GL4ES** 🕹️ | OpenGL → GLES 2.0 | Older versions (1.12 and below), devices without Zink support |
| **Krypton (NG-GL4ES)** ⚛️ | OpenGL → GLES 3.x | Shaders plus broad version coverage; bundled |
| **MobileGlues** 🚀 | OpenGL 4.x → GLES 3.2 | Best shader performance, **requires a separate APK** |

If the Zink path fails, the native side falls back automatically in the order **Freedreno (Adreno) → Panfrost (Mali) → GL4ES**.
You can also import your own **custom Vulkan driver** (Turnip and friends) as a zip — libadrenotools / AdrenoTools compatible format.

### JRE selection rules

| Minecraft version | Java |
|---|---|
| Alpha / Beta / Classic (`a1.` `b1.` `c0.` `inf-` `rd-`) | 8 |
| up to 1.16 | 8 |
| 1.17 | 16 |
| 1.18 – 1.20.4 | 17 |
| 1.20.5 and later | 21 |
| 26.x and later | 25 |
| Snapshots `YYwWWx` | 26+ → 25, 24+ → 21, older → 17 |

If the requested major is unavailable, it falls back `25 → 21 → 17`.

### Platform

- **ABI: `arm64-v8a` only** (no 32-bit devices)
- **minSdk 26** (Android 8.0) / **targetSdk 34** / **compileSdk 36**
- Current version: **2.0.0** (`versionCode 2`)

---

## Input

### Touch (the default path)

An on-screen virtual keypad plus touch camera, attack and hotbar control.

- The keypad layout is **editable by drag and drop** and scales itself for phones and tablets
- **Combat / normal mode toggle** — reassigns attack and use between tap and long-press depending on the situation
- Touch the hotbar area to pick a slot; drag anywhere else to look around

### Physical keyboard, mouse and gamepad

| Device | Behaviour |
|---|---|
| **Keyboard** | Full GLFW key mapping (A–Z, 0–9, F1–F12, numpad, left/right modifiers kept distinct). IME composition supported |
| **Mouse** | **Pointer Capture** gives unbounded look-around regardless of screen edges. Switches to an absolute cursor in menus; wheel = hotbar |
| **Gamepad** | Console-style layout translated into keyboard and mouse events |

Gamepad mapping (Minecraft: Java Edition has no native controller support, so translation is the only option):

```
Left stick  → WASD          Right stick → look (analog)
RT → left click (attack)    LT → right click (use)
A  → jump                   B  → sneak
X  → drop                   Y  → inventory
LB/RB → hotbar prev/next    L3 → sprint      R3 → middle click
Start → ESC                 Select → Tab     D-pad → arrow keys
```

---

## Project layout

```
app/src/main/
├── cpp/                              # Native core (C/C++)
│   ├── flamejvm.cpp                  # JVM boot entry point
│   ├── pojav_jni/                    # PojavLauncher core
│   │   ├── egl_bridge.c              # Renderer selection + fallback chain + Vulkan loading
│   │   ├── input_bridge_v3.c         # Input events → game JVM
│   │   ├── ctxbridges/               # GL / EGL / OSMesa context bridges
│   │   ├── jvm_hooks/                # LWJGL dlopen and forkAndExec hooks
│   │   ├── native_hooks/             # exit / chmod / DNS hooks (bytehook)
│   │   └── driver_helper/            # Turnip loader, namespace workarounds
│   └── libadrenotools/               # Custom Vulkan driver loading (BSD-2)
│
├── java/kr/co/donghyun/flamelauncher/
│   ├── data/                         # Domain and repositories
│   │   ├── renderer/                 # Renderer definitions, plugins, custom Vulkan drivers
│   │   ├── instance/  jvm/  auth/  setting/  update/
│   │   └── repository/               # Content search/install, bytecode patching
│   ├── presentation/
│   │   ├── MainActivity.kt           # Instance list and launch entry point
│   │   ├── MinecraftActivity.kt      # JVM boot, classpath assembly, input routing
│   │   ├── input/                    # GLFW key mapping, gamepad handler
│   │   ├── ui/                       # Compose screens, components, theme
│   │   └── util/
│   │       ├── forge/                # Forge/NeoForge install + separate builder process
│   │       ├── fabric/  quilt/       # Fabric and Quilt install
│   │       ├── mods/                 # Modpack importer (CurseForge / mrpack)
│   │       ├── minecraft/            # Mojang downloader, JRE extraction
│   │       └── crash/                # Crash log parser (guesses the culprit mod)
│   └── forge/ProcessorLauncher.java  # Runs Forge processors inside the game JVM
│
├── java/org/lwjgl/glfw/CallbackBridge.java   # Native ↔ Android input bridge
├── java/kr/co/donghyun/terracota/            # P2P multiplayer (Terracotta)
└── assets/
    ├── jre/jre{8,17,21,25}.zip       # Bundled JREs
    ├── caciocavallo{,17}/            # AWT replacement (headless Java GUI)
    ├── lwjgl3/lwjgl-glfw-classes.jar # Pojav-patched LWJGL
    ├── forge-runtime/                # Forge processor runner
    └── flame-mic-bridge.jar          # Java audio → AAudio microphone bridge
```

---

## Launch flow

```
1. Pick a version    Fetch Mojang version_manifest.json
2. Pick a loader     Query the Fabric / Forge / NeoForge meta APIs
3. Download          client jar + libraries + assets → instance folder
4. Install loader    Installer merges libraries → flattened into InstanceMeta
                     (modern Forge serialises its processors for the first launch)
5. Extract JRE       assets/jre/jreN.zip → filesDir/jreN_runtime/
6. (First Forge run) The :forgebuilder process patches the client jar, then exits
7. Load renderer     Load the selected .so and set up environment variables
8. Boot the JVM      flamejvm.cpp::bootMinecraftJVM → JNI_CreateJavaVM
9. Start the game    mainClass.main(args)
```

> **Why step 6 is a separate process:** `JNI_CreateJavaVM` can only be called once per process. Running Forge processors inside the game JVM freezes the classpath before the patched jar exists, which breaks the boot. So a dedicated process builds the jar, is killed entirely, and only then does the game start from a clean slate.

---

## Building

### Requirements

- Android Studio Hedgehog or newer
- Android SDK 36, **NDK r27** (27.0.12077973)
- CMake 3.22.1
- JDK 11+

### `local.properties`

```properties
sdk.dir=/path/to/Android/Sdk
curseforge.api.key="YOUR_CURSEFORGE_API_KEY"
```

Get a CurseForge API key at [console.curseforge.com](https://console.curseforge.com/). It is injected as `BuildConfig.CURSEFORGE_API_KEY`.

### Assets you must supply yourself

Large binaries are not committed; drop them under `app/src/main/assets/`:

- `jre/jre8.zip`, `jre/jre17.zip`, `jre/jre21.zip`, `jre/jre25.zip`
- `caciocavallo/`, `caciocavallo17/` — AWT replacement for legacy MC
- `lwjgl3/lwjgl-glfw-classes.jar` — PojavLauncher-patched LWJGL
- `jniLibs/arm64-v8a/` — renderer `.so` files (libOSMesa, libgl4es_114, libng_gl4es, …)

### Build commands

```bash
./gradlew :app:assembleDebug      # debug
./gradlew :app:assembleRelease    # release
./gradlew :app:testDebugUnitTest  # unit tests
```

Only `arm64-v8a` is built.

> **When bumping the version:** raise `versionCode` / `versionName` in `app/build.gradle.kts` before cutting a new GitHub release tag — the in-app update prompt compares `BuildConfig.VERSION_NAME` against the latest tag.

---

## Known limitations

Stated honestly.

| Limitation | Detail |
|---|---|
| **MC 26.3 snapshots do not run** | From 26.3-snapshot-4, Minecraft swapped its windowing library from **GLFW to SDL3**. This launcher (and the PojavLauncher family in general) intercepts GLFW with its own `libglfw.so` stub, so once the game stops calling GLFW that entire layer becomes meaningless. Supporting it needs an SDL3 Android build plus Android natives for LWJGL's `lwjgl-sdl`. **The latest stable release (26.2) still uses GLFW, so it is unaffected.** |
| **No native Vulkan backend** | The experimental Vulkan renderer in 26.2+ has no `VkSurfaceKHR` creation path here, so it cannot be used. Run with the OpenGL backend. |
| **Mods bundling desktop-only natives** | Mods that ship `.dll` or x86 `.so` files (Axiom, Controllable, Flashback, …) cannot work by design and are disabled automatically. They are renamed to `.jar.pingdisabled` rather than deleted, so modpack integrity is preserved. |
| **32-bit devices** | Unsupported — `arm64-v8a` only. |
| **Quilt is half-supported** | It installs through the modpack path, but is not in the direct loader-selection UI. |

---

## 🔖 License and credits

**This project is [AGPL-3.0](LICENSE).**

Not by preference but by obligation. The licence of a combined work is decided by the
**strongest copyleft** it contains, and here that is the bundled Terracotta (AGPL-3.0).

| Component | License | How it is used |
|---|---|---|
| **Terracotta** | **AGPL-3.0** | `jniLibs/arm64-v8a/libterracotta.so` + JNI binding |
| PojavLauncher core | GPL-3.0 | JNI / EGL / OSMesa bridges, LWJGL and exec hooks, AWT stubs |
| ZalithLauncher 2 | GPL-3.0 | Launch-flow handling (NeoForge classpath, Forge processors, GLFW 3.4 stubs) |
| EasyTier | LGPL-3.0 | Inside Terracotta |
| libadrenotools | BSD-2-Clause | Custom driver loading |
| LWJGL (Pojav-patched) | BSD-3-Clause | `lwjgl3/` |
| OpenJDK (Temurin) | GPL-2.0 + Classpath Exception | Bundled JRE |

GPL-3.0 code may be combined into an AGPL-3.0 work — GPLv3 section 13 permits exactly
this — and the result must be distributed under AGPL-3.0.

> **Correction to earlier releases.** The LICENSE file used to contain LGPL-3.0, and this
> project's own code was listed as "no declared license". Both were wrong: the tree already
> contained GPL-3.0 (PojavLauncher) and AGPL-3.0 (Terracotta), so LGPL-3.0 was weaker than
> the combination allowed, and leaving the project's own code undeclared left the whole work
> ambiguous. See [NOTICE](NOTICE) for details.

Custom Vulkan drivers (Turnip and similar) are **not** distributed by this project. Users must obtain them from a trusted source (official Mesa, K11MCH1/AdrenoToolsDrivers, …) and import them themselves.

<div align="right"><a href="#-언어--言語--language">⬆ Back to top</a></div>
