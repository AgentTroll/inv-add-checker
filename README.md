# `inv-add-checker`

This is a demo plugin that shows an implementation of adding
items to an inventory that mimics Vanilla's implementation
as closely as possible.

# Implementation

The implementation attempts to achieve three things:
firstly, it must fill items that are not fully stacked
before filling in the empty slots; secondly, to avoid
modifying the items passed through the `itemsToAdd`
collection; and finally, avoid cloning where unnecessary.
In order to reduce the number of times that an item can
possibly be cloned, a map containing the quantities of each
item is constructed and tracked as each item is placed into
the inventory. This map is then returned in place of the
`Map<Integer, ItemStack>` that is returned by
`Inventory#addItem(ItemStack...)` available in the Bukkit
API. No input items are ever modified or cloned, unless they
are being placed into an empty slot in an inventory (in
which case a clone is necessary regardless).


``` java
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
```

# Building

``` shell
git clone https://github.com/caojohnny/inv-add-checker.git
cd inv-add-checker
mvn clean install
```

The jar can be found at `target/InvAddChecker.jar`.

# Demo Usage

Start a server with the jar file added to the plugin folder.
Join and look at a chest block. Then, run /invaddchk to add
your entire inventory to the chest.

# Credits

Built with [IntelliJ IDEA](https://www.jetbrains.com/idea/)
