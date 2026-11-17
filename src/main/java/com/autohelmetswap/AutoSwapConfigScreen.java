package com.autohelmetswap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Simple Mod Menu screen with a numeric delay field. */
public final class AutoSwapConfigScreen extends Screen {
	private final Screen parent;
	private EditBox delayField;

	public AutoSwapConfigScreen(Screen parent) {
		super(Component.translatable("autohelmetswap.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int center = this.width / 2;
		this.delayField = new EditBox(this.font, center - 100, this.height / 2 - 8, 200, 20,
				Component.translatable("autohelmetswap.config.delay"));
		this.delayField.setMaxLength(5);
		this.delayField.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
		this.delayField.setValue(String.valueOf(SwapConfig.getDelayMillis()));
		this.addRenderableWidget(this.delayField);

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.saveAndClose())
				.bounds(center - 100, this.height / 2 + 24, 200, 20).build());
	}

	private void saveAndClose() {
		String value = this.delayField.getValue().strip();
		if (!value.isEmpty()) {
			try {
				int parsed = Integer.parseInt(value);
				if (parsed >= 0 && parsed <= 5000) {
					SwapConfig.setDelayMillis(parsed);
				} else {
					this.delayField.setValue("100");
					return;
				}
			} catch (NumberFormatException exception) {
				this.delayField.setValue("100");
				return;
			}
		}
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, Component.translatable("autohelmetswap.config.delay"),
				this.width / 2, this.height / 2 - 30, 0xFFFFFF);
		graphics.drawCenteredString(this.font, Component.literal("0–5000 ms"),
				this.width / 2, this.height / 2 + 50, 0xA0A0A0);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
