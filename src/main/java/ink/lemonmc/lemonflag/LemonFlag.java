package ink.lemonmc.lemonflag;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LemonFlag extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, String> playerFlags = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerCountries = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("lemonflag") != null) {
            getCommand("lemonflag").setExecutor(this);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FlagPlaceholder(this).register();
            printBanner();
        } else {
            getLogger().warning("PlaceholderAPI not found! Disabling LemonFlag...");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void printBanner() {
        var console = Bukkit.getConsoleSender();

        var purple = NamedTextColor.LIGHT_PURPLE;

        var colors = new TextColor[] {
            NamedTextColor.AQUA,
            NamedTextColor.GREEN,
            NamedTextColor.YELLOW,
            NamedTextColor.GOLD,
            NamedTextColor.RED,
            NamedTextColor.LIGHT_PURPLE
        };

        String[] logo = {
            "██╗     ███████╗███╗   ███╗ ██████╗ ███╗   ██╗",
            "██║     ██╔════╝████╗ ████║██╔═══██╗████╗  ██║",
            "██║     █████╗  ██╔████╔██║██║   ██║██╔██╗ ██║",
            "██║     ██╔══╝  ██║╚██╔╝██║██║   ██║██║╚██╗██║",
            "███████╗███████╗██║ ╚═╝ ██║╚██████╔╝██║ ╚████║",
            "╚══════╝╚══════╝╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═══╝"
        };

        console.sendMessage(Component.empty());

        // BIG COLORFUL LEMON FLAG
        for (String line : logo) {
            var message = Component.empty();

            int index = 0;
            for (char c : line.toCharArray()) {
                message = message.append(
                    Component.text(
                        String.valueOf(c),
                        colors[index % colors.length]
                    )
                );
                index++;
            }
            console.sendMessage(message);
        }

        // PURPLE INFORMATION
        String[] info = {
            "",
            "             Version : 3.1.0",
            "             Author  : Crownixed",
            "             Status  : ENABLED",
            "             Plugin  : LemonFlag GeoIP",
            "",
            "       Thanks for using LEMON FLAG!",
            "       --------------------------------------",
            ""
        };

        for (String line : info) {
            console.sendMessage(
                Component.text(line, purple)
            );
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            for (Player online : Bukkit.getOnlinePlayers()) {
                String code = playerCountries.get(online.getUniqueId());
                if (code != null && code.length() == 2) {
                    playerFlags.put(online.getUniqueId(), resolveFlag(code));
                }
            }
            sender.sendMessage(Component.text("LemonFlag config reloaded!", NamedTextColor.GREEN));
            return true;
        }
        sender.sendMessage(Component.text("Usage: /lemonflag reload", NamedTextColor.YELLOW));
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // បញ្ជូន Resource Pack ទៅកាន់ Player ភ្លាមៗពេលចូល Server
        sendFlagResourcePack(player);

        String defaultFlag = getConfig().getString("default-flag", "🏳️");
        playerFlags.put(uuid, defaultFlag);
        playerCountries.put(uuid, "Unknown");

        if (player.getAddress() == null) return;
        String ip = player.getAddress().getAddress().getHostAddress();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String countryCode = fetchCountryCode(ip);
            if (countryCode != null && countryCode.length() == 2) {
                String flag = resolveFlag(countryCode.toUpperCase());
                playerFlags.put(uuid, flag);
                playerCountries.put(uuid, countryCode.toUpperCase());
            }
        });
    }

    private void sendFlagResourcePack(Player player) {
        if (!getConfig().getBoolean("resource-pack.enabled", false)) return;

        String url = getConfig().getString("resource-pack.url", "");
        if (url.isEmpty() || url.contains("example.com")) return;

        String sha1 = getConfig().getString("resource-pack.sha1", "");
        boolean required = getConfig().getBoolean("resource-pack.required", true);
        String promptText = getConfig().getString("resource-pack.prompt", "<yellow>You need to download this Resource Pack to see the flags!");

        try {
            UUID packId = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));

            ResourcePackInfo.Builder infoBuilder = ResourcePackInfo.resourcePackInfo()
                    .id(packId)
                    .uri(URI.create(url));

            if (sha1 != null && sha1.length() == 40) {
                infoBuilder.hash(sha1);
            }

            Component promptComponent = MiniMessage.miniMessage().deserialize(promptText);

            ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                    .packs(infoBuilder.build())
                    .prompt(promptComponent)
                    .required(required)
                    .replace(true)
                    .build();

            player.sendResourcePacks(request);
        } catch (Exception e) {
            getLogger().warning("Failed to send Resource Pack to " + player.getName() + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerFlags.remove(uuid);
        playerCountries.remove(uuid);
    }

    private String fetchCountryCode(String ip) {
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return "KH";
        }

        try {
            URL url = URI.create("http://ip-api.com/line/" + ip + "?fields=countryCode").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && line.trim().length() == 2) {
                        return line.trim();
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("Failed to fetch country for IP " + ip + ": " + e.getMessage());
        }
        return null;
    }

    private String resolveFlag(String countryCode) {
        String custom = getConfig().getString("custom-flags." + countryCode);
        if (custom != null && !custom.isEmpty()) {
            return custom;
        }
        return countryCodeToEmoji(countryCode);
    }

    private String countryCodeToEmoji(String code) {
        int firstLetter = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6;
        int secondLetter = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
    }

    public String getPlayerFlag(UUID uuid) {
        return playerFlags.getOrDefault(uuid, getConfig().getString("default-flag", "🏳️"));
    }

    public String getPlayerCountryCode(UUID uuid) {
        return playerCountries.getOrDefault(uuid, "Unknown");
    }
}
