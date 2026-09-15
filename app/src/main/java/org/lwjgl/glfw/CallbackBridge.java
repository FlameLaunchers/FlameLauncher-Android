package org.lwjgl.glfw;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

import kr.co.donghyun.flamelauncher.presentation.MinecraftActivity;
import kr.co.donghyun.flamelauncher.presentation.util.MinecraftActivityBridge;

/**
 * PojavLauncher의 CallbackBridge — 안드로이드 측 진입점.
 * input_bridge_v3.c::JNI_OnLoad 가 이 클래스의 static 메서드를
 * GetStaticMethodID 로 찾는다. 없으면 JVM 부팅 시점에 abort.
 */
public class CallbackBridge {
    private static final String TAG = "CallbackBridge";

    // ─── input_bridge_v3.c 가 실제로 export 하는 native ───────────────
    public static native void nativeSetGrabbing(boolean grabbing);
    public static native boolean nativeSetInputReady(boolean inputReady);
    public static native void nativeSendData(boolean isAndroid, int type, String data);
    public static native String nativeClipboard(int action, byte[] copySrc);

    // ─── input_bridge_v3.c 의 nativeSendData 이벤트 타입 (CB_EVENT_*) ──
    public static final int EVENT_TYPE_CHAR             = 1000;
    public static final int EVENT_TYPE_CHAR_MODS        = 1001;
    public static final int EVENT_TYPE_CURSOR_POS       = 1003;
    public static final int EVENT_TYPE_FRAMEBUFFER_SIZE = 1004;
    public static final int EVENT_TYPE_KEY              = 1005;
    public static final int EVENT_TYPE_MOUSE_BUTTON     = 1006;
    public static final int EVENT_TYPE_SCROLL           = 1007;
    public static final int EVENT_TYPE_WINDOW_SIZE      = 1008;

    // ─── 아래는 예전에 `public static native` 로 선언돼 있었지만 네이티브에
    //     대응 구현(Java_org_lwjgl_glfw_CallbackBridge_nativeSendChar 등)이
    //     아예 없어서, 호출하면 UnsatisfiedLinkError 가 나고 호출부의 try/catch 가
    //     그걸 삼켜 "조용히 아무 일도 안 일어나는" 상태였다.
    //     (그 결과 채팅 문자 입력 · 소프트키보드 IME 입력 · 마우스 휠이 전부 무동작)
    //     실제로 export 되는 nativeSendData 로 라우팅해서 되살린다. 호출부는
    //     리플렉션으로 이 이름들을 찾으므로 시그니처는 그대로 유지한다.
    //     데이터 포맷은 input_bridge_v3.c 의 sscanf 포맷과 1:1 로 맞춰야 한다.

    /** CB_EVENT_CHAR — sscanf("%d") */
    public static boolean nativeSendChar(char codepoint) {
        nativeSendData(true, EVENT_TYPE_CHAR, Integer.toString(codepoint));
        return true;
    }

    /** CB_EVENT_CHAR_MODS — sscanf("%d,%d") */
    public static boolean nativeSendCharMods(char codepoint, int mods) {
        nativeSendData(true, EVENT_TYPE_CHAR_MODS, codepoint + "," + mods);
        return true;
    }

    /** CB_EVENT_KEY — sscanf("%d,%d,%d,%d") */
    public static void nativeSendKey(int key, int scancode, int action, int mods) {
        nativeSendData(true, EVENT_TYPE_KEY, key + "," + scancode + "," + action + "," + mods);
    }

    /** CB_EVENT_CURSOR_POS — sscanf("%f,%f"). Float.toString 은 로케일과 무관하게 '.' 를 쓴다. */
    public static void nativeSendCursorPos(float x, float y) {
        nativeSendData(true, EVENT_TYPE_CURSOR_POS, x + "," + y);
    }

    /** CB_EVENT_MOUSE_BUTTON — sscanf("%d,%d,%d") */
    public static void nativeSendMouseButton(int button, int action, int mods) {
        nativeSendData(true, EVENT_TYPE_MOUSE_BUTTON, button + "," + action + "," + mods);
    }

    /** CB_EVENT_SCROLL — sscanf("%f,%f") */
    public static void nativeSendScroll(double xoffset, double yoffset) {
        nativeSendData(true, EVENT_TYPE_SCROLL, (float) xoffset + "," + (float) yoffset);
    }

    /** CB_EVENT_WINDOW_SIZE — 현재 네이티브는 이 타입을 수신만 하고 무시한다(no-op). */
    public static void nativeSendScreenSize(int width, int height) {
        nativeSendData(true, EVENT_TYPE_WINDOW_SIZE, width + "," + height);
    }

    // ─── JVM → Android 콜백 (네이티브가 이 메서드들을 호출) ───────────

    /** 클립보드 접근. action: 2000=COPY, 2001=PASTE, 2002=OPEN. */
    public static String accessAndroidClipboard(int action, String copyContent) {
        try {
            Context ctx = MinecraftActivity.Companion.getCurrentInstance();
            if (ctx == null) return "";
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return "";

            switch (action) {
                case 2000: // COPY
                    cm.setPrimaryClip(ClipData.newPlainText("Minecraft", copyContent));
                    return "";
                case 2001: // PASTE
                    if (cm.hasPrimaryClip() && cm.getPrimaryClip() != null
                            && cm.getPrimaryClip().getItemCount() > 0) {
                        CharSequence t = cm.getPrimaryClip().getItemAt(0).getText();
                        return t != null ? t.toString() : "";
                    }
                    return "";
                case 2002: // OPEN — xdg-open 후킹으로 들어옴
                    Log.i(TAG, "openPath: " + copyContent);
                    if (!copyContent.isEmpty()) {
                        net.kdt.pojavlaunch.MainActivity.openLink(copyContent);
                    }
                    return "";
                default:
                    return "";
            }
        } catch (Throwable t) {
            Log.e(TAG, "accessAndroidClipboard error", t);
            return "";
        }
    }

    /** 마인크래프트가 마우스를 잡았는지/풀었는지 알림. */
    public static void onGrabStateChanged(boolean grabbing) {
        Log.d(TAG, "onGrabStateChanged: " + grabbing);
        try {
            MinecraftActivityBridge.onGrabStateChanged(grabbing);
        } catch (Throwable t) {
            Log.w(TAG, "onGrabStateChanged dispatch failed", t);
        }
    }

    /**
     * 네이티브(egl_bridge.c::reportFpsToJava)가 ~500ms 주기로 호출.
     * frames = 그 구간 동안 실제 스왑된 프레임 수, elapsedNanos = 구간 길이(ns).
     * 렌더(게임) 스레드에서 불리므로 dispatch 만 한다.
     */
    public static void onFramePresented(int frames, long elapsedNanos) {
        try {
            MinecraftActivityBridge.onFramePresented(frames, elapsedNanos);
        } catch (Throwable t) {
            Log.w(TAG, "onFramePresented dispatch failed", t);
        }
    }

    /**
     * 게임이 첫 프레임을 화면에 그린 직후 네이티브(egl_bridge.c::pojavSwapBuffers)가 호출.
     * 부팅 로딩 다이얼로그를 닫는 신호로 쓴다. 렌더 스레드에서 불리므로 dispatch 만 한다.
     */
    public static void onFirstFrameRendered() {
        Log.d(TAG, "onFirstFrameRendered");
        try {
            MinecraftActivityBridge.onFirstFrameRendered();
        } catch (Throwable t) {
            Log.w(TAG, "onFirstFrameRendered dispatch failed", t);
        }
    }

    public static void sendData(int type, String data) {
        nativeSendData(true, type, data);
    }

    public static void onGrabStateChanged(boolean grabbing, boolean ignoreSameValue) {
        onGrabStateChanged(grabbing);
    }

    /** 컨트롤러 direct input 활성화 요청. 미지원이라 no-op. */
    public static void onDirectInputEnable() {
        Log.d(TAG, "onDirectInputEnable (no-op)");
    }
}