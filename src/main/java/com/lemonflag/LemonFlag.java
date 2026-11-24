package com.lemonflag;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LemonFlag extends JavaPlugin implements Listener {
    private static final Pattern COUNTRY = Pattern.compile("\\\"country\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern CODE = Pattern.compile("\\\"country_code\\\"\\s*:\\s*\\\"([A-Za-z]{2})\\\"");
    private static final Country UNKNOWN = new Country("Unknown", "XX", "🌐");
    private final ConcurrentHashMap<UUID, Country> playerCountries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Country> ipCache = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private HttpClient http;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe("PlaceholderAPI is required. Install it, then restart the server.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "LemonFlag-IP-Lookup");
            t.setDaemon(true);
            return t;
        });
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        Bukkit.getPluginManager().registerEvents(this, this);
        if (!new LemonExpansion(this).register()) {
            getLogger().severe("Could not register PlaceholderAPI expansion 'lemon'.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) lookup(player);
        getLogger().info("Enabled. Placeholders: %lemon_flag%, %lemon_country%, %lemon_code%.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { lookup(event.getPlayer()); }

    private void lookup(Player player) {
        InetSocketAddress socket = player.getAddress();
        if (socket == null) { playerCountries.put(player.getUniqueId(), UNKNOWN); return; }
        InetAddress address = socket.getAddress();
        if (address == null) { playerCountries.put(player.getUniqueId(), UNKNOWN); return; }
        String ip = address.getHostAddress();
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
            playerCountries.put(player.getUniqueId(), UNKNOWN);
            return;
        }
        Country cached = ipCache.get(ip);
        if (cached != null) { playerCountries.put(player.getUniqueId(), cached); return; }
        playerCountries.put(player.getUniqueId(), UNKNOWN);
        executor.submit(() -> {
            Country result = fetchCountry(ip);
            ipCache.put(ip, result);
            Bukkit.getScheduler().runTask(this, () -> {
                if (player.isOnline()) playerCountries.put(player.getUniqueId(), result);
            });
        });
    }

    private Country fetchCountry(String ip) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://ipwho.is/" + ip))
                    .timeout(Duration.ofSeconds(8)).header("User-Agent", "LemonFlag/3.0.0").GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().contains("\"success\":false")) return UNKNOWN;
            Matcher country = COUNTRY.matcher(response.body());
            Matcher code = CODE.matcher(response.body());
            if (!country.find() || !code.find()) return UNKNOWN;
            String countryName = country.group(1);
            String countryCode = code.group(1).toUpperCase(Locale.ROOT);
            int first = 0x1F1E6 + countryCode.charAt(0) - 'A';
            int second = 0x1F1E6 + countryCode.charAt(1) - 'A';
            String flag = new String(Character.toChars(first)) + new String(Character.toChars(second));
            return new Country(countryName, countryCode, flag);
        } catch (Exception ex) {
            getLogger().warning("IP country lookup failed: " + ex.getClass().getSimpleName());
            return UNKNOWN;
        }
    }

    String placeholder(OfflinePlayer player, String parameter) {
        if (player == null) return "";
        Country country = playerCountries.getOrDefault(player.getUniqueId(), UNKNOWN);
        return switch (parameter.toLowerCase(Locale.ROOT)) {
            case "flag" -> country.flag();
            case "country" -> country.name();
            case "code" -> country.code();
            default -> null;
        };
    }

    @Override
    public void onDisable() {
        if (executor != null) executor.shutdownNow();
        playerCountries.clear();
        ipCache.clear();
    }

    private record Country(String name, String code, String flag) { }

    private static final class LemonExpansion extends PlaceholderExpansion {
        private final LemonFlag plugin;
        private LemonExpansion(LemonFlag plugin) { this.plugin = plugin; }
        @Override public String getIdentifier() { return "lemon"; }
        @Override public String getAuthor() { return "LemonFlag"; }
        @Override public String getVersion() { return "3.0.0"; }
        @Override public boolean persist() { return true; }
        @Override public String onRequest(OfflinePlayer player, String params) { return plugin.placeholder(player, params); }
    }
}
