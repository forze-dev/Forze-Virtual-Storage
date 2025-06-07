package com.forze.forzestorage.gui;

import com.forze.forzestorage.ForzeStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StorageGUI {
    private final ForzeStorage plugin;
    private final Player viewer;
    private final OfflinePlayer owner;
    private final boolean isAdmin;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 45;
    private volatile boolean isNavigating = false; // ВИПРАВЛЕННЯ: Volatile для thread safety

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
            plugin.getLogger().info("Навігація вже відбувається, пропускаємо відкриття для " + viewer.getName());
            return;
        }

        // ВИПРАВЛЕННЯ: Додаткові перевірки
        if (viewer == null || !viewer.isOnline()) {
            plugin.getLogger().warning("Спроба відкрити сховище для неактивного гравця");
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

                    // ВИПРАВЛЕННЯ: Безпечна десеріалізація додаткових даних
                    if (config.contains(path + ".data")) {
                        try {
                            ConfigurationSection dataSection = config.getConfigurationSection(path + ".data");
                            if (dataSection != null) {
                                ItemStack serializedItem = ItemStack.deserialize(dataSection.getValues(true));
                                serializedItem.setAmount(amount);
                                item = serializedItem;
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Помилка десеріалізації слоту " + globalSlot + ": " + e.getMessage());
                            // Використовуємо базовий предмет без додаткових даних
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

    // ВИПРАВЛЕННЯ: Переписана навігація з правильною синхронізацією
    public void nextPage() {
        if (isNavigating) {
            plugin.getLogger().info("Навігація вже відбувається для " + viewer.getName());
            return;
        }
        
        if (!hasNextPage()) {
            plugin.getLogger().info("Наступна сторінка недоступна");
            return;
        }
        
        isNavigating = true;
        plugin.getLogger().info("Початок nextPage: " + currentPage + " -> " + (currentPage + 1));
        
        try {
            // 1. СИНХРОННО зберігаємо поточну сторінку
            saveStorage();
            plugin.getLogger().info("Збережено сторінку " + currentPage);
            
            // 2. Змінюємо номер сторінки
            currentPage++;
            
            // 3. З затримкою відкриваємо нову сторінку
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    open();
                } finally {
                    isNavigating = false;
                    plugin.getLogger().info("Навігація завершена для " + viewer.getName());
                }
            }, 5L); // 5 тіків затримки
            
        } catch (Exception e) {
            plugin.getLogger().severe("Помилка навігації nextPage: " + e.getMessage());
            isNavigating = false;
            e.printStackTrace();
        }
    }

    public void previousPage() {
        if (isNavigating) {
            plugin.getLogger().info("Навігація вже відбувається для " + viewer.getName());
            return;
        }
        
        if (currentPage <= 0) {
            plugin.getLogger().info("Попередня сторінка недоступна");
            return;
        }
        
        isNavigating = true;
        plugin.getLogger().info("Початок previousPage: " + currentPage + " -> " + (currentPage - 1));
        
        try {
            // 1. СИНХРОННО зберігаємо поточну сторінку
            saveStorage();
            plugin.getLogger().info("Збережено сторінку " + currentPage);
            
            // 2. Змінюємо номер сторінки
            currentPage--;
            
            // 3. З затримкою відкриваємо нову сторінку
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    open();
                } finally {
                    isNavigating = false;
                    plugin.getLogger().info("Навігація завершена для " + viewer.getName());
                }
            }, 5L); // 5 тіків затримки
            
        } catch (Exception e) {
            plugin.getLogger().severe("Помилка навігації previousPage: " + e.getMessage());
            isNavigating = false;
            e.printStackTrace();
        }
    }

    // ВИПРАВЛЕННЯ: Покращений метод збереження
    public void saveStorage() {
        if (isNavigating) {
            plugin.getLogger().warning("Збереження під час навігації - пропускаємо для " + viewer.getName());
            return;
        }
        
        if (viewer == null || !viewer.isOnline()) {
            plugin.getLogger().warning("Гравець не онлайн - скасовуємо збереження");
            return;
        }

        // Перевіряємо, чи відкритий інвентар це наше сховище
        if (viewer.getOpenInventory() == null ||
                viewer.getOpenInventory().getTitle() == null ||
                !viewer.getOpenInventory().getTitle().contains("Сховище")) {
            plugin.getLogger().warning("Інвентар не є сховищем - скасовуємо збереження");
            return;
        }

        Inventory inv = viewer.getOpenInventory().getTopInventory();
        if (inv == null || inv.getSize() != 54) {
            plugin.getLogger().warning("Неправильний розмір інвентаря - скасовуємо збереження");
            return;
        }

        plugin.getLogger().info("=== ЗБЕРЕЖЕННЯ СТОРІНКИ " + currentPage + " для " + owner.getName() + " ===");
        
        FileConfiguration config = plugin.getStorageManager().getStorageConfig(owner.getUniqueId());
        int startIndex = currentPage * ITEMS_PER_PAGE;

        // Очищуємо ТІЛЬКИ поточну сторінку
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int globalSlot = startIndex + i;
            config.set("items." + globalSlot, null);
        }

        // Зберігаємо предмети з поточної сторінки
        int savedItems = 0;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                int globalSlot = startIndex + i;
                String path = "items." + globalSlot;
                
                try {
                    config.set(path + ".material", item.getType().name());
                    config.set(path + ".amount", item.getAmount());
                    
                    // Зберігаємо додаткові дані тільки якщо вони є
                    if (item.hasItemMeta() || item.getEnchantments().size() > 0) {
                        config.set(path + ".data", item.serialize());
                    }
                    
                    savedItems++;
                    plugin.getLogger().info("Збережено слот " + i + " -> " + globalSlot + 
                                          ": " + item.getType() + " x" + item.getAmount());
                } catch (Exception e) {
                    plugin.getLogger().warning("Не вдалося зберегти предмет у слот " + globalSlot + ": " + e.getMessage());
                }
            }
        }

        // ВИПРАВЛЕННЯ: Зберігаємо конфігурацію одразу
        plugin.getStorageManager().saveStorageConfig(owner.getUniqueId(), config);
        plugin.getLogger().info("=== ЗБЕРЕЖЕННЯ ЗАВЕРШЕНО: " + savedItems + " предметів ===");
    }

    // ВИПРАВЛЕННЯ: Додано діагностику
    public void debugCurrentState() {
        plugin.getLogger().info("=== DEBUG StorageGUI ===");
        plugin.getLogger().info("Viewer: " + viewer.getName());
        plugin.getLogger().info("Owner: " + owner.getName());
        plugin.getLogger().info("Current Page: " + currentPage);
        plugin.getLogger().info("Is Admin: " + isAdmin);
        plugin.getLogger().info("Is Navigating: " + isNavigating);
        plugin.getLogger().info("Has Next Page: " + hasNextPage());
        
        if (viewer.getOpenInventory() != null) {
            Inventory inv = viewer.getOpenInventory().getTopInventory();
            if (inv != null && inv.getSize() == 54) {
                plugin.getLogger().info("Предмети в інвентарі:");
                for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        plugin.getLogger().info("  Слот " + i + ": " + item.getType() + " x" + item.getAmount());
                    }
                }
            } else {
                plugin.getLogger().info("Інвентар недоступний або неправильного розміру");
            }
        } else {
            plugin.getLogger().info("Інвентар не відкритий");
        }
    }

    // Геттери
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

    public boolean isNavigating() {
        return isNavigating;
    }
}