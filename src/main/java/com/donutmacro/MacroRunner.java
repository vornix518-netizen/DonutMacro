package com.donutmacro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Comparator;
import java.util.List;

public class MacroRunner {
    public static boolean running = false;

    private enum Orders { NONE, OPENING, CLICK_ORDER, MOVE_ITEMS }

    private static Orders orders = Orders.NONE;
    private static int cooldown = 0;
    private static int waitTicks = 0;
    private static int movedTotal = 0;
    private static int ordersFailures = 0;
    /** Ticks during which /orders is skipped after a failure, so selling keeps going. */
    private static int ordersSkip = 0;

    public static void start(MinecraftClient mc) {
        running = true;
        cooldown = 0;
        movedTotal = 0;
        ordersFailures = 0;
        ordersSkip = 0;
        orders = Orders.NONE;
        msg(mc, "Macro started.");
    }

    public static void stop(MinecraftClient mc) {
        running = false;
        orders = Orders.NONE;
        if (mc != null) {
            mc.options.forwardKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            msg(mc, "Macro stopped.");
        }
    }

    private static void msg(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[DonutMacro] " + s), true);
    }

    public static void tick(MinecraftClient mc) {
        if (!running) return;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) { running = false; return; }
        if (mc.currentScreen instanceof MacroScreen) return;

        MacroConfig cfg = MacroConfig.INSTANCE;
        Item target = cfg.targetItem();
        if (target == null) { msg(mc, "Unknown item id in settings."); stop(mc); return; }

        if (ordersSkip > 0) ordersSkip--;
        if (cooldown > 0) { cooldown--; return; }

        // ---- Orders flow (runs inside the /orders container screen) ----
        if (orders != Orders.NONE) { handleOrders(mc, p, target, cfg); return; }

        // Don't fire commands or clicks while some other screen (chat, a menu) is open.
        if (mc.currentScreen != null) return;

        int invSlot = findInInventory(p, target);

        if (invSlot >= 0) {
            mc.options.forwardKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);

            // ---- 1. Fill orders first (up to the cap), if enabled ----
            if (cfg.ordersEnabled && movedTotal < cfg.maxOrderAmount && ordersSkip == 0) {
                p.networkHandler.sendChatCommand("orders");
                orders = Orders.OPENING;
                waitTicks = 0;
                cooldown = 10;
                return;
            }

            // ---- 2. Sell whatever is left on the auction house ----
            int selected = p.getInventory().selectedSlot;
            if (invSlot < 9) {
                p.getInventory().selectedSlot = invSlot;
            } else {
                // main inventory index 9-35 equals the menu slot id; SWAP with the selected hotbar slot
                mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, invSlot, selected, SlotActionType.SWAP, p);
            }
            p.networkHandler.sendChatCommand("ah sell " + cfg.price.trim());
            cooldown = Math.max(10, cfg.delayTicks);
            return;
        }

        // ---- 3. Nothing in inventory: walk to / pick up items on the ground ----
        if (cfg.pickupGround) {
            List<ItemEntity> items = mc.world.getEntitiesByClass(ItemEntity.class,
                    p.getBoundingBox().expand(16), e -> e.getStack().isOf(target));
            ItemEntity nearest = items.stream().min(Comparator.comparingDouble(p::squaredDistanceTo)).orElse(null);
            if (nearest != null) {
                double dx = nearest.getX() - p.getX();
                double dz = nearest.getZ() - p.getZ();
                p.setYaw((float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f);
                mc.options.forwardKey.setPressed(true);
                mc.options.jumpKey.setPressed(p.horizontalCollision);
                return;
            }
        }
        mc.options.forwardKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
    }

    private static void handleOrders(MinecraftClient mc, ClientPlayerEntity p, Item target, MacroConfig cfg) {
        if (!(mc.currentScreen instanceof GenericContainerScreen gcs)) {
            if (++waitTicks > 60) abortOrders(mc, "Orders menu never opened.");
            return;
        }
        GenericContainerScreenHandler h = gcs.getScreenHandler();
        int containerSlots = h.slots.size() - 36;

        switch (orders) {
            case OPENING -> { orders = Orders.CLICK_ORDER; cooldown = 10; }
            case CLICK_ORDER -> {
                Slot found = null;
                for (int i = 0; i < containerSlots; i++) {
                    if (h.slots.get(i).getStack().isOf(target)) { found = h.slots.get(i); break; }
                }
                if (found == null) { abortOrders(mc, "No order for that item found."); return; }
                mc.interactionManager.clickSlot(h.syncId, found.id, 0, SlotActionType.PICKUP, p);
                orders = Orders.MOVE_ITEMS;
                cooldown = 10;
            }
            case MOVE_ITEMS -> {
                // Shift-click matching stacks from our inventory until we reach the cap (whole stacks).
                // The menu that opens after clicking an order may differ in size, so re-read it.
                GenericContainerScreenHandler cur = h;
                int curContainer = cur.slots.size() - 36;
                for (int i = curContainer; i < cur.slots.size(); i++) {
                    ItemStack s = cur.slots.get(i).getStack();
                    if (!s.isOf(target)) continue;
                    if (movedTotal >= cfg.maxOrderAmount) break;
                    int count = s.getCount();
                    mc.interactionManager.clickSlot(cur.syncId, i, 0, SlotActionType.QUICK_MOVE, p);
                    movedTotal += count;
                }
                ordersFailures = 0;
                p.closeHandledScreen();
                orders = Orders.NONE;
                cooldown = Math.max(20, cfg.delayTicks);
            }
            default -> {}
        }
    }

    private static void abortOrders(MinecraftClient mc, String why) {
        if (mc.player != null && mc.currentScreen instanceof GenericContainerScreen) mc.player.closeHandledScreen();
        orders = Orders.NONE;
        cooldown = 20;
        ordersSkip = 600; // skip /orders for 30s so selling continues instead of spamming the command
        if (++ordersFailures >= 3) { ordersFailures = 0; msg(mc, why + " Retrying orders in 30s."); }
    }

    private static int findInInventory(ClientPlayerEntity p, Item target) {
        for (int i = 0; i < 36; i++) {
            if (p.getInventory().getStack(i).isOf(target)) return i;
        }
        return -1;
    }
}
