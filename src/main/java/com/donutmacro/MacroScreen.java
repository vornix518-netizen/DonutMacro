package com.donutmacro;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class MacroScreen extends Screen {
    private TextFieldWidget itemField, priceField, maxField;
    private ButtonWidget ordersButton, pickupButton, startButton;

    public MacroScreen() {
        super(Text.literal("Donut Macro"));
    }

    @Override
    protected void init() {
        MacroConfig c = MacroConfig.INSTANCE;
        int cx = width / 2, y = height / 2 - 90, w = 200;

        itemField = new TextFieldWidget(textRenderer, cx - w / 2, y + 12, w, 20, Text.literal("Item"));
        itemField.setMaxLength(100);
        itemField.setText(c.item);
        addDrawableChild(itemField);

        priceField = new TextFieldWidget(textRenderer, cx - w / 2, y + 52, w, 20, Text.literal("Price"));
        priceField.setMaxLength(20);
        priceField.setText(c.price);
        addDrawableChild(priceField);

        ordersButton = ButtonWidget.builder(ordersLabel(), b -> {
            c.ordersEnabled = !c.ordersEnabled;
            b.setMessage(ordersLabel());
        }).dimensions(cx - w / 2, y + 82, w, 20).build();
        addDrawableChild(ordersButton);

        maxField = new TextFieldWidget(textRenderer, cx - w / 2, y + 122, w, 20, Text.literal("Max"));
        maxField.setMaxLength(6);
        maxField.setText(String.valueOf(c.maxOrderAmount));
        addDrawableChild(maxField);

        pickupButton = ButtonWidget.builder(pickupLabel(), b -> {
            c.pickupGround = !c.pickupGround;
            b.setMessage(pickupLabel());
        }).dimensions(cx - w / 2, y + 148, w, 20).build();
        addDrawableChild(pickupButton);

        startButton = ButtonWidget.builder(startLabel(), b -> {
            apply();
            if (MacroRunner.running) MacroRunner.stop(client); else MacroRunner.start(client);
            close();
        }).dimensions(cx - w / 2, y + 176, w, 20).build();
        addDrawableChild(startButton);
    }

    private Text ordersLabel() {
        return Text.literal("Fill orders: " + (MacroConfig.INSTANCE.ordersEnabled ? "ON" : "OFF"));
    }

    private Text pickupLabel() {
        return Text.literal("Pick up ground items: " + (MacroConfig.INSTANCE.pickupGround ? "ON" : "OFF"));
    }

    private Text startLabel() {
        return Text.literal(MacroRunner.running ? "Stop macro" : "Save & start macro");
    }

    private void apply() {
        MacroConfig c = MacroConfig.INSTANCE;
        c.item = itemField.getText().trim();
        c.price = priceField.getText().trim();
        try { c.maxOrderAmount = Math.max(0, Integer.parseInt(maxField.getText().trim())); }
        catch (NumberFormatException ignored) {}
        MacroConfig.save();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2, y = height / 2 - 90;
        ctx.drawCenteredTextWithShadow(textRenderer, "Donut Macro", cx, y - 14, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "1) Item to sell (e.g. minecraft:diamond)", cx - 100, y + 2, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "2) Auction house price (e.g. 1000, 1k, 2.5m)", cx - 100, y + 42, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "3) Max items to fill on orders", cx - 100, y + 112, 0xAAAAAA);
    }

    @Override
    public void close() {
        apply();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
