package com.autohelmetswap;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

/**
 * Auto Helmet Swap (client-only).
 *
 * <p>Three independent keybinds, each toggling the helmet slot between two assigned items.
 * The swap is performed with ordinary inventory clicks (pick up / place / put back), so the
 * whole ItemStack - enchantments, custom name, lore and every other data component - is moved
 * untouched, and it works on servers because the server sees normal inventory clicks.
 */
public class AutoHelmetSwapClient implements ClientModInitializer {
	public static final String MOD_ID = "autohelmetswap";

	/** Slot index of the helmet slot in the player's inventory menu. */
	private static final int HEAD_SLOT = 5;
	/** First slot of the main inventory in the player's inventory menu (9-35 main, 36-44 hotbar, 45 offhand). */
	private static final int FIRST_STORAGE_SLOT = 9;

	/** An item the mod can recognise. */
	private enum Target {
		SUPERMAN("autohelmetswap.name.superman", "Сила Супермена"),
		JACK("autohelmetswap.name.jack", "Голова Джека"),
		NETHERITE("autohelmetswap.name.netherite", null);

		private final String translationKey;
		private final String customName;

		Target(String translationKey, String customName) {
			this.translationKey = translationKey;
			this.customName = customName;
		}

		Component displayName() {
			return Component.translatable(this.translationKey);
		}

		boolean matches(ItemStack stack) {
			if (stack.isEmpty()) {
				return false;
			}

			if (this.customName != null) {
				return hasCustomName(stack, this.customName);
			}

			// Netherite Helmet: identified by item type. A helmet that carries one of the two
			// head names is treated as that head, so every keybind stays unambiguous.
			return stack.is(Items.NETHERITE_HELMET)
					&& !hasCustomName(stack, SUPERMAN.customName)
					&& !hasCustomName(stack, JACK.customName);
		}

		private static boolean hasCustomName(ItemStack stack, String expected) {
			Component name = stack.get(DataComponents.CUSTOM_NAME);
			return name != null && name.getString().strip().equals(expected);
		}
	}

	private record SwapPair(KeyMapping key, Target a, Target b) {
	}

	/** One swap in progress; performed one inventory click per client tick. */
	private static final class Job {
		final int sourceSlot;
		final Target want;
		int step;

		Job(int sourceSlot, Target want) {
			this.sourceSlot = sourceSlot;
			this.want = want;
		}
	}

	private List<SwapPair> pairs;
	private Job job;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "main"));

		this.pairs = List.of(
				new SwapPair(registerKey("superman_jack", GLFW.GLFW_KEY_Z, category), Target.SUPERMAN, Target.JACK),
				new SwapPair(registerKey("netherite_superman", GLFW.GLFW_KEY_V, category), Target.NETHERITE, Target.SUPERMAN),
				new SwapPair(registerKey("netherite_jack", GLFW.GLFW_KEY_B, category), Target.NETHERITE, Target.JACK)
		);

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private static KeyMapping registerKey(String name, int defaultKey, KeyMapping.Category category) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key." + MOD_ID + "." + name,
				InputConstants.Type.KEYSYM,
				defaultKey,
				category
		));
	}

	private void onClientTick(Minecraft mc) {
		LocalPlayer player = mc.player;

		// Always drain the key queues so presses never pile up.
		SwapPair pressed = null;
		for (SwapPair pair : this.pairs) {
			while (pair.key().consumeClick()) {
				if (pressed == null) {
					pressed = pair;
				}
			}
		}

		if (player == null || mc.gameMode == null) {
			this.job = null;
			return;
		}

		if (this.job != null) {
			this.runStep(mc, player);
			return;
		}

		if (pressed != null && mc.screen == null) {
			this.startJob(player, pressed);
		}
	}

	private void startJob(LocalPlayer player, SwapPair pair) {
		AbstractContainerMenu menu = player.inventoryMenu;
		if (player.containerMenu != menu || !menu.getCarried().isEmpty()) {
			return;
		}

		ItemStack head = menu.getSlot(HEAD_SLOT).getItem();

		List<Target> wanted;
		if (pair.a().matches(head)) {
			wanted = List.of(pair.b());
		} else if (pair.b().matches(head)) {
			wanted = List.of(pair.a());
		} else {
			// Helmet slot holds neither item: equip whichever of the two is available.
			wanted = List.of(pair.a(), pair.b());
		}

		for (Target target : wanted) {
			int slot = findSlot(menu, target);
			if (slot >= 0) {
				this.job = new Job(slot, target);
				return;
			}
		}

		Component names = wanted.size() == 1
				? wanted.get(0).displayName()
				: Component.empty().append(pair.a().displayName()).append(" / ").append(pair.b().displayName());
		player.sendOverlayMessage(Component.translatable("autohelmetswap.msg.not_found", names));
	}

	private static int findSlot(AbstractContainerMenu menu, Target target) {
		for (int i = FIRST_STORAGE_SLOT; i < menu.slots.size(); i++) {
			if (target.matches(menu.getSlot(i).getItem())) {
				return i;
			}
		}
		return -1;
	}

	private void runStep(Minecraft mc, LocalPlayer player) {
		AbstractContainerMenu menu = player.inventoryMenu;
		Job current = this.job;

		// A screen opened or the open container changed: stop, vanilla returns any carried item on close.
		if (player.containerMenu != menu || mc.screen != null) {
			this.job = null;
			return;
		}

		switch (current.step) {
			case 0 -> {
				// Pick up the wanted item.
				if (!current.want.matches(menu.getSlot(current.sourceSlot).getItem())) {
					this.job = null;
					return;
				}
				click(mc, player, menu, current.sourceSlot);
			}
			case 1 -> {
				// Put it into the helmet slot; the old helmet (if any) ends up on the cursor.
				if (menu.getCarried().isEmpty()) {
					this.job = null;
					return;
				}
				click(mc, player, menu, HEAD_SLOT);
			}
			case 2 -> {
				// Put the old helmet into the slot the new one came from.
				if (!menu.getCarried().isEmpty()) {
					click(mc, player, menu, current.sourceSlot);
				}
			}
			default -> {
				if (!current.want.matches(menu.getSlot(HEAD_SLOT).getItem())) {
					player.sendOverlayMessage(Component.translatable(
							"autohelmetswap.msg.failed", current.want.displayName()));
				}
				this.job = null;
				return;
			}
		}

		current.step++;
	}

	private static void click(Minecraft mc, LocalPlayer player, AbstractContainerMenu menu, int slot) {
		mc.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.PICKUP, player);
	}
}
