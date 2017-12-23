package com.gmail.stefvanschiedev.buildinggame.utils.guis.moboptions.mobs.ocelot;

import com.gmail.stefvanschiedev.buildinggame.utils.guis.Gui;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Ocelot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Wool;

/**
 * A menu for changing the type of an ocelot
 *
 * @since 5.3.0
 */
class OcelotTypeMenu extends Gui {

    /**
     * {@inheritDoc}
     */
    OcelotTypeMenu(Ocelot ocelot) {
        super(null, 9, ChatColor.GREEN + "Select the ocelot type", 1);

        setStartingPoint(2);

        //wild ocelot
        ItemStack wildOcelot = new ItemStack(Material.RAW_FISH);
        ItemMeta wildOcelotMeta = wildOcelot.getItemMeta();
        wildOcelotMeta.setDisplayName(ChatColor.GREEN + "Wild ocelot");
        wildOcelot.setItemMeta(wildOcelotMeta);

        addItem(wildOcelot, event -> {
            ocelot.setCatType(Ocelot.Type.WILD_OCELOT);

            event.setCancelled(true);
        });

        //black cat
        ItemStack blackCat = new Wool(DyeColor.BLACK).toItemStack(1);
        ItemMeta blackCatMeta = blackCat.getItemMeta();
        blackCatMeta.setDisplayName(ChatColor.GREEN + "Black cat");
        blackCat.setItemMeta(blackCatMeta);

        addItem(blackCat, event -> {
            ocelot.setCatType(Ocelot.Type.BLACK_CAT);

            event.setCancelled(true);
        });

        setStartingPoint(5);

        //red cat
        ItemStack redCat = new Wool(DyeColor.RED).toItemStack(1);
        ItemMeta redCatMeta = redCat.getItemMeta();
        redCatMeta.setDisplayName(ChatColor.GREEN + "Red cat");
        redCat.setItemMeta(redCatMeta);

        addItem(redCat, event -> {
            ocelot.setCatType(Ocelot.Type.RED_CAT);

            event.setCancelled(true);
        });

        //siamese cat
        ItemStack siameseCat = new Wool(DyeColor.SILVER).toItemStack(1);
        ItemMeta siameseCatMeta = siameseCat.getItemMeta();
        siameseCatMeta.setDisplayName(ChatColor.GREEN + "Siamese cat");
        siameseCat.setItemMeta(siameseCatMeta);

        addItem(siameseCat, event -> {
            ocelot.setCatType(Ocelot.Type.SIAMESE_CAT);

            event.setCancelled(true);
        });
    }
}
