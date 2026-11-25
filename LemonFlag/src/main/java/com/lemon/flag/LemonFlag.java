package com.lemon.flag;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LemonFlag extends JavaPlugin implements Listener {

    private final ConcurrentHashMap<UUID, String> playerFlags = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FlagExpansion(this).register();
            getLogger().info("LemonFlag enabled and hooked into PlaceholderAPI!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Disabling LemonFlag...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            fetchPlayerFlag(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        fetchPlayerFlag(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerFlags.remove(event.getPlayer().getUniqueId());
    }

    public String getFlag(UUID uuid) {
        return playerFlags.getOrDefault(uuid, "🏳️");
    }

    private void fetchPlayerFlag(Player player) {
        UUID uuid = player.getUniqueId();
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "";

        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.startsWith("192.168.")) {
            playerFlags.put(uuid, countryCodeToEmoji("KH"));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL("http://ip-api.com/line/" + ip + "?fields=countryCode");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String countryCode = in.readLine();
                in.close();

                if (countryCode != null && countryCode.length() == 2) {
                    playerFlags.put(uuid, countryCodeToEmoji(countryCode.trim()));
                } else {
                    playerFlags.put(uuid, "🏳️");
                }
            } catch (Exception e) {
                playerFlags.put(uuid, "🏳️");
            }
        });
    }

    public static String countryCodeToEmoji(String code) {
        if (code == null || code.length() != 2) return "🏳️";
        code = code.toUpperCase();
        int firstLetter = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6;
        int secondLetter = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
    }
}
