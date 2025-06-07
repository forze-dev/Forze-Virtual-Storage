package com.forze.forzestorage.managers;

import com.forze.forzestorage.ForzeStorage;
import com.forze.forzestorage.gui.StorageGUI;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageManager {
    private final ForzeStorage plugin;
    private final Map<UUID, StorageGUI> openStorages;
    private final File storageFolder;

    public StorageManager(ForzeStorage plugin) {
        this.plugin = plugin;
        this.openStorages = new HashMap<>();
        this.storageFolder = new File(plugin.getDataFolder(), "storages");
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }
    }

    public StorageGUI getStorage(Player player, boolean isAdmin) {
        UUID uuid = player.getUniqueId();
        // ВИПРАВЛЕННЯ: Завжди повертаємо той самий об'єкт для гравця
        StorageGUI gui = openStorages.get(uuid);
        if (gui == null) {
            gui = new StorageGUI(plugin, player, isAdmin);
            openStorages.put(uuid, gui);
            plugin.getLogger().info("Створено новий StorageGUI для " + player.getName());
        }
        return gui;
    }

    public StorageGUI getStorage(Player viewer, OfflinePlayer target) {
        UUID uuid = viewer.getUniqueId();
        // ВИПРАВЛЕННЯ: Для перегляду чужого сховища також зберігаємо консистентність
        StorageGUI gui = openStorages.get(uuid);
        if (gui == null || !gui.getOwner().getUniqueId().equals(target.getUniqueId())) {
            // Якщо GUI не існує або відкрито інше сховище - створюємо новий
            gui = new StorageGUI(plugin, viewer, target, true);
            openStorages.put(uuid, gui);
            plugin.getLogger().info(
                    "Створено новий StorageGUI для " + viewer.getName() + " (перегляд сховища " + target.getName() + ")");
        }
        return gui;
    }

    // ВИПРАВЛЕННЯ: Додано метод для отримання відкритого сховища
    public StorageGUI getOpenStorage(UUID viewerUuid) {
        return openStorages.get(viewerUuid);
    }

    public void closeStorage(Player player) {
        UUID uuid = player.getUniqueId();
        StorageGUI gui = openStorages.remove(uuid);
        if (gui != null) {
            plugin.getLogger().info("Закрито StorageGUI для " + player.getName());
        }
    }

    public void addItem(OfflinePlayer player, ItemStack item, int amount) {
        try {
            File storageFile = getStorageFile(player.getUniqueId());
            FileConfiguration config = YamlConfiguration.loadConfiguration(storageFile);

            // Знаходимо вільний слот
            int slot = 0;
            while (config.contains("items." + slot + ".material") && 
                   config.contains("items." + slot + ".amount") &&
                   config.getInt("items." + slot + ".amount") > 0) {
                slot++;
                // Захист від нескінченного циклу
                if (slot > 10000) {
                    plugin.getLogger()
                            .warning("Сховище гравця " + player.getName() + " переповнено! Не вдалося додати предмет.");
                    return;
                }
            }

            // Зберігаємо предмет використовуючи новий формат
            config.set("items." + slot + ".material", item.getType().name());
            config.set("items." + slot + ".amount", amount);

            // Якщо предмет має додаткові дані
            if (item.hasItemMeta() || item.getEnchantments().size() > 0) {
                config.set("items." + slot + ".data", item.serialize());
            }

            // Зберігаємо файл
            config.save(storageFile);

            plugin.getLogger().info("Додано " + amount + " " + item.getType().name() + " до сховища гравця "
                    + player.getName() + " (слот " + slot + ")");

        } catch (IOException e) {
            plugin.getLogger()
                    .severe("Не вдалося зберегти предмет до сховища гравця " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            plugin.getLogger().severe(
                    "Невідома помилка при додаванні предмета до сховища гравця " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveAllStorages() {
        plugin.getLogger().info("Збереження всіх відкритих сховищ (" + openStorages.size() + ")");
        
        for (Map.Entry<UUID, StorageGUI> entry : openStorages.entrySet()) {
            try {
                StorageGUI gui = entry.getValue();
                if (gui != null && gui.getViewer() != null && gui.getViewer().isOnline()) {
                    gui.saveStorage();
                    plugin.getLogger().info("Збережено сховище для " + gui.getViewer().getName());
                } else {
                    plugin.getLogger().warning("Пропущено збереження для відключеного гравця: " + entry.getKey());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Помилка при збереженні сховища " + entry.getKey() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private File getStorageFile(UUID uuid) {
        return new File(storageFolder, uuid.toString() + ".yml");
    }

    public FileConfiguration getStorageConfig(UUID uuid) {
        File file = getStorageFile(uuid);
        if (!file.exists()) {
            try {
                file.createNewFile();
                plugin.getLogger().info("Створено новий файл сховища для гравця: " + uuid.toString());
            } catch (IOException e) {
                plugin.getLogger()
                        .severe("Не вдалося створити файл сховища для " + uuid.toString() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void saveStorageConfig(UUID uuid, FileConfiguration config) {
        try {
            config.save(getStorageFile(uuid));
            plugin.getLogger().info("Збережено конфігурацію сховища для " + uuid.toString());
        } catch (IOException e) {
            plugin.getLogger()
                    .severe("Не вдалося зберегти конфігурацію сховища для " + uuid.toString() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ВИПРАВЛЕННЯ: Покращений метод діагностики
    public void debugStorage(UUID uuid) {
        FileConfiguration config = getStorageConfig(uuid);
        plugin.getLogger().info("=== Діагностика сховища " + uuid.toString() + " ===");
        plugin.getLogger().info("Файл існує: " + getStorageFile(uuid).exists());
        plugin.getLogger().info("Розмір файлу: " + getStorageFile(uuid).length() + " байт");

        if (config.contains("items")) {
            plugin.getLogger().info("Знайдено секцію items");
            int totalItems = 0;
            int maxSlot = -1;
            
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    String material = config.getString("items." + key + ".material", "UNKNOWN");
                    int amount = config.getInt("items." + key + ".amount", 0);
                    
                    if (amount > 0) {
                        totalItems++;
                        maxSlot = Math.max(maxSlot, slot);
                        plugin.getLogger().info("Слот " + key + ": " + material + " x" + amount);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Неправильний номер слоту: " + key);
                }
            }
            
            plugin.getLogger().info("Загалом предметів: " + totalItems);
            plugin.getLogger().info("Максимальний слот: " + maxSlot);
            
            if (maxSlot >= 0) {
                int maxPage = (maxSlot / 45) + 1;
                plugin.getLogger().info("Кількість сторінок: " + maxPage);
            }
        } else {
            plugin.getLogger().info("Секція items не знайдена");
        }
    }

    // ВИПРАВЛЕННЯ: Метод для очищення неактивних GUI
    public void cleanupInactiveGUIs() {
        openStorages.entrySet().removeIf(entry -> {
            StorageGUI gui = entry.getValue();
            if (gui == null || gui.getViewer() == null || !gui.getViewer().isOnline()) {
                plugin.getLogger().info("Очищено неактивний GUI для " + entry.getKey());
                return true;
            }
            return false;
        });
    }

    // ВИПРАВЛЕННЯ: Метод для отримання статистики
    public void printStatistics() {
        plugin.getLogger().info("=== Статистика StorageManager ===");
        plugin.getLogger().info("Відкритих сховищ: " + openStorages.size());
        
        for (Map.Entry<UUID, StorageGUI> entry : openStorages.entrySet()) {
            StorageGUI gui = entry.getValue();
            if (gui != null && gui.getViewer() != null) {
                plugin.getLogger().info("  " + gui.getViewer().getName() + 
                                      " -> " + gui.getOwner().getName() + 
                                      " (сторінка " + gui.getCurrentPage() + 
                                      ", навігація: " + gui.isNavigating() + ")");
            }
        }
    }
}