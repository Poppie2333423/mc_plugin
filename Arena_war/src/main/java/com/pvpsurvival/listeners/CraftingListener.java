package com.pvpsurvival.listeners;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CraftingListener implements Listener {
    
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        
        // Prüfe ob es eine Spitzhacke ist
        if (isPickaxe(result.getType())) {
            ItemStack enchantedPickaxe = result.clone();
            ItemMeta meta = enchantedPickaxe.getItemMeta();
            
            if (meta != null) {
                // Effizienz 1 für alle Spitzhacken
                meta.addEnchant(Enchantment.EFFICIENCY, 1, true);
                
                // Glück basierend auf Material
                if (result.getType() == Material.DIAMOND_PICKAXE) {
                    meta.addEnchant(Enchantment.FORTUNE, 2, true); // Diamant = Glück 2
                } else {
                    meta.addEnchant(Enchantment.FORTUNE, 1, true); // Alle anderen = Glück 1
                }
                
                enchantedPickaxe.setItemMeta(meta);
            }
            
            // Ersetze das Ergebnis mit der verzauberten Version
            event.setCurrentItem(enchantedPickaxe);
        }
    }
    
    private boolean isPickaxe(Material material) {
        return material == Material.WOODEN_PICKAXE ||
               material == Material.STONE_PICKAXE ||
               material == Material.IRON_PICKAXE ||
               material == Material.GOLDEN_PICKAXE ||
               material == Material.DIAMOND_PICKAXE ||
               material == Material.NETHERITE_PICKAXE;
    }
}