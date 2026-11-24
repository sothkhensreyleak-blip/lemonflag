package com.lemon.flag;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FlagCommand implements CommandExecutor, TabCompleter {

    private final LemonFlag plugin;

    public FlagCommand(LemonFlag plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("lemonflag.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(plugin.getMessage("reloaded"));
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                sender.sendMessage(plugin.getMessage("usage"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getMessage("player-not-found")
                        .replace("{player}", args[1]));
                return true;
            }

            String code = args[2].toUpperCase();
            if (!plugin.hasCountryCodeInConfig(code)) {
                sender.sendMessage(plugin.getMessage("invalid-code")
                        .replace("{code}", code));
                return true;
            }

            plugin.setPlayerCountryCode(target.getUniqueId(), code);
            String flag = plugin.getFlagByCode(code);

            sender.sendMessage(plugin.getMessage("flag-set")
                    .replace("{player}", target.getName())
                    .replace("{flag}", flag)
                    .replace("{code}", code));
            return true;
        }

        sender.sendMessage(plugin.getMessage("usage"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("lemonflag.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "set").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            ConfigurationSection flagsSection = plugin.getConfig().getConfigurationSection("flags");
            if (flagsSection != null) {
                return new ArrayList<>(flagsSection.getKeys(false)).stream()
                        .filter(code -> code.toUpperCase().startsWith(args[2].toUpperCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
