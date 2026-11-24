package com.lemon.flag;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LemonFlag extends JavaPlugin implements Listener {

    private final Map<UUID, String> playerCountryCodes = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand cmd = getCommand("lemonflag");
        if (cmd != null) {
            FlagCommand flagCommand = new FlagCommand(this);
            cmd.setExecutor(flagCommand);
            cmd.setTabCompleter(flagCommand);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FlagExpansion(this).register();
            getLogger().info("LemonFlag hooked into PlaceholderAPI successfully!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Disabling LemonFlag...");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // កំណត់ជា 🇰🇭 ទុកជាមុនពេល Join ដើម្បីកុំឲ្យទទេរ
        playerCountryCodes.put(uuid, "KH");

        String ip = player.getAddress() != null && player.getAddress().getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : "";

        if (ip.isEmpty() || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            playerCountryCodes.put(uuid, "KH");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL("http://ip-api.com/json/" + ip + "?fields=status,countryCode");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                if (conn.getResponseCode() == 200) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                    if (json.has("status") && json.get("status").getAsString().equals("success") && json.has("countryCode")) {
                        String countryCode = json.get("countryCode").getAsString().toUpperCase();
                        playerCountryCodes.put(uuid, countryCode);
                        return;
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Could not fetch country for " + player.getName() + ", defaulting to KH.");
            }
            playerCountryCodes.put(uuid, "KH");
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerCountryCodes.remove(event.getPlayer().getUniqueId());
    }

    public void setPlayerCountryCode(UUID uuid, String countryCode) {
        playerCountryCodes.put(uuid, countryCode.toUpperCase());
    }

    public boolean hasCountryCodeInConfig(String code) {
        return getConfig().contains("flags." + code.toUpperCase());
    }

    public String getFlagByCode(String code) {
        String path = "flags." + code.toUpperCase();
        String raw = getConfig().contains(path)
                ? getConfig().getString(path)
                : getConfig().getString("default-flag", "🇰🇭");
        return colorize(raw);
    }

    public String getPlayerFlag(UUID uuid) {
        String code = playerCountryCodes.getOrDefault(uuid, "KH");
        return getFlagByCode(code);
    }

    public String getMessage(String key) {
        String msg = getConfig().getString("messages." + key, "");
        return colorize(msg);
    }

    public String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
