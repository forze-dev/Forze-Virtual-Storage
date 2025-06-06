package com.example.virtualstorage;

import com.example.virtualstorage.commands.StorageCommand;
import com.example.virtualstorage.listeners.StorageListener;
import com.example.virtualstorage.managers.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VirtualStorage extends JavaPlugin {
    private StorageManager storageManager;

    @Override
    public void onEnable() {
        // Зберігаємо конфігурацію
        saveDefaultConfig();
        saveResource("messages.yml", false);

        // Ініціалізуємо менеджер сховищ
        storageManager = new StorageManager(this);

        // Реєструємо команди
        getCommand("storage").setExecutor(new StorageCommand(this));

        // Реєструємо слухачів подій
        getServer().getPluginManager().registerEvents(new StorageListener(this), this);

        getLogger().info("VirtualStorage успішно увімкнено!");
    }

    @Override
    public void onDisable() {
        // Зберігаємо всі сховища при вимкненні
        if (storageManager != null) {
            storageManager.saveAllStorages();
        }
        getLogger().info("VirtualStorage успішно вимкнено!");
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }
} 