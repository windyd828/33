package com.example.headsetblocker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Android-independent rules and event-pair accounting. */
public final class KeyRules {
    private KeyRules() {}

    public static boolean isVolume(int code) {
        return code == 24 || code == 25 || code == 164;
    }

    public static boolean isCandidate(int code) {
        switch (code) {
            case 24: case 25: case 164: // volume
            case 79: // headset hook (play/pause/call, as routed by the OS)
            case 85: case 86: case 87: case 88: case 89: case 90: case 91:
            case 126: case 127: case 128: case 129: case 130: case 222:
            case 219: case 231: // assistant / voice assist
                return true;
            default:
                return false;
        }
    }

    public static String fingerprint(String descriptor, int code, int scanCode) {
        return descriptor + "|" + code + "|" + scanCode;
    }

    public static boolean shouldBlock(boolean enabled, boolean wired, String signature,
                                      Set<String> learned, Set<String> protectedKeys) {
        return enabled && wired && signature != null
                && learned.contains(signature) && !protectedKeys.contains(signature);
    }

    public static final class PressTracker {
        // Track passed AND consumed downs. A settings change cannot split a pair.
        private final Map<String, Boolean> presses = new LinkedHashMap<>();

        public boolean filter(int action, int repeat, long downTime, int deviceId,
                              int code, int scanCode, boolean newDownDecision) {
            String token = deviceId + ":" + code + ":" + scanCode + ":" + downTime;
            if (action == 0) {
                Boolean original = presses.get(token);
                if (original != null) return original;
                // If the service missed the original down, do not start blocking mid-press.
                boolean decision = repeat == 0 && newDownDecision;
                if (presses.size() >= 128) {
                    presses.remove(presses.keySet().iterator().next());
                }
                presses.put(token, decision);
                return decision;
            }
            if (action == 1) {
                Boolean original = presses.remove(token);
                return original != null && original;
            }
            return false;
        }

        public void clear() { presses.clear(); }
    }
}
