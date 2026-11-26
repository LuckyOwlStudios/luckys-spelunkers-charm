// Cart loader screen.
package net.luckystudio.spelunkers_charm.block.custom.cartloader;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CartLoaderScreen extends AbstractContainerScreen<CartLoaderMenu> {
    private static final ResourceLocation TEXTURE =
            SpelunkersCharm.id("textures/gui/cart_loader.png");

    public CartLoaderScreen(CartLoaderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 133;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
