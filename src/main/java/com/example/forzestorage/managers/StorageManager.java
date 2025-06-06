package com.forze.forzestorage.managers;

import com.forze.forzestorage.ForzeStorage;
import com.forze.forzestorage.gui.StorageGUI;
import org.bukkit.Bukkit;
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
        if (!openStorages.containsKey(uuid)) {
            openStorages.put(uuid, new StorageGUI(plugin, player, isAdmin));
        }
        return openStorages.get(uuid);
    }

    public StorageGUI getStorage(Player viewer, OfflinePlayer target) {
        UUID uuid = viewer.getUniqueId();
        if (!openStorages.containsKey(uuid)) {
            openStorages.put(uuid, new StorageGUI(plugin, viewer, target, true));
        }
        return openStorages.get(uuid);
    }

    public void closeStorage(Player player) {
        openStorages.remove(player.getUniqueId());
    }

    public void addItem(OfflinePlayer player, ItemStack item, int amount) {
        File storageFile = getStorageFile(player.getUniqueId());
        FileConfiguration config = YamlConfiguration.loadConfiguration(storageFile);
        
        // Знаходимо вільний слот
        int slot = 0;
        while (config.contains("items." + slot)) {
            slot++;
        }
        
        // Зберігаємо предмет
        config.set("items." + slot + ".item", item.serialize());
        config.set("items." + slot + ".amount", amount);
        
        try {
            config.save(storageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAllStorages() {
        for (StorageGUI gui : openStorages.values()) {
            gui.saveStorage();
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void saveStorageConfig(UUID uuid, FileConfiguration config) {
        try {
            config.save(getStorageFile(uuid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}