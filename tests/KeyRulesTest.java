package com.example.headsetblocker;

import java.util.Collections;
import java.util.Set;

public final class KeyRulesTest {
    private static int passed;
    private static void check(boolean truth, String name) {
        if (!truth) throw new AssertionError(name);
        passed++;
    }
    public static void main(String[] args) {
        String headset = KeyRules.fingerprint("usb-adapter", 24, 115);
        String phone = KeyRules.fingerprint("gpio-keys", 24, 115);
        Set<String> rules = Collections.singleton(headset);
        Set<String> none = Collections.emptySet();
        check(KeyRules.shouldBlock(true, true, headset, rules, none), "learned headset is blocked");
        check(!KeyRules.shouldBlock(true, true, phone, rules, none), "same key code on phone passes");
        check(!KeyRules.shouldBlock(true, true, KeyRules.fingerprint("usb-adapter", 24, 116), rules, none), "other scan code passes");
        check(!KeyRules.shouldBlock(false, true, headset, rules, none), "off passes");
        check(!KeyRules.shouldBlock(true, false, headset, rules, none), "unplugged passes");
        check(!KeyRules.shouldBlock(true, true, null, rules, none), "unidentified passes");
        check(!KeyRules.shouldBlock(true, true, headset, rules, rules), "protected wins over learned");
        check(!KeyRules.isCandidate(26) && !KeyRules.isCandidate(3) && !KeyRules.isCandidate(29), "power home typing ignored");
        check(KeyRules.isCandidate(79) && KeyRules.isCandidate(85) && KeyRules.isCandidate(231), "media voice supported");

        KeyRules.PressTracker tracker = new KeyRules.PressTracker();
        check(tracker.filter(0, 0, 100, 2, 24, 115, true), "consume initial down");
        check(tracker.filter(0, 1, 100, 2, 24, 115, false), "repeat consumed after disabling");
        check(tracker.filter(1, 0, 100, 2, 24, 115, false), "up consumed after disabling or unplugging");
        check(!tracker.filter(1, 0, 100, 2, 24, 115, true), "duplicate orphan up passes");
        check(!tracker.filter(0, 0, 200, 2, 24, 115, false), "initial down passes when disabled");
        check(!tracker.filter(0, 1, 200, 2, 24, 115, true), "repeat passes after enabling");
        check(!tracker.filter(1, 0, 200, 2, 24, 115, true), "up passes after enabling");
        check(!tracker.filter(0, 3, 300, 2, 24, 115, true), "joining during repeat passes");
        check(!tracker.filter(1, 0, 300, 2, 24, 115, true), "joining during repeat up passes");
        check(tracker.filter(0, 0, 400, 2, 24, 115, true), "headset down");
        check(!tracker.filter(0, 0, 400, 3, 24, 115, false), "concurrent phone down");
        check(!tracker.filter(1, 0, 400, 3, 24, 115, false), "concurrent phone up");
        check(tracker.filter(1, 0, 400, 2, 24, 115, false), "concurrent headset up");
        check(tracker.filter(0, 0, 500, 2, 24, 115, true), "old missing up");
        check(!tracker.filter(0, 0, 501, 2, 24, 115, false), "new downTime is a new press");
        check(!tracker.filter(1, 0, 501, 2, 24, 115, true), "new press remains passed");
        tracker.clear();
        check(!tracker.filter(1, 0, 500, 2, 24, 115, true), "after service reset orphan up passes");
        check(!tracker.filter(2, 0, 600, 2, 24, 115, true), "ACTION_MULTIPLE passes");
        System.out.println("PASS: " + passed + " core assertions");
    }
}
