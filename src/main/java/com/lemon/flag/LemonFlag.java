package com.lemon.flag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LemonFlag extends JavaPlugin implements Listener {

    private final Map<UUID, String> playerIcons = new ConcurrentHashMap<>();

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
        playerIcons.put(player.getUniqueId(), "KH");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerIcons.remove(event.getPlayer().getUniqueId());
    }

    public void setPlayerIconKey(UUID uuid, String key) {
        playerIcons.put(uuid, key.toUpperCase());
    }

    public boolean hasIconKey(String key) {
        return getConfig().contains("icons." + key.toUpperCase());
    }

    public String getIconByKey(String key) {
        String path = "icons." + key.toUpperCase();
        String raw = getConfig().contains(path)
                ? getConfig().getString(path)
                : getConfig().getString("default-flag", "🇰🇭");
        return colorize(raw);
    }

    public String getPlayerFlag(UUID uuid) {
        String key = playerIcons.getOrDefault(uuid, "KH");
        return getIconByKey(key);
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
