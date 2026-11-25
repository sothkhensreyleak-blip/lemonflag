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
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LemonFlag extends JavaPlugin implements Listener {
    private static final long CACHE_TTL = Duration.ofMinutes(30).toMillis();
    private final Map<UUID, CountryData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> pending = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private HttpClient http;

    @Override
    public void onEnable() {
        executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "LemonFlag-GeoIP");
            t.setDaemon(true);
            return t;
        });
        http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(executor)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        getServer().getPluginManager().registerEvents(this, this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LemonExpansion(this).register();
            getLogger().info("PlaceholderAPI connected: %lemon_flag%, %lemon_country%, %lemon_code%");
        } else {
            getLogger().warning("PlaceholderAPI not found; LemonFlag placeholders are unavailable.");
        }
        for (Player player : Bukkit.getOnlinePlayers()) lookup(player);
        getLogger().info("LemonFlag 3.0.0 enabled.");
    }

    @Override
    public void onDisable() {
        if (executor != null) executor.shutdownNow();
        cache.clear();
        pending.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        lookup(event.getPlayer());
    }

    private void lookup(Player player) {
        UUID uuid = player.getUniqueId();
        CountryData previous = cache.get(uuid);
        if (previous != null && !previous.expired()) return;

        final String ip;
        try {
            if (player.getAddress() == null || player.getAddress().getAddress() == null) return;
            InetAddress address = player.getAddress().getAddress();
            if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                    || address.isMulticastAddress()) {
                cache.put(uuid, new CountryData("Unknown", "XX", "🏳️"));
                return;
            }
            ip = address.getHostAddress();
        } catch (Exception ex) {
            getLogger().warning("Could not read player connection address.");
            return;
        }

        if (pending.putIfAbsent(uuid, Boolean.TRUE) != null) return;
        executor.execute(() -> {
            try {
                cache.put(uuid, fetch(ip));
            } catch (Exception ex) {
                getLogger().warning("GeoIP lookup failed: " + ex.getMessage());
                cache.put(uuid, new CountryData("Unknown", "XX", "🏳️"));
            } finally {
                pending.remove(uuid);
            }
        });
    }

    private CountryData fetch(String ip) throws Exception {
        String safeIp = URLEncoder.encode(ip, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://ipwho.is/" + safeIp))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "LemonFlag/3.0.0")
                .GET().build();
        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) throw new IllegalStateException("GeoIP HTTP " + response.statusCode());
        String body = response.body();
        if (body.contains("\"success\":false")) throw new IllegalStateException("GeoIP service rejected lookup");
        String country = jsonValue(body, "country");
        String code = jsonValue(body, "country_code");
        if (country == null || code == null || !code.matches("(?i)[a-z]{2}")) {
            throw new IllegalStateException("GeoIP response missing country fields");
        }
        code = code.toUpperCase(Locale.ROOT);
        return new CountryData(country, code, flag(code));
    }

    private static String jsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key)
                + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) return null;
        return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\").trim();
    }

    private static String flag(String code) {
        if (code == null || !code.matches("[A-Z]{2}")) return "🏳️";
        int base = 0x1F1E6;
        return new String(Character.toChars(base + code.charAt(0) - 'A'))
                + new String(Character.toChars(base + code.charAt(1) - 'A'));
    }

    private record CountryData(String country, String code, String flag, long createdAt) {
        CountryData(String country, String code, String flag) {
            this(country, code, flag, System.currentTimeMillis());
        }
        boolean expired() { return System.currentTimeMillis() - createdAt > CACHE_TTL; }
    }

    private static final class LemonExpansion extends PlaceholderExpansion {
        private final LemonFlag plugin;
        private LemonExpansion(LemonFlag plugin) { this.plugin = plugin; }
        @Override public String getIdentifier() { return "lemon"; }
        @Override public String getAuthor() { return "LemonFlag"; }
        @Override public String getVersion() { return "3.0.0"; }
        @Override public boolean persist() { return true; }

        @Override
        public String onRequest(OfflinePlayer offlinePlayer, String params) {
            if (offlinePlayer == null) return "";
            Player online = offlinePlayer.getPlayer();
            if (online != null) plugin.lookup(online);
            CountryData data = plugin.cache.get(offlinePlayer.getUniqueId());
            if (data == null || data.expired()) {
                return switch (params.toLowerCase(Locale.ROOT)) {
                    case "flag" -> "⏳";
                    case "country" -> "Loading";
                    case "code" -> "XX";
                    default -> null;
                };
            }
            return switch (params.toLowerCase(Locale.ROOT)) {
                case "flag" -> data.flag();
                case "country" -> data.country();
                case "code" -> data.code();
                default -> null;
            };
        }
    }
}
