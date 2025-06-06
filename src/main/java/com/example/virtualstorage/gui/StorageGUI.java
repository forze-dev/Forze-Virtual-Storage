package com.example.virtualstorage.gui;

import com.example.virtualstorage.VirtualStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StorageGUI {
    private final VirtualStorage plugin;
    private final Player viewer;
    private final OfflinePlayer owner;
    private final boolean isAdmin;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 45;
    private static final int[] NAVIGATION_SLOTS = {45, 46, 47, 48, 49, 50, 51, 52, 53};

    public StorageGUI(VirtualStorage plugin, Player viewer, boolean isAdmin) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.owner = viewer;
        this.isAdmin = isAdmin;
    }

    public StorageGUI(VirtualStorage plugin, Player viewer, OfflinePlayer owner, boolean isAdmin) {
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
            if (config.contains(path)) {
                ItemStack item = ItemStack.deserialize(config.getConfigurationSection(path + ".item").getValues(true));
                item.setAmount(config.getInt(path + ".amount", 1));
                inv.setItem(i, item);
            }
        }
    }

    private void setupNavigation(Inventory inv) {
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
        return config.contains("items." + ((currentPage + 1) * ITEMS_PER_PAGE));
    }

    private ItemStack createNavigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(meta);
        return item;
    }

    public void nextPage() {
        if (hasNextPage()) {
            currentPage++;
            open();
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            open();
        }
    }

    public void saveStorage() {
        if (!isAdmin) return;

        Inventory inv = viewer.getOpenInventory().getTopInventory();
        FileConfiguration config = plugin.getStorageManager().getStorageConfig(owner.getUniqueId());
        int startIndex = currentPage * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            ItemStack item = inv.getItem(i);
            String path = "items." + (startIndex + i);
            
            if (item != null && item.getType() != Material.AIR) {
                config.set(path + ".item", item.serialize());
                config.set(path + ".amount", item.getAmount());
            } else {
                config.set(path, null);
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
} 