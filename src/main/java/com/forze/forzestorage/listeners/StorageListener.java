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
        
        // ВИПРАВЛЕННЯ 1: Отримуємо правильний GUI сховища
        StorageGUI gui = null;
        try {
            // Спробуємо знайти GUI для цього гравця
            gui = plugin.getStorageManager().getStorage(player, player.hasPermission("storage.admin"));
        } catch (Exception e) {
            plugin.getLogger().warning("Помилка при отриманні GUI сховища для " + player.getName());
            return;
        }
        
        if (gui == null) return;

        // Дозволяємо взаємодію з власним інвентарем гравця (нижній інвентар)
        if (event.getClickedInventory() != null && 
            event.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        // ВИПРАВЛЕННЯ 2: Перевіряємо навігаційні кнопки у верхньому інвентарі
        if (event.getClickedInventory() != null && 
            event.getClickedInventory().getType() == InventoryType.CHEST &&
            event.getSlot() >= 45 && event.getSlot() <= 53) {
            
            event.setCancelled(true);
            
            // Обробка навігації
            if (event.getSlot() == 45) {
                // Кнопка "Попередня сторінка"
                gui.previousPage();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.previous-page", "&aПопередня сторінка")));
            } else if (event.getSlot() == 53) {
                // Кнопка "Наступна сторінка"
                gui.nextPage();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.next-page", "&aНаступна сторінка")));
            }
            return;
        }

        // ВИПРАВЛЕННЯ 3: Правильна логіка для звичайних гравців
        if (!gui.isAdmin()) {
            // Забороняємо shift+click для безпеки
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Shift+Click заборонено у сховищі!");
                return;
            }
            
            // Перевіряємо, чи гравець намагається покласти щось у сховище
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                // Гравець намагається покласти предмет у порожній слот
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Ви можете тільки забирати предмети зі сховища!");
                    return;
                }
            } else {
                // У слоті є предмет - гравець може його забрати
                // Перевіряємо, чи у курсора немає предмета (інакше це спроба обміну)
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Ви не можете міняти предмети у сховищі!");
                    return;
                }
                
                // Дозволяємо забрати предмет і зберігаємо зміни
                final StorageGUI finalGui = gui; // Робимо змінну final для lambda
                Bukkit.getScheduler().runTask(plugin, () -> {
                    finalGui.saveStorage();
                });
                return;
            }
            
            // Всі інші дії забороняємо
            event.setCancelled(true);
        } else {
            // Для адмінів дозволяємо все і зберігаємо зміни
            final StorageGUI finalGui = gui; // Робимо змінну final для lambda
            Bukkit.getScheduler().runTask(plugin, () -> {
                finalGui.saveStorage();
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
        StorageGUI gui = plugin.getStorageManager().getStorage(player, player.hasPermission("storage.admin"));
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
        try {
            StorageGUI gui = plugin.getStorageManager().getStorage(player, player.hasPermission("storage.admin"));
            if (gui != null) {
                gui.saveStorage();
                plugin.getStorageManager().closeStorage(player);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Помилка при збереженні сховища гравця " + player.getName() + " при виході");
        }
    }
}