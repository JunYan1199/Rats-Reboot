package com.github.alexthe666.rats.server.items.upgrades;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.upgrades.BaseRatUpgradeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CreateWorkerRatUpgradeItem
extends BaseRatUpgradeItem {
    public CreateWorkerRatUpgradeItem(Item.Properties properties) {
        super(properties, 1, 3);
    }

    @Override
    public boolean shouldDepositItem(TamedRat rat, ItemStack stack) {
        try {
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0441\u0434\u0430\u0447\u0443 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u0430 \u0440\u0430\u0431\u043e\u0442\u044f\u0433\u043e\u0439 Create {} \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", stack, rat.getUUID(), exception);
            throw exception;
        }
    }
}

