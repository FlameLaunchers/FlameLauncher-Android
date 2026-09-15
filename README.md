<div align="center">

# 🔥 FlameLauncher for Android

**안드로이드에서 마인크래프트 자바 에디션을 실행합니다.**

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](#)
[![ABI](https://img.shields.io/badge/ABI-arm64--v8a-orange)](#)
[![AGPL-3.0](https://img.shields.io/badge/license-AGPL--3.0-blue)](LICENSE)

**[🇰🇷 한국어](#-한국어)** · **[🇺🇸 English](#-english)**

</div>

---
---

# 🇰🇷 한국어

## 1. 무엇을 할 수 있나

베드락이 아닌 **자바 에디션**입니다. PC 서버에 그대로 접속하고, PC 모드를 그대로 씁니다.
앱 프로세스 안에 진짜 OpenJDK 를 띄우는 방식이라 에뮬레이터가 아닙니다.

### 버전과 모드

| | |
|---|---|
| **바닐라** | 알파·베타부터 최신 정식까지 |
| **모드 로더** | Fabric · Forge · NeoForge · Quilt |
| **모드·모드팩** | CurseForge · Modrinth 에서 검색해 바로 설치 |
| **의존성** | 필요한 모드를 **자동으로 같이 설치**합니다 |

자바 런타임은 버전에 맞춰 **자동으로 고릅니다** — 1.16 이하는 Java 8, 1.17 은 16,
1.20.4 까지는 17, 1.20.5 부터는 21, 26.x 는 25. 직접 고를 필요가 없습니다.

### 그래픽

| 렌더러 | 방식 | 권장 |
|---|---|---|
| **Zink** 🌋 | Vulkan → OpenGL (Mesa) | 모던 GPU, 1.17+ · **셰이더는 이것** |
| **Krypton** ⚛️ | OpenGL → GLES 3.x | 폭넓은 버전 지원, 내장 |
| **GL4ES** 🕹️ | OpenGL → GLES 2.0 | 구버전(1.12 이하), Zink 안 되는 기기 |
| **MobileGlues** 🚀 | OpenGL 4.x → GLES 3.2 | 셰이더 성능 최상 · **별도 APK 필요** |

Zink 는 실패하면 **Freedreno(Adreno) → Panfrost(Mali) → GL4ES** 순으로 알아서 내려갑니다.
직접 받은 **커스텀 Vulkan 드라이버(Turnip 등)** 를 zip 으로 가져와 쓸 수도 있습니다
(AdrenoTools 규격).

셰이더는 **Iris** 를 씁니다 — 물 반사·그림자·광원까지 들어갑니다.

### 조작

- **화면 버튼** — 위치·크기를 직접 배치합니다
- **물리 키보드** — 연결하면 바로 잡힙니다
- **마우스** — Pointer Capture 로 화면 경계와 무관하게 시점을 돌립니다.
  메뉴에서는 절대좌표 커서로 자동 전환되고, 휠은 핫바입니다
- **게임패드** — 스틱·트리거까지 매핑됩니다

### 멀티플레이

일반 서버 접속은 물론, **온라인 LAN** 으로 서버 없이 친구와 함께 할 수 있습니다.
방을 열면 코드가 나오고, 친구는 그 코드로 들어옵니다.

### 계정

마이크로소프트 정품 로그인만 지원합니다. **게임 파일은 앱에 들어 있지 않고**,
본인 계정으로 Mojang 서버에서 직접 받습니다.

---

## 2. 실행 방법

### 준비물

| | |
|---|---|
| **안드로이드 8.0 이상** | API 26 |
| **arm64-v8a** | 2017년 이후 기기는 대부분 해당. 32비트 기기는 **미지원** |
| **여유 공간 4GB** | 앱 + 게임 + 자바 런타임 |
| **램 4GB 권장** | 3GB 이하는 모드팩이 버겁습니다 |
| **정품 계정** | 마인크래프트 자바 에디션 |

> iOS 와 달리 **JIT 설정이 필요 없습니다.** 안드로이드는 앱이 실행 중에 코드를 만드는
> 것을 막지 않아서, 받아서 바로 쓰면 됩니다.

### 2-1. 설치

1. [Releases](https://github.com/FlameLaunchers/FlameLauncher-Android/releases) 에서 최신 APK 를 받습니다
2. "출처를 알 수 없는 앱" 허용을 묻습니다 — 허용해주세요
3. 첫 실행 때 저장소 권한을 줍니다

### 2-2. 첫 실행

1. **로그인** — 상단 아바타 → 마이크로소프트 계정
2. **버전 고르기** — 왼쪽 메뉴 `인스턴스 선택` → `정식` 탭
3. **모드 로더 선택** — 바닐라 / Fabric / Forge / NeoForge
4. **다운로드** — 게임 파일과 자바 런타임 (첫 실행만, 5~15분)
5. **실행** — `설치됨` 탭에서 고른 뒤 `▶ 실행`

### 2-3. 셰이더 켜기

1. 인스턴스에 **Fabric** 을 설치합니다
2. `모드팩 설치` 에서 **Iris Shaders** 를 설치합니다 (의존 모드는 자동으로 같이)
3. 렌더러를 **Zink** 로 바꿉니다 (`옵션 · 렌더러`)
4. 게임 안 `설정 → 그래픽 → Shader Packs`

무거운 셰이더는 버겁습니다. 가벼운 것(Complementary, BSL 저설정)부터 시작하세요.
MobileGlues APK 를 따로 설치하면 성능이 더 나옵니다.

### 2-4. 조작 설정

`키보드 편집` 에서 화면 버튼의 위치·크기를 직접 배치합니다. 물리 키보드·마우스·게임패드는
연결하는 순간 잡히므로 따로 설정할 것이 없습니다.

### 2-5. 안 될 때

| 증상 | 확인할 것 |
|---|---|
| 설치는 됐는데 실행하면 바로 꺼짐 | **arm64-v8a** 기기인지 확인하세요. 32비트는 미지원입니다 |
| 게임 중 꺼짐 | 램 부족입니다. `옵션` 에서 **할당 메모리를 낮추세요** — 직관과 반대 같지만, 자바 힙을 줄여야 텍스처가 쓸 자리가 생깁니다. 렌더 거리도 같이 낮추세요 |
| 모드팩이 안 켜짐 | 모드가 많으면 첫 부팅이 몇 분 걸립니다. 그래도 안 되면 렌더러를 바꿔보세요 — Zink 와 GL4ES 는 허용 범위가 다릅니다 |
| 셰이더를 켜면 화면이 깨짐 | 렌더러를 **Zink** 로 바꾸세요. GL4ES 는 셰이더를 제대로 못 돌립니다 |
| 26.3 스냅샷이 안 됨 | 마인크래프트가 GLFW 를 SDL3 로 교체했습니다. 미대응이며 **26.2 는 정상입니다** |

---

## 3. 라이선스

**[AGPL-3.0](LICENSE)** 입니다. 선택이 아니라 의무입니다.

결합 저작물의 라이선스는 **가장 강한 카피레프트**가 정하는데, 이 앱이 번들하는
Terracotta 가 AGPL-3.0 입니다.

| 구성 요소 | 라이선스 | 쓰이는 방식 |
|---|---|---|
| **Terracotta** | **AGPL-3.0** | `libterracotta.so` + JNI 바인딩 (온라인 LAN) |
| PojavLauncher 코어 | GPL-3.0 | JNI · EGL · OSMesa 브릿지, LWJGL/exec 후킹, AWT 스텁 |
| ZalithLauncher 2 | GPL-3.0 | 실행 흐름 (NeoForge 클래스패스, Forge 프로세서, GLFW 3.4 스텁) |
| EasyTier | LGPL-3.0 | Terracotta 내부 |
| libadrenotools | BSD-2-Clause | 커스텀 Vulkan 드라이버 로딩 |
| LWJGL (Pojav 패치) | BSD-3-Clause | `lwjgl3/` |
| OpenJDK (Temurin) | GPL-2.0 + Classpath Exception | 번들 JRE |

GPL-3.0 코드는 AGPL-3.0 저작물에 결합할 수 있고(GPLv3 13조가 명시적으로 허용),
결과물은 AGPL-3.0 으로 배포해야 합니다.

> **이전 배포분 정정.** 예전에는 `LICENSE` 에 LGPL-3.0 이 들어 있었고 이 프로젝트의
> 코드는 "라이선스 미선언" 이었습니다. 둘 다 틀렸습니다 — 이미 GPL-3.0(PojavLauncher)과
> AGPL-3.0(Terracotta)을 품고 있었으니 LGPL-3.0 은 허용 범위보다 약했고, 자체 코드를
> 미선언으로 두면 저작물 전체가 모호해집니다.

커스텀 Vulkan 드라이버(Turnip 등)는 **이 프로젝트가 배포하지 않습니다.** 신뢰할 수 있는
출처(Mesa 공식, K11MCH1/AdrenoToolsDrivers 등)에서 사용자가 직접 받아 가져와야 합니다.

자세한 내용은 [NOTICE](NOTICE) 를 보세요.

> Minecraft 는 Mojang AB 의 상표입니다. 이 프로젝트는 Mojang AB · Microsoft 와
> 아무 관련이 없습니다.

<div align="right"><a href="#-flamelauncher-for-android">⬆ 맨 위로</a></div>

---
---

# 🇺🇸 English

## 1. What it does

This is **Java Edition**, not Bedrock. Join the servers your PC friends use, run the mods
they run. A real OpenJDK boots inside the app process — this is not an emulator.

### Versions and mods

| | |
|---|---|
| **Vanilla** | Alpha and Beta through the current release |
| **Mod loaders** | Fabric · Forge · NeoForge · Quilt |
| **Mods and modpacks** | Search and install straight from CurseForge and Modrinth |
| **Dependencies** | Required mods are **installed alongside automatically** |

The Java runtime is **chosen automatically** to match the version — 8 for 1.16 and below,
16 for 1.17, 17 through 1.20.4, 21 from 1.20.5, 25 for 26.x. Nothing to pick.

### Graphics

| Renderer | How | Best for |
|---|---|---|
| **Zink** 🌋 | Vulkan → OpenGL (Mesa) | Modern GPUs, 1.17+ · **use this for shaders** |
| **Krypton** ⚛️ | OpenGL → GLES 3.x | Broad version coverage, built in |
| **GL4ES** 🕹️ | OpenGL → GLES 2.0 | Old versions (1.12 and below), devices Zink won't run on |
| **MobileGlues** 🚀 | OpenGL 4.x → GLES 3.2 | Fastest for shaders · **needs a separate APK** |

If Zink fails it steps down on its own: **Freedreno (Adreno) → Panfrost (Mali) → GL4ES**.
You can also import a **custom Vulkan driver** (Turnip and similar) as a zip, in the
AdrenoTools format.

Shaders run on **Iris** — reflections, shadows, coloured light.

### Controls

- **On-screen buttons** — you place and size them yourself
- **Hardware keyboard** — picked up as soon as it connects
- **Mouse** — Pointer Capture, so looking around isn't bounded by the screen edge.
  Menus switch to an absolute cursor automatically; the wheel is the hotbar
- **Gamepad** — sticks and triggers included

### Multiplayer

Ordinary servers, plus **Online LAN** for playing with a friend without renting a server:
open a room, share the code, they join with it.

### Account

Microsoft sign-in only. **No game files ship with the app** — you download them from
Mojang's own servers with your own account.

---

## 2. Running it

### What you need

| | |
|---|---|
| **Android 8.0 or later** | API 26 |
| **arm64-v8a** | Essentially any device from 2017 onward. 32-bit is **not supported** |
| **4 GB free** | App, game and a Java runtime |
| **4 GB RAM recommended** | Modpacks are a stretch below 3 GB |
| **A paid account** | Minecraft Java Edition |

> Unlike iOS, **no JIT setup is needed.** Android doesn't stop apps from generating code at
> runtime, so you install and go.

### 2-1. Install

1. Grab the latest APK from [Releases](https://github.com/FlameLaunchers/FlameLauncher-Android/releases)
2. Android asks you to allow installs from unknown sources — allow it
3. Grant storage permission on first launch

### 2-2. First run

1. **Sign in** — avatar at the top → Microsoft account
2. **Pick a version** — left menu → the release tab
3. **Pick a loader** — vanilla, Fabric, Forge or NeoForge
4. **Download** — game files and a Java runtime; first time only, 5–15 minutes
5. **Play** — select it under the installed tab and press play

### 2-3. Turning on shaders

1. Install **Fabric** on the instance
2. Install **Iris Shaders** from the modpack browser — dependencies come along
3. Switch the renderer to **Zink**
4. In game: `Options → Video Settings → Shader Packs`

Heavy packs are a lot to ask of a phone. Start light — Complementary, or BSL on low.
Installing the MobileGlues APK separately gets you more performance.

### 2-4. Controls

The keyboard editor lets you place and resize the on-screen buttons. Hardware keyboards,
mice and gamepads are picked up the moment they connect — nothing to configure.

### 2-5. When it doesn't work

| Symptom | What to check |
|---|---|
| Installs, then closes immediately | Confirm the device is **arm64-v8a**. 32-bit is unsupported |
| Crashes mid-game | Out of RAM. **Lower the heap allocation** in options — counter-intuitive, but a smaller Java heap leaves room for textures. Lower the render distance too |
| A modpack won't start | With many mods the first boot takes minutes. If it still fails, try another renderer; Zink and GL4ES differ in what they tolerate |
| Shaders render garbage | Switch to **Zink**. GL4ES cannot drive shaders properly |
| 26.3 snapshots won't run | Minecraft swapped GLFW for SDL3. Unsupported; **26.2 works fine** |

---

## 3. Licence

**[AGPL-3.0](LICENSE)**, by obligation rather than preference.

A combined work takes the **strongest copyleft** it contains, and the Terracotta bundled
here is AGPL-3.0.

| Component | Licence | How it is used |
|---|---|---|
| **Terracotta** | **AGPL-3.0** | `libterracotta.so` + JNI binding (Online LAN) |
| PojavLauncher core | GPL-3.0 | JNI / EGL / OSMesa bridges, LWJGL and exec hooks, AWT stubs |
| ZalithLauncher 2 | GPL-3.0 | Launch flow (NeoForge classpath, Forge processors, GLFW 3.4 stubs) |
| EasyTier | LGPL-3.0 | inside Terracotta |
| libadrenotools | BSD-2-Clause | custom Vulkan driver loading |
| LWJGL (Pojav-patched) | BSD-3-Clause | `lwjgl3/` |
| OpenJDK (Temurin) | GPL-2.0 + Classpath Exception | bundled JRE |

GPL-3.0 code may be combined into an AGPL-3.0 work — GPLv3 section 13 permits exactly this
— and the result must be distributed under AGPL-3.0.

> **Correction to earlier releases.** The `LICENSE` file used to contain LGPL-3.0, and this
> project's own code was listed as "no declared license". Both were wrong: the tree already
> contained GPL-3.0 (PojavLauncher) and AGPL-3.0 (Terracotta), so LGPL-3.0 was weaker than
> the combination allowed, and leaving the project's own code undeclared left the whole work
> ambiguous.

Custom Vulkan drivers (Turnip and similar) are **not distributed by this project.** Get
them from a source you trust — official Mesa, K11MCH1/AdrenoToolsDrivers — and import them
yourself.

See [NOTICE](NOTICE) for the details.

> Minecraft is a trademark of Mojang AB. This project is not affiliated with, endorsed by,
> or connected to Mojang AB or Microsoft.

<div align="right"><a href="#-flamelauncher-for-android">⬆ Back to top</a></div>
