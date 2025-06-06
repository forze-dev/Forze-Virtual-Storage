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
		Inventory inv = Bukkit.createInventory(null, 54, getTitle());
		loadItems(inv);
		setupNavigation(inv);
		viewer.openInventory(inv);
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
			String path = "items." + (startIndex + i);
			if (config.contains(path + ".material") && config.contains(path + ".amount")) {
				try {
					String materialName = config.getString(path + ".material");
					int amount = config.getInt(path + ".amount");

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
									"Не вдалося десеріалізувати додаткові дані для предмета в слоті " + (startIndex + i));
						}
					}

					inv.setItem(i, item);
					loadedItems++;
					plugin.getLogger().info("Завантажено предмет у слот " + i + ": " + materialName + " x" + amount);
				} catch (IllegalArgumentException e) {
					plugin.getLogger()
							.warning("Невідомий матеріал у сховищі " + owner.getName() + " слот " + (startIndex + i));
				} catch (Exception e) {
					plugin.getLogger().warning("Не вдалося завантажити предмет зі сховища " + owner.getName() + " слот "
							+ (startIndex + i) + ": " + e.getMessage());
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
				plugin.getLogger().info("Знайдено предмет для наступної сторінки в слоті " + (nextPageStart + i));
				return true;
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
		if (hasNextPage()) {
			plugin.getLogger().info("Переключення на наступну сторінку: " + currentPage + " -> " + (currentPage + 1));

			// Спочатку зберігаємо поточну сторінку
			saveStorage();

			// Потім переключаємося на наступну
			currentPage++;

			// І відкриваємо нову сторінку
			open();
		} else {
			plugin.getLogger().info("Наступна сторінка недоступна");
		}
	}

	public void previousPage() {
		if (currentPage > 0) {
			plugin.getLogger().info("Переключення на попередню сторінку: " + currentPage + " -> " + (currentPage - 1));

			// Спочатку зберігаємо поточну сторінку
			saveStorage();

			// Потім переключаємося на попередню
			currentPage--;

			// І відкриваємо нову сторінку
			open();
		} else {
			plugin.getLogger().info("Попередня сторінка недоступна");
		}
	}

	public void saveStorage() {
		// Перевіряємо, чи відкритий інвентар це наше сховище
		if (viewer.getOpenInventory() == null ||
				viewer.getOpenInventory().getTitle() == null ||
				!viewer.getOpenInventory().getTitle().contains("Сховище")) {
			plugin.getLogger().info("Збереження скасовано - інвентар не є сховищем");
			return;
		}

		Inventory inv = viewer.getOpenInventory().getTopInventory();
		FileConfiguration config = plugin.getStorageManager().getStorageConfig(owner.getUniqueId());
		int startIndex = currentPage * ITEMS_PER_PAGE;

		plugin.getLogger().info("Збереження сторінки " + (currentPage + 1) + " для " + owner.getName()
				+ " (індекси " + startIndex + "-" + (startIndex + ITEMS_PER_PAGE - 1) + ")");

		// Очищуємо поточну сторінку перед збереженням
		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			String path = "items." + (startIndex + i);
			config.set(path, null);
		}

		// Зберігаємо тільки предмети з верхнього інвентаря (сховища)
		int savedItems = 0;
		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			ItemStack item = inv.getItem(i);
			String path = "items." + (startIndex + i);

			if (item != null && item.getType() != Material.AIR) {
				try {
					config.set(path + ".material", item.getType().name());
					config.set(path + ".amount", item.getAmount());

					// Зберігаємо додаткові дані тільки якщо вони є
					if (item.hasItemMeta() || item.getEnchantments().size() > 0) {
						config.set(path + ".data", item.serialize());
					}

					savedItems++;
					plugin.getLogger().info("Збережено предмет у слот " + (startIndex + i) + ": " + item.getType().name()
							+ " x" + item.getAmount());
				} catch (Exception e) {
					plugin.getLogger().warning("Не вдалося зберегти предмет у сховище " + owner.getName() + " слот "
							+ (startIndex + i) + ": " + e.getMessage());
				}
			}
		}

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
}