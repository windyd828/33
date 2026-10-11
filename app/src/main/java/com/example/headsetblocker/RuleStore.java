package com.example.headsetblocker;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class RuleStore {
    private final SharedPreferences prefs;
    RuleStore(Context context) {
        prefs = context.getSharedPreferences("rules", Context.MODE_PRIVATE);
    }
    boolean enabled() { return prefs.getBoolean("enabled", false); }
    void setEnabled(boolean value) { prefs.edit().putBoolean("enabled", value).apply(); }
    Set<String> learned() { return copy("learned"); }
    Set<String> protectedKeys() { return copy("protected"); }
    private Set<String> copy(String name) {
        return new HashSet<>(prefs.getStringSet(name, Collections.emptySet()));
    }
    boolean volumeProtectionReady() {
        return prefs.getBoolean("protected24", false) && prefs.getBoolean("protected25", false);
    }
    void protect(String signature, int code) {
        Set<String> protectedSet = protectedKeys();
        Set<String> learnedSet = learned();
        protectedSet.add(signature);
        learnedSet.remove(signature);
        prefs.edit().putStringSet("protected", protectedSet)
                .putStringSet("learned", learnedSet)
                .putBoolean("protected" + code, true).apply();
    }
    void learn(String signature, String label) {
        Set<String> set = learned();
        set.add(signature);
        prefs.edit().putStringSet("learned", set).putString("label:" + signature, label).apply();
    }
    String label(String signature) { return prefs.getString("label:" + signature, signature); }
    void remove(String signature) {
        Set<String> set = learned(); set.remove(signature);
        prefs.edit().putStringSet("learned", set).remove("label:" + signature).apply();
    }
    void reset() { prefs.edit().clear().apply(); }
}
