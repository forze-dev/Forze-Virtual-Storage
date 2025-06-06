package com.forze.forzestorage.commands;

import com.forze.forzestorage.ForzeStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StorageCommand implements CommandExecutor {
    private final VirtualStorage plugin;

    public StorageCommand(VirtualStorage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "open":
                handleOpen(sender, args);
                break;
            case "add":
                handleAdd(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ця команда доступна тільки для гравців!");
            return;
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            // Відкрити своє сховище
            if (!player.hasPermission("storage.use")) {
                player.sendMessage(getMessage("no-permission"));
                return;
            }
            
            // ВИПРАВЛЕННЯ: перевіряємо чи гравець адмін для своого сховища
            boolean isAdmin = player.hasPermission("storage.admin");
            plugin.getStorageManager().getStorage(player, isAdmin).open();
            player.sendMessage(getMessage("storage-opened"));
        } else if (args.length == 2) {
            // Відкрити чуже сховище (тільки для адмінів)
            if (!player.hasPermission("storage.admin")) {
                player.sendMessage(getMessage("no-permission"));
                return;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target == null) {
                player.sendMessage(getMessage("player-not-found"));
                return;
            }

            plugin.getStorageManager().getStorage(player, target).open();
            player.sendMessage(getMessage("storage-opened"));
        }
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("storage.admin")) {
            sender.sendMessage(getMessage("no-permission"));
            return;
        }

        if (args.length != 4) {
            sender.sendMessage(ChatColor.RED + "Використання: /storage add <гравець> <предмет> <кількість>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-found"));
            return;
        }

        Material material;
        try {
            material = Material.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(getMessage("invalid-item"));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("invalid-amount"));
            return;
        }

        ItemStack item = new ItemStack(material, amount);
        plugin.getStorageManager().addItem(target, item, amount);
        sender.sendMessage(getMessage("item-added"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== VirtualStorage Help ===");
        sender.sendMessage(ChatColor.YELLOW + "/storage open " + ChatColor.WHITE + "- Відкрити своє сховище");
        if (sender.hasPermission("storage.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/storage open <гравець> " + ChatColor.WHITE + "- Відкрити сховище гравця");
            sender.sendMessage(ChatColor.YELLOW + "/storage add <гравець> <предмет> <кількість> " + ChatColor.WHITE + "- Додати предмет до сховища");
        }
    }

    private String getMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, "&cПовідомлення не знайдено: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}