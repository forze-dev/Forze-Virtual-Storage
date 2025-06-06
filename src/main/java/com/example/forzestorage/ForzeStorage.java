package com.forze.forzestorage;

import com.forze.forzestorage.commands.StorageCommand;
import com.forze.forzestorage.listeners.StorageListener;
import com.forze.forzestorage.managers.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ForzeStorage extends JavaPlugin {
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

        getLogger().info("ForzeStorage успішно увімкнено!");
    }

    @Override
    public void onDisable() {
        // Зберігаємо всі сховища при вимкненні
        if (storageManager != null) {
            storageManager.saveAllStorages();
        }
        getLogger().info("ForzeStorage успішно вимкнено!");
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }
}