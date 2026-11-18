package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class LocomotiveScreen extends AbstractContainerScreen<LocomotiveMenu> {

    private static final ResourceLocation GUI_TEXTURE = SpelunkersCharm.id("textures/gui/locomotive/locomotive_gui.png");
    // Vanilla furnace flame sprite
    private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");

    public LocomotiveScreen(LocomotiveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int percent = this.menu.getFuelPercent();
        if (percent > 0) {
            int height = Mth.ceil(percent * 13 / 100.0F) + 1;
            int flameX = x + LocomotiveMenu.FUEL_SLOT_X + 1;
            int flameY = y + LocomotiveMenu.FUEL_SLOT_Y - 18;
            guiGraphics.blitSprite(LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - height, flameX, flameY + 14 - height, 14, height);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        Component status = Component.translatable(this.menu.isReversed()
                ? "gui.spelunkers_charm.locomotive.reversed"
                : "gui.spelunkers_charm.locomotive.forward");
        guiGraphics.drawString(this.font, status, (this.imageWidth - this.font.width(status)) / 2, 22, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
