package com.example.headsetblocker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public final class BlockerService extends AccessibilityService {
    private RuleStore store;
    private AudioManager audio;
    private final KeyRules.PressTracker tracker = new KeyRules.PressTracker();
    private final AudioDeviceCallback audioCallback = new AudioDeviceCallback() {
        @Override public void onAudioDevicesAdded(AudioDeviceInfo[] added) { refreshWired(); }
        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed) { refreshWired(); }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        store = new RuleStore(this);
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio != null) audio.registerAudioDeviceCallback(audioCallback, new Handler(Looper.getMainLooper()));
        refreshWired();
        SessionState.connected = true;
        SessionState.message = "服务已连接。请先保护手机音量键，再学习耳机按键。";
    }

    private void refreshWired() {
        boolean found = false;
        if (audio != null) {
            for (AudioDeviceInfo device : audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                int type = device.getType();
                if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || type == AudioDeviceInfo.TYPE_USB_HEADSET
                        || type == AudioDeviceInfo.TYPE_USB_DEVICE
                        || type == AudioDeviceInfo.TYPE_USB_ACCESSORY) {
                    found = true;
                    break;
                }
            }
        }
        SessionState.wired = found;
    }

    @Override protected boolean onKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (store == null || !KeyRules.isCandidate(code)) return false;
        InputDevice device = event.getDevice();
        // Never guess the source of virtual or unidentified input.
        boolean identifiable = device != null && !device.isVirtual()
                && device.getDescriptor() != null && !device.getDescriptor().isEmpty();
        String signature = identifiable ? KeyRules.fingerprint(device.getDescriptor(), code, event.getScanCode()) : null;
        boolean firstDown = event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0;
        boolean decision = false;

        if (firstDown) {
            int mode = SessionState.mode();
            String label = (device == null ? "未知设备" : device.getName()) + " · "
                    + KeyEvent.keyCodeToString(code) + " · 扫描码 " + event.getScanCode();
            if (mode == SessionState.PROTECT_PHONE) {
                // Calibration passes keys through, even if an old rule matched.
                if ((code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN) && identifiable) {
                    store.protect(signature, code);
                    if (SessionState.noteProtected(code)) {
                        SessionState.finish("手机音量键已设为始终放行。现在可以学习耳机按键。");
                    } else {
                        SessionState.message = "已保护一个手机音量键，请再按另一个。";
                    }
                }
            } else if (mode == SessionState.LEARN_HEADSET) {
                if (!SessionState.wired) {
                    SessionState.finish("未检测到有线或 USB 音频输出，请连接耳机后重试。");
                } else if (!identifiable) {
                    SessionState.finish("系统没有提供可区分的输入设备，无法安全学习此键。");
                } else if (store.protectedKeys().contains(signature)) {
                    SessionState.finish("此事件与已保护的手机音量键相同，已拒绝添加，避免误屏蔽。");
                } else if (KeyRules.isVolume(code) && !store.volumeProtectionReady()) {
                    SessionState.finish("请先完成两个手机音量键的保护，再学习耳机音量键。");
                } else {
                    store.learn(signature, label);
                    SessionState.finish("已学习：" + label + "。其余按键需分别学习；最后开启屏蔽开关。");
                    decision = true; // Explicit learning consumes this whole press, even when disabled.
                }
            } else {
                decision = KeyRules.shouldBlock(store.enabled(), SessionState.wired, signature,
                        store.learned(), store.protectedKeys());
            }
            boolean consumed = tracker.filter(event.getAction(), event.getRepeatCount(), event.getDownTime(),
                    event.getDeviceId(), code, event.getScanCode(), decision);
            SessionState.log((consumed ? "拦截  " : "放行  ") + label);
            return consumed;
        }
        return tracker.filter(event.getAction(), event.getRepeatCount(), event.getDownTime(),
                event.getDeviceId(), code, event.getScanCode(), false);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { /* No screen access. */ }
    @Override public void onInterrupt() { SessionState.cancel(); }
    @Override public void onDestroy() {
        if (audio != null) audio.unregisterAudioDeviceCallback(audioCallback);
        tracker.clear();
        SessionState.connected = false;
        SessionState.wired = false;
        SessionState.cancel();
        super.onDestroy();
    }
}
