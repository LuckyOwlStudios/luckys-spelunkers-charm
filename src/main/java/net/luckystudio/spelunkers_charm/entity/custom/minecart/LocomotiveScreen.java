// Locomotive screen.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class LocomotiveScreen extends AbstractContainerScreen<LocomotiveMenu> {
    private static final ResourceLocation TEXTURE =
            SpelunkersCharm.id("textures/gui/locomotive/locomotive_gui.png");
    private final Control[] burners = new Control[3];
    private final Control[] modes = new Control[3];

    public LocomotiveScreen(LocomotiveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        for (int slot = 0; slot < 3; slot++) {
            burners[slot] =
                    addRenderableWidget(
                            new Control(leftPos + 58 + slot * 25, topPos + 17, 10, 12, slot, -1));
        }
        int[] ids = {
            LocomotiveMenu.BACKWARD_BUTTON,
            LocomotiveMenu.STOP_BUTTON,
            LocomotiveMenu.FORWARD_BUTTON
        };
        String[] names = {"backward", "stop", "forward"};
        for (int mode = 0; mode < 3; mode++) {
            modes[mode] =
                    addRenderableWidget(
                            new Control(
                                    leftPos + 42 + mode * 31,
                                    topPos + 60,
                                    29,
                                    13,
                                    ids[mode],
                                    mode));
            modes[mode].setMessage(
                    Component.translatable("gui.spelunkers_charm.locomotive.mode_" + names[mode]));
            modes[mode].setTooltip(Tooltip.create(modes[mode].getMessage()));
        }
        updateBurners();
    }

    private void updateBurners() {
        for (int slot = 0; slot < 3; slot++) {
            Component message =
                    Component.translatable(
                            menu.burnerEnabled(slot)
                                    ? "gui.spelunkers_charm.locomotive.disable_burner"
                                    : "gui.spelunkers_charm.locomotive.enable_burner",
                            slot + 1);
            burners[slot].setMessage(message);
            burners[slot].setTooltip(Tooltip.create(message));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateBurners();
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 200, 200);
        for (int slot = 0; slot < 3; slot++) {
            if (!menu.burnerEnabled(slot) || menu.emergencyBrake()) continue;
            int percent = menu.getFuelPercent(slot);
            if (percent > 0) {
                int height = Mth.clamp(Mth.ceil(percent * 7 / 100F), 1, 7);
                gui.blit(
                        TEXTURE,
                        leftPos + 60 + slot * 25,
                        topPos + 27 - height,
                        179,
                        23 - height,
                        6,
                        height,
                        200,
                        200);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        String name = font.plainSubstrByWidth(title.getString(), 92);
        gui.drawString(font, name, (imageWidth - font.width(name)) / 2, 5, 0xDDDDDD, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }

    /** Draws and handles a locomotive mode or firebox button. */
    private final class Control extends AbstractButton {
        private final int id;
        private final int mode;

        private Control(int x, int y, int width, int height, int id, int mode) {
            super(x, y, width, height, Component.empty());
            this.id = id;
            this.mode = mode;
        }

        @Override
        public void onPress() {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }

        @Override
        protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            if (mode < 0) {
                if (!menu.burnerEnabled(id))
                    gui.blit(TEXTURE, getX(), getY(), 177, 0, 10, 12, 200, 200);
            } else {
                int selected = menu.emergencyBrake() ? 1 : menu.isReversed() ? 0 : 2;
                if (selected == mode)
                    gui.blit(TEXTURE, getX(), getY(), mode * 30, 167, 29, 13, 200, 200);
            }
            if (isHoveredOrFocused()) gui.renderOutline(getX(), getY(), width, height, 0x99FFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }
}
