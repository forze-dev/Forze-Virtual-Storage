package com.example.virtualstorage.listeners;

import com.example.virtualstorage.VirtualStorage;
import com.example.virtualstorage.gui.StorageGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class StorageListener implements Listener {
    private final VirtualStorage plugin;

    public StorageListener(VirtualStorage plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        // Перевіряємо, чи це сховище
        if (!event.getView().getTitle().contains("Сховище")) return;
        
        // Отримуємо GUI сховища
        StorageGUI gui = plugin.getStorageManager().getStorage(player, false);
        if (gui == null) return;

        // Перевіряємо, чи це навігаційний слот
        if (event.getSlot() >= 45 && event.getSlot() <= 53) {
            event.setCancelled(true);
            
            // Обробка навігації
            if (event.getSlot() == 45) {
                gui.previousPage();
            } else if (event.getSlot() == 53) {
                gui.nextPage();
            }
            return;
        }

        // Якщо гравець не адмін, дозволяємо тільки забирати предмети
        if (!gui.isAdmin()) {
            if (event.getClick().isShiftClick() || event.getClick().isRightClick()) {
                event.setCancelled(true);
                return;
            }
            
            // Дозволяємо тільки забирати предмети
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                // Зберігаємо зміни в сховищі після кліку
                Bukkit.getScheduler().runTask(plugin, () -> {
                    gui.saveStorage();
                });
                return;
            }
            
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        
        // Перевіряємо, чи це сховище
        if (!event.getView().getTitle().contains("Сховище")) return;
        
        // Отримуємо GUI сховища
        StorageGUI gui = plugin.getStorageManager().getStorage(player, false);
        if (gui == null) return;

        // Зберігаємо зміни
        gui.saveStorage();
        
        // Закриваємо сховище
        plugin.getStorageManager().closeStorage(player);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("messages.storage-closed", "&aСховище закрито!")));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        StorageGUI gui = plugin.getStorageManager().getStorage(player, false);
        if (gui != null) {
            gui.saveStorage();
            plugin.getStorageManager().closeStorage(player);
        }
    }
} 