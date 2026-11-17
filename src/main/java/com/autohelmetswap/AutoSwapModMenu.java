package com.autohelmetswap;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Registers Auto Swap's settings screen with Mod Menu. */
public final class AutoSwapModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return AutoSwapConfigScreen::new;
	}
}
