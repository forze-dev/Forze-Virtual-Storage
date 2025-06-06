package com.forze.forzestorage.commands;

import org.bukkit.command.ConsoleCommandSender;
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
	private final ForzeStorage plugin;

	public StorageCommand(ForzeStorage plugin) {
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
			case "reload":
				handleReload(sender);
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
			if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
				player.sendMessage(getMessage("player-not-found"));
				return;
			}

			plugin.getStorageManager().getStorage(player, target).open();
			player.sendMessage(getMessage("storage-opened"));
		}
	}

	private void handleAdd(CommandSender sender, String[] args) {
		// ВИПРАВЛЕННЯ: Перевіряємо права для гравців і консолі/RCON окремо
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (!player.hasPermission("storage.admin")) {
				player.sendMessage(getMessage("no-permission"));
				return;
			}
		} else if (sender instanceof ConsoleCommandSender) {
			// Консоль і RCON завжди мають права адміністратора
			sender.sendMessage(ChatColor.GREEN + "[ForzeStorage] Команда виконується з консолі/RCON");
		}

		if (args.length != 4) {
			sender.sendMessage(ChatColor.RED + "Використання: /storage add <гравець> <предмет> <кількість>");
			sender.sendMessage(ChatColor.YELLOW + "Приклад: /storage add Steve DIAMOND 64");
			return;
		}

		// Отримуємо гравця
		OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
		if (target == null) {
			sender.sendMessage(getMessage("player-not-found"));
			return;
		}

		// Перевіряємо матеріал
		Material material;
		try {
			material = Material.valueOf(args[2].toUpperCase());
		} catch (IllegalArgumentException e) {
			sender.sendMessage(getMessage("invalid-item"));
			sender.sendMessage(ChatColor.YELLOW + "Доступні матеріали: DIAMOND, IRON_INGOT, GOLDEN_APPLE, EMERALD тощо");
			sender.sendMessage(
					ChatColor.GRAY + "Повний список: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html");
			return;
		}

		// Перевіряємо кількість
		int amount;
		try {
			amount = Integer.parseInt(args[3]);
			if (amount <= 0) {
				sender.sendMessage(getMessage("invalid-amount"));
				return;
			}
			if (amount > 64) {
				sender.sendMessage(
						ChatColor.YELLOW + "Увага: кількість більше 64. Предмет буде розділено на кілька стаків.");
			}
		} catch (NumberFormatException e) {
			sender.sendMessage(getMessage("invalid-amount"));
			return;
		}

		// Додаємо предмет
		try {
			ItemStack item = new ItemStack(material, Math.min(amount, 64));
			int remainingAmount = amount;
			int addedStacks = 0;

			while (remainingAmount > 0) {
				int stackSize = Math.min(remainingAmount, 64);
				ItemStack stackToAdd = new ItemStack(material, stackSize);

				plugin.getStorageManager().addItem(target, stackToAdd, stackSize);
				remainingAmount -= stackSize;
				addedStacks++;

				// Захист від нескінченного циклу
				if (addedStacks > 100) {
					sender.sendMessage(ChatColor.RED + "Помилка: занадто велика кількість предметів!");
					break;
				}
			}

			sender.sendMessage(getMessage("item-added"));
			sender.sendMessage(
					ChatColor.GREEN + "Додано " + amount + " " + material.name() + " до сховища гравця " + target.getName());

			// Сповіщаємо гравця, якщо він онлайн
			if (target.isOnline()) {
				Player onlineTarget = target.getPlayer();
				onlineTarget.sendMessage(ChatColor.GREEN + "До вашого сховища додано " + amount + " " + material.name());
			}

		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Помилка при додаванні предмета: " + e.getMessage());
			plugin.getLogger().warning("Помилка при додаванні предмета " + material.name() + " гравцю " + target.getName()
					+ ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void handleReload(CommandSender sender) {
		// НОВИЙ: команда для перезавантаження конфігурації
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (!player.hasPermission("storage.admin")) {
				player.sendMessage(getMessage("no-permission"));
				return;
			}
		}

		try {
			plugin.reloadConfig();
			sender.sendMessage(ChatColor.GREEN + "✓ Конфігурація ForzeStorage перезавантажена!");
			plugin.getLogger().info(sender.getName() + " перезавантажив конфігурацію ForzeStorage");
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Помилка при перезавантаженні конфігурації: " + e.getMessage());
		}
	}

	private void sendHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.GOLD + "=== ForzeStorage Help ===");

		if (sender instanceof Player) {
			sender.sendMessage(ChatColor.YELLOW + "/storage open " + ChatColor.WHITE + "- Відкрити своє сховище");
		}

		// Команди для адмінів (гравців) або консолі/RCON
		if ((sender instanceof Player && sender.hasPermission("storage.admin")) ||
				sender instanceof ConsoleCommandSender) {

			if (sender instanceof Player) {
				sender.sendMessage(
						ChatColor.YELLOW + "/storage open <гравець> " + ChatColor.WHITE + "- Відкрити сховище гравця");
			}
			sender.sendMessage(ChatColor.YELLOW + "/storage add <гравець> <предмет> <кількість> " + ChatColor.WHITE
					+ "- Додати предмет до сховища");
			sender.sendMessage(ChatColor.YELLOW + "/storage reload " + ChatColor.WHITE + "- Перезавантажити конфігурацію");
			sender.sendMessage(ChatColor.GRAY + "Приклади для RCON:");
			sender.sendMessage(ChatColor.GRAY + "  storage add Steve DIAMOND 64");
			sender.sendMessage(ChatColor.GRAY + "  storage add Alex IRON_INGOT 32");
			sender.sendMessage(ChatColor.GRAY + "  storage add Bob GOLDEN_APPLE 5");
		}
	}

	private String getMessage(String key) {
		String message = plugin.getConfig().getString("messages." + key, "&cПовідомлення не знайдено: " + key);
		return ChatColor.translateAlternateColorCodes('&', message);
	}
}