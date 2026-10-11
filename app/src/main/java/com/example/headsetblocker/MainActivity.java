package com.example.headsetblocker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MainActivity extends Activity {
    private RuleStore store;
    private TextView status, message, log;
    private LinearLayout rules;
    private Switch enabled;
    private String shownRules = "";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override public void run() { render(); handler.postDelayed(this, 500); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new RuleStore(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(22);
        body.setPadding(pad, pad, pad, pad);
        scroll.addView(body);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            body.setPadding(pad + insets.getSystemWindowInsetLeft(), pad + insets.getSystemWindowInsetTop(),
                    pad + insets.getSystemWindowInsetRight(), pad + insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        setContentView(scroll);

        text(body, "耳机线控屏蔽", 27, true);
        text(body, "保留声音，过滤你指定的线控按键", 16, false);
        text(body, "免 Root · Android 8.0 及以上\n只处理已学习的按键，不读取屏幕、不联网。实际效果取决于手机和转接头。", 14, false);
        status = text(body, "", 15, true);
        enabled = new Switch(this);
        enabled.setText("开启屏蔽");
        enabled.setTextSize(18);
        enabled.setPadding(0, dp(14), 0, dp(14));
        enabled.setChecked(store.enabled());
        enabled.setOnCheckedChangeListener((button, checked) -> {
            store.setEnabled(checked);
            if (checked && store.learned().isEmpty()) toast("还没有规则，请先学习耳机按键。");
        });
        body.addView(enabled);
        button(body, "1. 开启无障碍服务", view -> {
            new AlertDialog.Builder(this).setTitle("允许过滤按键")
                    .setMessage("请在系统无障碍设置中启用“耳机线控屏蔽”。本应用只检查媒体、音量和助手按键；不读取页面内容，不上传数据。")
                    .setNegativeButton("取消", null).setPositiveButton("前往设置", (dialog, which) -> {
                        try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
                        catch (ActivityNotFoundException e) { toast("请手动打开系统设置中的无障碍页面。"); }
                    }).show();
        });
        button(body, "2. 保护手机音量键", view -> {
            if (!requireService()) return;
            SessionState.start(SessionState.PROTECT_PHONE); render();
        });
        text(body, "先拔下耳机，点击上方按钮后，各按一次手机机身的音量＋和音量－。这两个事件将始终放行。", 14, false);
        button(body, "3. 学习一个耳机按键", view -> {
            if (!requireService()) return;
            if (!SessionState.wired) { toast("请先接好转接头和有线耳机。"); return; }
            SessionState.start(SessionState.LEARN_HEADSET); render();
        });
        text(body, "插好耳机后，每次学习一个按键（例如中键、＋、－）。学习成功的这一次按键会被拦截。长按如产生新的按键代码，也需单独学习。", 14, false);
        message = text(body, "", 15, true);
        button(body, "取消本次学习", view -> { SessionState.cancel(); render(); });

        text(body, "已保存的按键", 19, true);
        rules = new LinearLayout(this); rules.setOrientation(LinearLayout.VERTICAL); body.addView(rules);
        button(body, "关闭屏蔽并清除所有规则", view -> {
            new AlertDialog.Builder(this).setMessage("清除耳机按键和手机音量键保护记录？")
                    .setNegativeButton("取消", null).setPositiveButton("清除", (dialog, which) -> {
                        store.reset(); SessionState.finish("规则已清除，屏蔽已关闭。"); render();
                    }).show();
        });
        text(body, "最近收到的控制按键", 19, true);
        log = text(body, "", 12, false); log.setTextIsSelectable(true);
        text(body, "没有记录：系统可能未交付事件，或其他按键过滤服务产生冲突。显示拦截但仍有动作：该指令可能由转接头或系统提前处理。请同时测试锁屏、音乐播放和通话场景。\n\n恢复：关闭屏蔽开关或在系统设置中关闭本应用的无障碍服务。", 14, false);
    }

    private boolean requireService() {
        if (SessionState.connected) return true;
        toast("请先开启本应用的无障碍服务。"); return false;
    }
    private void render() {
        SessionState.mode();
        status.setText((SessionState.connected ? "服务已连接" : "服务未连接")
                + "  ·  " + (SessionState.wired ? "检测到有线/USB音频" : "未检测到有线音频")
                + "\n手机音量键保护：" + (store.volumeProtectionReady() ? "已完成" : "未完成"));
        if (enabled.isChecked() != store.enabled()) enabled.setChecked(store.enabled());
        message.setText(SessionState.message);
        String events = SessionState.logs();
        log.setText(events.isEmpty() ? "暂未收到控制按键。只显示本次进程内最近 12 次控制按键，不保存普通输入。" : events);
        List<String> list = new ArrayList<>(store.learned()); Collections.sort(list);
        String key = list.toString();
        if (!key.equals(shownRules)) {
            shownRules = key; rules.removeAllViews();
            if (list.isEmpty()) text(rules, "暂无。请逐个学习耳机按键。", 14, false);
            for (String signature : list) {
                button(rules, store.label(signature) + "\n点击删除", view -> { store.remove(signature); render(); });
            }
        }
    }
    private TextView text(LinearLayout parent, String value, int size, boolean bold) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size);
        view.setTextColor(Color.rgb(30, 49, 45));
        view.setPadding(0, dp(9), 0, dp(9));
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        parent.addView(view); return view;
    }
    private void button(LinearLayout parent, String label, View.OnClickListener action) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false);
        button.setOnClickListener(action);
        parent.addView(button, new LinearLayout.LayoutParams(-1, -2));
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
    @Override protected void onResume() { super.onResume(); handler.post(poll); }
    @Override protected void onPause() {
        handler.removeCallbacks(poll); SessionState.cancel(); super.onPause();
    }
}
