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
		FileConfiguration config = plugin.getStorageManager().getStorageConfig(owner.getUniqueId());
		int startIndex = currentPage * ITEMS_PER_PAGE;

		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			String path = "items." + (startIndex + i);
			if (config.contains(path + ".material") && config.contains(path + ".amount")) {
				try {
					// ВИПРАВЛЕННЯ: використовуємо простіший спосіб збереження
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
							// Якщо десеріалізація не вдалася, використовуємо простий предмет
							plugin.getLogger().warning(
									"Не вдалося десеріалізувати додаткові дані для предмета в слоті " + (startIndex + i));
						}
					}

					inv.setItem(i, item);
				} catch (IllegalArgumentException e) {
					plugin.getLogger()
							.warning("Невідомий матеріал у сховищі " + owner.getName() + " слот " + (startIndex + i));
				} catch (Exception e) {
					plugin.getLogger().warning("Не вдалося завантажити предмет зі сховища " + owner.getName() + " слот "
							+ (startIndex + i) + ": " + e.getMessage());
				}
			}
		}
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
	}

	private boolean hasNextPage() {
		FileConfiguration config = plugin.getStorageManager().getStorageConfig(owner.getUniqueId());
		int nextPageStart = (currentPage + 1) * ITEMS_PER_PAGE;

		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			if (config.contains("items." + (nextPageStart + i))) {
				return true;
			}
		}
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
			currentPage++;
			saveStorage();
			open();
		}
	}

	public void previousPage() {
		if (currentPage > 0) {
			saveStorage();
			currentPage--;
			open();
		}
	}

	public void saveStorage() {
		// Перевіряємо, чи відкритий інвентар це наше сховище
		if (viewer.getOpenInventory() == null ||
				viewer.getOpenInventory().getTitle() == null ||
				!viewer.getOpenInventory().getTitle().contains("Сховище")) {
			return;
		}

		Inventory inv = viewer.getOpenInventory().getTopInventory();
		FileConfiguration config = plugin.getStorageManager().getStorageConfig(owner.getUniqueId());
		int startIndex = currentPage * ITEMS_PER_PAGE;

		// Очищуємо поточну сторінку перед збереженням
		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			String path = "items." + (startIndex + i);
			config.set(path, null);
		}

		// Зберігаємо тільки предмети з верхнього інвентаря (сховища)
		for (int i = 0; i < ITEMS_PER_PAGE; i++) {
			ItemStack item = inv.getItem(i);
			String path = "items." + (startIndex + i);

			if (item != null && item.getType() != Material.AIR) {
				try {
					// ВИПРАВЛЕННЯ: використовуємо простіший спосіб збереження
					config.set(path + ".material", item.getType().name());
					config.set(path + ".amount", item.getAmount());

					// Зберігаємо додаткові дані тільки якщо вони є
					if (item.hasItemMeta() || item.getEnchantments().size() > 0) {
						config.set(path + ".data", item.serialize());
					}
				} catch (Exception e) {
					plugin.getLogger().warning("Не вдалося зберегти предмет у сховище " + owner.getName() + " слот "
							+ (startIndex + i) + ": " + e.getMessage());
				}
			}
		}

		plugin.getStorageManager().saveStorageConfig(owner.getUniqueId(), config);
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