package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;

/** Shows a furnace minecart's remaining fuel: a flame, a bar and the time left. */
public class FurnaceMinecartScreen extends AbstractContainerScreen<FurnaceMinecartMenu> {

    private static final ResourceLocation GUI_TEXTURE = SpelunkersCharm.id("textures/gui/furnace_minecart/furnace_minecart_gui.png");
    private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");

    private static final int BAR_X = 30;
    private static final int BAR_Y = 26;
    private static final int BAR_WIDTH = 136;
    private static final int BAR_HEIGHT = 8;

    public FurnaceMinecartScreen(FurnaceMinecartMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 60;
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

        int fuel = this.menu.getFuel();
        if (fuel > 0) {
            guiGraphics.blitSprite(LIT_PROGRESS_SPRITE, 14, 14, 0, 0, x + 10, y + BAR_Y - 3, 14, 14);
            int filled = Math.max(1, fuel * BAR_WIDTH / FurnaceMinecartMenu.MAX_FUEL);
            guiGraphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + filled, y + BAR_Y + BAR_HEIGHT, 0xFFE07A1F);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        int fuel = this.menu.getFuel();
        Component text = fuel > 0
                ? Component.translatable("gui.spelunkers_charm.furnace_minecart.fuel", StringUtil.formatTickDuration(fuel, 20.0F))
                : Component.translatable("gui.spelunkers_charm.furnace_minecart.empty");
        guiGraphics.drawString(this.font, text, (this.imageWidth - this.font.width(text)) / 2, 42, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
