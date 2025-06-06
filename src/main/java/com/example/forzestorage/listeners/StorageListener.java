package com.forze.forzestorage.listeners;

import com.forze.forzestorage.ForzeStorage;
import com.forze.forzestorage.gui.StorageGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class StorageListener implements Listener {
    private final ForzeStorage plugin;

    public StorageListener(ForzeStorage plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        // Перевіряємо, чи це сховище
        if (event.getView().getTitle() == null || !event.getView().getTitle().contains("Сховище")) return;
        
        // Отримуємо GUI сховища
        StorageGUI gui = plugin.getStorageManager().getStorage(player, false);
        if (gui == null) return;

        // СПОЧАТКУ перевіряємо клік у нижньому інвентарі (інвентар гравця)
        if (event.getClickedInventory() != null && 
            event.getClickedInventory().getType() == InventoryType.PLAYER) {
            return; // Дозволяємо взаємодію з власним інвентарем
        }

        // Перевіряємо, чи це навігаційний слот (нижній ряд) ТІЛЬКИ у верхньому інвентарі
        if (event.getClickedInventory() != null && 
            event.getClickedInventory().getType() == InventoryType.CHEST &&
            event.getSlot() >= 45 && event.getSlot() <= 53) {
            
            event.setCancelled(true);
            
            // Обробка навігації
            if (event.getSlot() == 45) {
                gui.previousPage();
            } else if (event.getSlot() == 53) {
                gui.nextPage();
            }
            return;
        }

        // Якщо гравець не адмін, обмежуємо дії
        if (!gui.isAdmin()) {
            // Забороняємо shift+click для безпеки
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
                return;
            }
            
            // Дозволяємо тільки забирати предмети зі сховища
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                // Якщо у курсора вже є предмет, забороняємо дію
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    event.setCancelled(true);
                    return;
                }
                
                // Дозволяємо забрати предмет, але зберігаємо зміни після кліку
                Bukkit.getScheduler().runTask(plugin, () -> {
                    gui.saveStorage();
                });
                return;
            }
            
            // Забороняємо додавання нових предметів
            event.setCancelled(true);
        } else {
            // Для адмінів зберігаємо зміни після будь-якої дії
            Bukkit.getScheduler().runTask(plugin, () -> {
                gui.saveStorage();
            });
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        
        // Перевіряємо, чи це сховище
        if (event.getView().getTitle() == null || !event.getView().getTitle().contains("Сховище")) return;
        
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