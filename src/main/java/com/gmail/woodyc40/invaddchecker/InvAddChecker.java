package com.gmail.woodyc40.invaddchecker;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class InvAddChecker extends JavaPlugin {
    @Override
    public void onEnable() {
        this.getCommand("invaddchk").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You are not a player!");
            return true;
        }

        if (!sender.hasPermission("inv-add-checker.invaddchk")) {
            sender.sendMessage("You do not have permission to do this!");
            return true;
        }

        Player player = (Player) sender;

        Block target = player.getTargetBlock(null, 6);
        BlockState state = target.getState();
        if (!(state instanceof Chest)) {
            sender.sendMessage("You are not looking at a chest!");
            return true;
        }

        Chest chest = (Chest) state;
        Inventory inventory = chest.getInventory();

        PlayerInventory pInv = player.getInventory();
        Collection<ItemStack> itemsToAdd = new ArrayList<>();
        for (ItemStack item : pInv) {
            if (item != null) {
                itemsToAdd.add(item);
            }
        }
        addItemTo(inventory, itemsToAdd);

        player.sendMessage("Added your inventory to the chest!");

        return true;
    }

    private static Map<ItemStack, Integer> addItemTo(Inventory inv, Collection<ItemStack> itemsToAdd) {
        Map<ItemStack, Integer> quantities = new HashMap<>(itemsToAdd.size());
        for (ItemStack item : itemsToAdd) {
            if (item != null) {
                quantities.put(item, item.getAmount());
            }
        }

        // Pass 1: Do not include empty slots
        boolean includeEmpty = false;

        for (int i = 0; i < 2; i++) {
            for (int slot = 0; slot < inv.getSize(); slot++) {
                // Ensure there is anything left to add
                if (itemsToAdd.isEmpty()) {
                    return quantities;
                }

                ItemStack item = inv.getItem(slot);

                boolean modified = false;
                int itemAmt = 0;
                int maxStack = 0;

                if (item == null) {
                    if (includeEmpty) {
                        for (Iterator<ItemStack> it = itemsToAdd.iterator(); it.hasNext(); ) {
                            ItemStack add = it.next();
                            if (add == null) {
                                continue;
                            }

                            int addAmt = quantities.get(add);
                            maxStack = add.getMaxStackSize();

                            // Ensure max stack size
                            int delta = Math.min(maxStack, addAmt);

                            addAmt -= delta;

                            // No items left to add, remove it
                            // Otherwise, subtract and move on
                            if (addAmt == 0) {
                                it.remove();
                            } else {
                                quantities.put(add, addAmt);
                            }

                            modified = true;
                            itemAmt = delta;
                            item = add;

                            break;
                        }
                    }

                    if (item == null) {
                        continue;
                    }
                } else {
                    itemAmt = item.getAmount();
                    maxStack = item.getMaxStackSize();
                }

                // Ensure this slot can accept more items
                if (itemAmt < maxStack) {
                    for (Iterator<ItemStack> it = itemsToAdd.iterator(); it.hasNext(); ) {
                        ItemStack add = it.next();
                        if (add == null) {
                            continue;
                        }

                        if (item.isSimilar(add)) {
                            int maxDelta = maxStack - itemAmt;
                            int addAmt = quantities.get(add);

                            // Ensure max stack size
                            int delta = Math.min(maxDelta, addAmt);

                            addAmt -= delta;

                            // No items left to add, remove it
                            // Otherwise, subtract and move on
                            if (addAmt == 0) {
                                it.remove();
                            } else {
                                quantities.put(add, addAmt);
                            }

                            modified = true;
                            itemAmt += delta;

                            // If the amount of items added
                            // maxes the stack, go to the
                            // next slot in the inventory
                            if (delta == maxDelta) {
                                break;
                            }
                        }
                    }
                }

                // Update the item back once we've gone through
                // all the items that can be added to the slot
                if (modified) {
                    ItemStack clone = item.clone();
                    clone.setAmount(itemAmt);

                    inv.setItem(slot, clone);
                }
            }

            // Pass 2: Include empty slots
            includeEmpty = true;
        }

        return quantities;
    }
}
