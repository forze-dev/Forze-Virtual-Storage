package com.forze.forzestorage.listeners;

import com.forze.forzestorage.ForzeStorage;
import com.forze.forzestorage.gui.StorageGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class StorageListener implements Listener {
	private final ForzeStorage plugin;

	public StorageListener(ForzeStorage plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player))
			return;
		Player player = (Player) event.getWhoClicked();

		// Перевіряємо, чи це сховище
		if (event.getView().getTitle() == null || !event.getView().getTitle().contains("Сховище"))
			return;

		// Отримуємо правильний GUI сховища
		StorageGUI gui = null;
		try {
			gui = plugin.getStorageManager().getStorage(player, player.hasPermission("storage.admin"));
		} catch (Exception e) {
			plugin.getLogger().warning("Помилка при отриманні GUI сховища для " + player.getName());
			return;
		}

		if (gui == null)
			return;

		// Перевіряємо shift+click з інвентаря гравця (це може переносити предмети в
		// сховище)
		if (event.getClick().isShiftClick()) {
			if (!gui.isAdmin()) {
				event.setCancelled(true);
				player.sendMessage(
						ChatColor.RED + "Shift+Click заборонено! Ви можете тільки забирати предмети зі сховища!");
				return;
			}
		}

		// Дозволяємо взаємодію з власним інвентарем гравця (нижній інвентар)
		// але тільки якщо це не shift+click для неадмінів
		if (event.getClickedInventory() != null &&
				event.getClickedInventory().getType() == InventoryType.PLAYER) {

			// Якщо це не адмін і це shift+click - блокуємо
			if (!gui.isAdmin() && event.getClick().isShiftClick()) {
				event.setCancelled(true);
				player.sendMessage(
						ChatColor.RED + "Shift+Click заборонено! Ви можете тільки забирати предмети зі сховища!");
				return;
			}

			return; // Дозволяємо всі інші дії в інвентарі гравця
		}

		// Перевіряємо навігаційні кнопки у верхньому інвентарі
		if (event.getClickedInventory() != null &&
				event.getClickedInventory().getType() == InventoryType.CHEST &&
				event.getSlot() >= 45 && event.getSlot() <= 53) {

			event.setCancelled(true);

			// Обробка навігації
			if (event.getSlot() == 45) {
				// Кнопка "Попередня сторінка"
				gui.previousPage();
				player.sendMessage(ChatColor.translateAlternateColorCodes('&',
						plugin.getConfig().getString("messages.previous-page", "&aПопередня сторінка")));
			} else if (event.getSlot() == 53) {
				// Кнопка "Наступна сторінка"
				gui.nextPage();
				player.sendMessage(ChatColor.translateAlternateColorCodes('&',
						plugin.getConfig().getString("messages.next-page", "&aНаступна сторінка")));
			}
			return;
		}

		// Якщо це не адмін - застосовуємо обмеження
		if (!gui.isAdmin()) {
			// Перевіряємо, чи клік відбувається в зоні сховища (слоти 0-44)
			if (event.getSlot() >= 0 && event.getSlot() <= 44) {

				// Забороняємо всі типи кліків, крім лівого та правого для забирання
				if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) {
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + "Ця дія заборонена у сховищі!");
					return;
				}

				// Перевіряємо, чи у гравця щось є на курсорі
				ItemStack cursor = event.getCursor();
				if (cursor != null && cursor.getType() != Material.AIR) {
					// Гравець тримає щось на курсорі - забороняємо покласти
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + "Ви можете тільки забирати предмети зі сховища!");
					return;
				}

				// Перевіряємо, чи є предмет у слоті
				ItemStack clickedItem = event.getCurrentItem();
				if (clickedItem == null || clickedItem.getType() == Material.AIR) {
					// Слот порожній - забороняємо будь-які дії
					event.setCancelled(true);
					player.sendMessage(ChatColor.RED + "Ви можете тільки забирати предмети зі сховища!");
					return;
				}

				// Якщо дійшли до цього місця - гравець забирає предмет зі сховища
				// Дозволяємо і зберігаємо зміни
				final StorageGUI finalGui = gui;
				Bukkit.getScheduler().runTask(plugin, () -> {
					finalGui.saveStorage();
				});
				return;
			}

			// Клік поза зоною сховища - скасовуємо
			event.setCancelled(true);

		} else {
			// Для адмінів дозволяємо все в зоні сховища і зберігаємо зміни
			if (event.getSlot() >= 0 && event.getSlot() <= 44) {
				final StorageGUI finalGui = gui;
				Bukkit.getScheduler().runTask(plugin, () -> {
					finalGui.saveStorage();
				});
			}
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player))
			return;
		Player player = (Player) event.getPlayer();

		// Перевіряємо, чи це сховище
		if (event.getView().getTitle() == null || !event.getView().getTitle().contains("Сховище"))
			return;

		// Отримуємо GUI сховища
		StorageGUI gui = plugin.getStorageManager().getStorage(player, player.hasPermission("storage.admin"));
		if (gui == null)
			return;

		// Зберігаємо зміни
		gui.saveStorage();

		// Закриваємо сховище
		plugin.getStorageManager().closeStorage(player);
		player.sendMessage(ChatColor.translateAlternateColorCodes('&',
				plugin.getConfig().getString("messages.storage-closed", "&aСховище закрито!")));
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		try {
			StorageGUI gui = plugin.getStorageManager().getStorage(player, player.hasPermission("storage.admin"));
			if (gui != null) {
				gui.saveStorage();
				plugin.getStorageManager().closeStorage(player);
			}
		} catch (Exception e) {
			plugin.getLogger().warning("Помилка при збереженні сховища гравця " + player.getName() + " при виході");
		}
	}
}