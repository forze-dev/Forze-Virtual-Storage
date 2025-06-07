package com.forze.forzestorage.gui;

import com.forze.forzestorage.ForzeStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class StorageGUI {
	private final ForzeStorage plugin;
	private final Player viewer;
	private final OfflinePlayer owner;
	private final boolean isAdmin;
	private int currentPage = 0;
	private static final int ITEMS_PER_PAGE = 45;
	private boolean isNavigating = false; // ВИПРАВЛЕННЯ: Флаг для запобігання конфліктам навігації

	public StorageGUI(ForzeStorage plugin, Player viewer, boolean isAdmin) {
		this.plugin = plugin;
		this.viewer = viewer;
		this.owner = viewer;
		this.isAdmin = isAdmin;
	}

	public StorageGUI(ForzeStorage plugin, Player viewer, OfflinePlayer owner, boolean isAdmin) {
		this.plugin = plugin;
		this.viewer = viewer;
		this.owner = owner;
		this.isAdmin = isAdmin;
	}

	public void open() {
		// ВИПРАВЛЕННЯ: Перевіряємо, чи не відбувається навігація
		if (isNavigating) {
			plugin.getLogger().info("Навігація вже відбувається, пропускаємо відкриття");
			return;
		}

		Inventory inv = Bukkit.createInventory(null, 54, getTitle());
		loadItems(inv);
		setupNavigation(inv);
		viewer.openInventory(inv);

		plugin.getLogger().info("Відкрито сховище " + owner.getName() + " для " + viewer.getName() + " (сторінка "
				+ (currentPage + 1) + ")");
	}

	private String getTitle() {
		String title = plugin.getConfig().getString("messages.storage-title", "&8Сховище %player%");
		return ChatColor.translateAlternateColorCodes('&', title.replace("%player%", owner.getName()));
	}

	private void loadItems(Inventory inv) {
		// Спочатку очищаємо всі слоти сховища
		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			inv.setItem(i, new ItemStack(Material.AIR));
		}

		FileConfiguration config = plugin.getStorageManager().getStorageConfig(owner.getUniqueId());
		int startIndex = currentPage * ITEMS_PER_PAGE;

		plugin.getLogger().info("Завантаження сторінки " + (currentPage + 1) + " для " + owner.getName()
				+ " (індекси " + startIndex + "-" + (startIndex + ITEMS_PER_PAGE - 1) + ")");

		int loadedItems = 0;
		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			int globalSlot = startIndex + i;
			String path = "items." + globalSlot;

			if (config.contains(path + ".material") && config.contains(path + ".amount")) {
				try {
					String materialName = config.getString(path + ".material");
					int amount = config.getInt(path + ".amount");

					if (materialName == null || amount <= 0) {
						continue; // Пропускаємо некоректні записи
					}

					Material material = Material.valueOf(materialName);
					ItemStack item = new ItemStack(material, amount);

					// Якщо є додаткові дані (enchantments, meta тощо)
					if (config.contains(path + ".data")) {
						try {
							ItemStack serializedItem = ItemStack.deserialize(
									config.getConfigurationSection(path + ".data").getValues(true));
							serializedItem.setAmount(amount);
							item = serializedItem;
						} catch (Exception e) {
							plugin.getLogger().warning(
									"Не вдалося десеріалізувати додаткові дані для предмета в слоті " + globalSlot);
						}
					}

					inv.setItem(i, item);
					loadedItems++;
					plugin.getLogger().info("Завантажено предмет у слот " + i + " (глобальний " + globalSlot + "): "
							+ materialName + " x" + amount);
				} catch (IllegalArgumentException e) {
					plugin.getLogger()
							.warning("Невідомий матеріал у сховищі " + owner.getName() + " слот " + globalSlot + ": "
									+ config.getString(path + ".material"));
				} catch (Exception e) {
					plugin.getLogger().warning("Не вдалося завантажити предмет зі сховища " + owner.getName() + " слот "
							+ globalSlot + ": " + e.getMessage());
				}
			}
		}

		plugin.getLogger().info("Завантажено " + loadedItems + " предметів на сторінку " + (currentPage + 1));
	}

	private void setupNavigation(Inventory inv) {
		// Заповнюємо навігаційні слоти повітрям для очищення
		for (int i = 45; i <= 53; i++) {
			inv.setItem(i, new ItemStack(Material.AIR));
		}

		// Кнопка "Попередня сторінка"
		if (currentPage > 0) {
			ItemStack prevPage = createNavigationItem(Material.ARROW,
					plugin.getConfig().getString("messages.previous-page", "&aПопередня сторінка"));
			inv.setItem(45, prevPage);
		}

		// Інформація про поточну сторінку
		ItemStack info = createNavigationItem(Material.PAPER,
				plugin.getConfig().getString("messages.page-changed", "&aСторінка %page%")
						.replace("%page%", String.valueOf(currentPage + 1)));
		inv.setItem(49, info);

		// Кнопка "Наступна сторінка"
		if (hasNextPage()) {
			ItemStack nextPage = createNavigationItem(Material.ARROW,
					plugin.getConfig().getString("messages.next-page", "&aНаступна сторінка"));
			inv.setItem(53, nextPage);
		}

		plugin.getLogger().info("Налаштування навігації: попередня=" + (currentPage > 0) + ", наступна=" + hasNextPage());
	}

	private boolean hasNextPage() {
		FileConfiguration config = plugin.getStorageManager().getStorageConfig(owner.getUniqueId());
		int nextPageStart = (currentPage + 1) * ITEMS_PER_PAGE;

		// Перевіряємо чи є хоча б один предмет на наступній сторінці
		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			String path = "items." + (nextPageStart + i);
			if (config.contains(path + ".material") && config.contains(path + ".amount")) {
				String material = config.getString(path + ".material");
				int amount = config.getInt(path + ".amount");
				if (material != null && amount > 0) {
					plugin.getLogger()
							.info("Знайдено предмет для наступної сторінки в слоті " + (nextPageStart + i) + ": " + material);
					return true;
				}
			}
		}

		plugin.getLogger().info("Наступна сторінка порожня");
		return false;
	}

	private ItemStack createNavigationItem(Material material, String name) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
			item.setItemMeta(meta);
		}
		return item;
	}

	public void nextPage() {
		// ВИПРАВЛЕННЯ: Захист від множинних викликів
		if (isNavigating) {
			plugin.getLogger().info("Навігація вже відбувається, пропускаємо nextPage");
			return;
		}

		if (hasNextPage()) {
			isNavigating = true;
			plugin.getLogger().info("Переключення на наступну сторінку: " + currentPage + " -> " + (currentPage + 1));

			// Спочатку зберігаємо поточну сторінку
			saveStorage();

			// Потім переключаємося на наступну
			currentPage++;

			// Відкриваємо нову сторінку в наступному тіку
			Bukkit.getScheduler().runTask(plugin, () -> {
				open();
				isNavigating = false;
			});
		} else {
			plugin.getLogger().info("Наступна сторінка недоступна");
		}
	}

	public void previousPage() {
		// ВИПРАВЛЕННЯ: Захист від множинних викликів
		if (isNavigating) {
			plugin.getLogger().info("Навігація вже відбувається, пропускаємо previousPage");
			return;
		}

		if (currentPage > 0) {
			isNavigating = true;
			plugin.getLogger().info("Переключення на попередню сторінку: " + currentPage + " -> " + (currentPage - 1));

			// Спочатку зберігаємо поточну сторінку
			saveStorage();

			// Потім переключаємося на попередню
			currentPage--;

			// Відкриваємо нову сторінку в наступному тіку
			Bukkit.getScheduler().runTask(plugin, () -> {
				open();
				isNavigating = false;
			});
		} else {
			plugin.getLogger().info("Попередня сторінка недоступна");
		}
	}

	public void saveStorage() {
		// ВИПРАВЛЕННЯ: Додаємо додаткові перевірки
		if (viewer == null || !viewer.isOnline()) {
			plugin.getLogger().info("Збереження скасовано - гравець не онлайн");
			return;
		}

		// Перевіряємо, чи відкритий інвентар це наше сховище
		if (viewer.getOpenInventory() == null ||
				viewer.getOpenInventory().getTitle() == null ||
				!viewer.getOpenInventory().getTitle().contains("Сховище")) {
			plugin.getLogger().info("Збереження скасовано - інвентар не є сховищем");
			return;
		}

		Inventory inv = viewer.getOpenInventory().getTopInventory();
		if (inv == null || inv.getSize() != 54) {
			plugin.getLogger().info("Збереження скасовано - неправильний розмір інвентаря");
			return;
		}

		FileConfiguration config = plugin.getStorageManager().getStorageConfig(owner.getUniqueId());
		int startIndex = currentPage * ITEMS_PER_PAGE;

		plugin.getLogger().info("Збереження сторінки " + (currentPage + 1) + " для " + owner.getName()
				+ " (індекси " + startIndex + "-" + (startIndex + ITEMS_PER_PAGE - 1) + ")");

		// ВИПРАВЛЕННЯ: Спочатку очищуємо тільки поточну сторінку
		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			int globalSlot = startIndex + i;
			String path = "items." + globalSlot;
			config.set(path, null);
		}

		// Зберігаємо тільки предмети з верхнього інвентаря (сховища)
		int savedItems = 0;
		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			ItemStack item = inv.getItem(i);
			int globalSlot = startIndex + i;
			String path = "items." + globalSlot;

			if (item != null && item.getType() != Material.AIR) {
				try {
					config.set(path + ".material", item.getType().name());
					config.set(path + ".amount", item.getAmount());

					// Зберігаємо додаткові дані тільки якщо вони є
					if (item.hasItemMeta() || item.getEnchantments().size() > 0) {
						config.set(path + ".data", item.serialize());
					}

					savedItems++;
					plugin.getLogger()
							.info("Збережено предмет у глобальний слот " + globalSlot + " (локальний " + i + "): "
									+ item.getType().name()
									+ " x" + item.getAmount());
				} catch (Exception e) {
					plugin.getLogger().warning("Не вдалося зберегти предмет у сховище " + owner.getName() + " слот "
							+ globalSlot + ": " + e.getMessage());
				}
			}
		}

		// ВИПРАВЛЕННЯ: Зберігаємо конфігурацію одразу
		plugin.getStorageManager().saveStorageConfig(owner.getUniqueId(), config);
		plugin.getLogger().info("Збережено " + savedItems + " предметів на сторінці " + (currentPage + 1));
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public Player getViewer() {
		return viewer;
	}

	public OfflinePlayer getOwner() {
		return owner;
	}

	public int getCurrentPage() {
		return currentPage;
	}

	// ВИПРАВЛЕННЯ: Додаємо метод для діагностики
	public void debugCurrentState() {
		plugin.getLogger().info("=== Стан StorageGUI ===");
		plugin.getLogger().info("Viewer: " + viewer.getName());
		plugin.getLogger().info("Owner: " + owner.getName());
		plugin.getLogger().info("Current Page: " + currentPage);
		plugin.getLogger().info("Is Admin: " + isAdmin);
		plugin.getLogger().info("Is Navigating: " + isNavigating);
		plugin.getLogger().info("Has Next Page: " + hasNextPage());
	}
}