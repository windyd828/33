package com.example.headsetblocker;

import android.os.SystemClock;
import java.util.ArrayDeque;

/** In-memory UI state only. App and service use the same main thread/process. */
final class SessionState {
    static final int IDLE = 0, PROTECT_PHONE = 1, LEARN_HEADSET = 2;
    static boolean connected = false, wired = false;
    private static int mode = IDLE;
    private static int protectedThisSession;
    private static long expires;
    static String message = "先开启无障碍服务，再学习耳机按键。";
    private static final ArrayDeque<String> log = new ArrayDeque<>();

    static void start(int nextMode) {
        mode = nextMode;
        protectedThisSession = 0;
        expires = SystemClock.elapsedRealtime() + 20000;
        message = nextMode == PROTECT_PHONE
                ? "20 秒内各按一次手机机身的音量＋和音量－，不要按耳机。"
                : "20 秒内按一次需要屏蔽的耳机按键；每个按键分别学习。";
    }
    static int mode() {
        if (mode != IDLE && SystemClock.elapsedRealtime() > expires) {
            mode = IDLE;
            message = "等待超时。如没有按键记录，系统可能未把该指令交给本应用。";
        }
        return mode;
    }
    static void finish(String text) { mode = IDLE; message = text; }
    static boolean noteProtected(int keyCode) {
        if (keyCode == 24) protectedThisSession |= 1;
        if (keyCode == 25) protectedThisSession |= 2;
        return protectedThisSession == 3;
    }
    static void cancel() {
        if (mode != IDLE) finish("学习已取消，保存的规则仍保留。");
    }
    static void log(String text) {
        log.addFirst(text);
        while (log.size() > 12) log.removeLast();
    }
    static String logs() { return String.join("\n\n", log); }
}
