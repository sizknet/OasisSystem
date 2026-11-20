package com.example.httpconsolebridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class HttpConsoleBridge extends JavaPlugin {

    // 🌐 あなたのRenderアプリのURL（末尾に /get_command をつける）
    private final String BOT_API_URL = "https://discordbot-xokk.onrender.com/get_command";

    @Override
    public void onEnable() {
        // 3秒ごとにRenderボットへコマンドを確認しに行くタスクを開始（非同期処理）
        new BukkitRunnable() {
            @Override
            public void run() {
                fetchAndExecuteCommand();
            }
        }.runTaskTimerAsynchronously(this, 0L, 60L); // 60L = 3秒ごと (20tick = 1秒)

        getLogger().info("HttpConsoleBridge プラグインが有効化されました！");
    }

    @Override
    public void onDisable() {
        getLogger().info("HttpConsoleBridge プラグインが無効化されました。");
    }

    private void fetchAndExecuteCommand() {
        try {
            URL url = new URL(BOT_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();
                
                // 返ってきたJSONから "command": "..." の中身を抜き出す簡易パース
                if (json.contains("\"command\":\"") && !json.contains("\"command\":\"\"")) {
                    int startIndex = json.indexOf("\"command\":\"") + 11;
                    int endIndex = json.indexOf("\"", startIndex);
                    String command = json.substring(startIndex, endIndex);

                    if (!command.isEmpty()) {
                        // マイクラのメインスレッドに戻して安全にコンソールコマンドを実行
                        Bukkit.getScheduler().runTask(this, () -> {
                            getLogger().info("Discordから受信したコマンドを実行します: " + command);
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        });
                    }
                }
            }
        } catch (Exception e) {
            // 接続エラー（Renderの起動中やタイムアウトなど）は無視して次回リトライ
        }
    }
}
