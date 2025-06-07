package com.forze.forzestorage.listeners;

import com.forze.forzestorage.ForzeStorage;
import com.forze.forzestorage.gui.StorageGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StorageListener implements Listener {
	private final ForzeStorage plugin;
	private final Set<UUID> navigatingPlayers; // Захист від множинних кліків навігації

	public StorageListener(ForzeStorage plugin) {
		this.plugin = plugin;
		this.navigatingPlayers = new HashSet<>();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent event) {
		// Базові перевірки
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getWhoClicked();

		// Перевіряємо, чи це наше сховище
		if (!isStorageInventory(event)) {
			return;
		}

		plugin.getLogger().info("Клік у сховищі: гравець=" + player.getName() +
				", слот=" + event.getSlot() +
				", тип кліку=" + event.getClick() +
				", інвентар=" + (event.getClickedInventory() != null ? event.getClickedInventory().getType() : "null"));

		// Отримуємо GUI сховища
		StorageGUI gui = getStorageGUI(player);
		if (gui == null) {
			plugin.getLogger().warning("Не вдалося отримати GUI сховища для " + player.getName());
			event.setCancelled(true);
			return;
		}

		// Обробляємо навігаційні кнопки (слоти 45-53 у верхньому інвентарі)
		if (isNavigationClick(event)) {
			handleNavigationClick(event, player, gui);
			return;
		}

		// Обробляємо shift+click з інвентаря гравця до сховища
		if (isShiftClickFromPlayerInventory(event)) {
			handleShiftClickFromPlayer(event, player, gui);
			return;
		}

		// Дозволяємо вільну взаємодію з інвентарем гравця
		if (isPlayerInventoryClick(event)) {
			// Дозволяємо всі дії в інвентарі гравця
			return;
		}

		// Обробляємо кліки в зоні сховища (слоти 0-44)
		if (isStorageAreaClick(event)) {
			handleStorageAreaClick(event, player, gui);
			return;
		}

		// Всі інші кліки блокуємо
		event.setCancelled(true);
		plugin.getLogger().info("Заблоковано невідомий тип кліку");
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getPlayer();

		// Перевіряємо, чи це наше сховище
		if (!isStorageInventory(event.getView().getTitle())) {
			return;
		}

		plugin.getLogger().info("Закриття сховища гравцем " + player.getName());

		// Отримуємо та зберігаємо GUI
		StorageGUI gui = getStorageGUI(player);
		if (gui != null) {
			gui.saveStorage();
			plugin.getLogger().info("Збережено сховище при закритті для " + player.getName());
		}

		// Очищаємо з менеджера і захисту навігації
		plugin.getStorageManager().closeStorage(player);
		navigatingPlayers.remove(player.getUniqueId());

		// Відправляємо повідомлення
		player.sendMessage(ChatColor.translateAlternateColorCodes('&',
				plugin.getConfig().getString("messages.storage-closed", "&aСховище закрито!")));
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		UUID playerId = player.getUniqueId();

		plugin.getLogger().info("Гравець " + player.getName() + " виходить з гри, зберігаємо сховище");

		try {
			// Отримуємо та зберігаємо GUI
			StorageGUI gui = getStorageGUI(player);
			if (gui != null) {
				gui.saveStorage();
				plugin.getLogger().info("Збережено сховище при виході для " + player.getName());
			}
		} catch (Exception e) {
			plugin.getLogger().severe("Помилка при збереженні сховища гравця " + player.getName() +
					" при виході: " + e.getMessage());
			e.printStackTrace();
		} finally {
			// Завжди очищаємо дані
			plugin.getStorageManager().closeStorage(player);
			navigatingPlayers.remove(playerId);
		}
	}

	// ===== ПРИВАТНІ МЕТОДИ =====

	private boolean isStorageInventory(InventoryClickEvent event) {
		return event.getView() != null &&
				event.getView().getTitle() != null &&
				event.getView().getTitle().contains("Сховище");
	}

	private boolean isStorageInventory(String title) {
		return title != null && title.contains("Сховище");
	}

	private StorageGUI getStorageGUI(Player player) {
		try {
			boolean isAdmin = player.hasPermission("storage.admin");
			return plugin.getStorageManager().getStorage(player, isAdmin);
		} catch (Exception e) {
			plugin.getLogger()
					.warning("Помилка при отриманні GUI сховища для " + player.getName() + ": " + e.getMessage());
			return null;
		}
	}

	private boolean isNavigationClick(InventoryClickEvent event) {
		return event.getClickedInventory() != null &&
				event.getClickedInventory().getType() == InventoryType.CHEST &&
				event.getSlot() >= 45 && event.getSlot() <= 53;
	}

	private boolean isShiftClickFromPlayerInventory(InventoryClickEvent event) {
		return event.getClick().isShiftClick() &&
				event.getClickedInventory() != null &&
				event.getClickedInventory().getType() == InventoryType.PLAYER;
	}

	private boolean isPlayerInventoryClick(InventoryClickEvent event) {
		return event.getClickedInventory() != null &&
				event.getClickedInventory().getType() == InventoryType.PLAYER;
	}

	private boolean isStorageAreaClick(InventoryClickEvent event) {
		return event.getClickedInventory() != null &&
				event.getClickedInventory().getType() == InventoryType.CHEST &&
				event.getSlot() >= 0 && event.getSlot() <= 44;
	}

	private void handleNavigationClick(InventoryClickEvent event, Player player, StorageGUI gui) {
		event.setCancelled(true);

		UUID playerId = player.getUniqueId();
		int slot = event.getSlot();

		// Захист від множинних кліків
		if (navigatingPlayers.contains(playerId)) {
			plugin.getLogger().info("Ігноруємо навігаційний клік - гравець " + player.getName() + " вже навігує");
			return;
		}

		ItemStack clickedItem = event.getCurrentItem();
		if (clickedItem == null || clickedItem.getType() != Material.ARROW) {
			plugin.getLogger().info("Клік на неактивну навігаційну кнопку, слот " + slot);
			return;
		}

		// Додаємо захист
		navigatingPlayers.add(playerId);

		plugin.getLogger().info("Обробка навігаційного кліку: слот=" + slot + ", гравець=" + player.getName());

		// Виконуємо навігацію з затримкою
		Bukkit.getScheduler().runTask(plugin, () -> {
			try {
				if (slot == 45) {
					// Попередня сторінка
					plugin.getLogger().info("Перехід на попередню сторінку");
					gui.previousPage();
					player.sendMessage(ChatColor.translateAlternateColorCodes('&',
							plugin.getConfig().getString("messages.previous-page", "&aПопередня сторінка")));
				} else if (slot == 53) {
					// Наступна сторінка
					plugin.getLogger().info("Перехід на наступну сторінку");
					gui.nextPage();
					player.sendMessage(ChatColor.translateAlternateColorCodes('&',
							plugin.getConfig().getString("messages.next-page", "&aНаступна сторінка")));
				}
			} catch (Exception e) {
				plugin.getLogger().severe("Помилка при навігації для " + player.getName() + ": " + e.getMessage());
				e.printStackTrace();
			} finally {
				// Знімаємо захист через кілька тіків
				Bukkit.getScheduler().runTaskLater(plugin, () -> {
					navigatingPlayers.remove(playerId);
					plugin.getLogger().info("Знято захист навігації для " + player.getName());
				}, 10L); // 0.5 секунди
			}
		});
	}

	private void handleShiftClickFromPlayer(InventoryClickEvent event, Player player, StorageGUI gui) {
		if (!gui.isAdmin()) {
			event.setCancelled(true);
			player.sendMessage(ChatColor.RED + "Shift+Click заборонено! Ви можете тільки забирати предмети зі сховища!");
			plugin.getLogger().info("Заблоковано Shift+Click для неадміна " + player.getName());
		} else {
			plugin.getLogger().info("Дозволено Shift+Click для адміна " + player.getName());
			// Для адмінів дозволяємо і зберігаємо зміни
			scheduleStorageSave(gui);
		}
	}

	private void handleStorageAreaClick(InventoryClickEvent event, Player player, StorageGUI gui) {
		if (!gui.isAdmin()) {
			// Для звичайних гравців - тільки забирання предметів
			handleNonAdminStorageClick(event, player);
		} else {
			// Для адмінів - повна свобода дій
			handleAdminStorageClick(event, gui);
		}
	}

	private void handleNonAdminStorageClick(InventoryClickEvent event, Player player) {
		// Дозволяємо тільки ліві та праві кліки
		if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) {
			event.setCancelled(true);
			player.sendMessage(ChatColor.RED + "Ця дія заборонена у сховищі!");
			plugin.getLogger().info("Заблоковано заборонений тип кліку " + event.getClick() + " для " + player.getName());
			return;
		}

		// Перевіряємо курсор гравця
		ItemStack cursor = event.getCursor();
		if (cursor != null && cursor.getType() != Material.AIR) {
			event.setCancelled(true);
			player.sendMessage(ChatColor.RED + "Ви можете тільки забирати предмети зі сховища!");
			plugin.getLogger().info("Заблоковано спробу покласти предмет у сховище для " + player.getName());
			return;
		}

		// Перевіряємо наявність предмета в слоті
		ItemStack clickedItem = event.getCurrentItem();
		if (clickedItem == null || clickedItem.getType() == Material.AIR) {
			event.setCancelled(true);
			player.sendMessage(ChatColor.RED + "Ви можете тільки забирати предмети зі сховища!");
			plugin.getLogger().info("Заблоковано клік по порожньому слоту для " + player.getName());
			return;
		}

		// Якщо дійшли сюди - гравець забирає предмет
		plugin.getLogger().info("Дозволено забирання предмета " + clickedItem.getType() + " гравцем " + player.getName());

		// Зберігаємо зміни
		StorageGUI gui = getStorageGUI(player);
		if (gui != null) {
			scheduleStorageSave(gui);
		}
	}

	private void handleAdminStorageClick(InventoryClickEvent event, StorageGUI gui) {
		plugin.getLogger().info("Дозволено адмінську дію у сховищі");

		// Для адмінів дозволяємо все і зберігаємо зміни
		scheduleStorageSave(gui);
	}

	private void scheduleStorageSave(StorageGUI gui) {
		// Зберігаємо з мінімальною затримкою для консистентності
		Bukkit.getScheduler().runTask(plugin, () -> {
			try {
				gui.saveStorage();
			} catch (Exception e) {
				plugin.getLogger().warning("Помилка при збереженні сховища: " + e.getMessage());
			}
		});
	}
}