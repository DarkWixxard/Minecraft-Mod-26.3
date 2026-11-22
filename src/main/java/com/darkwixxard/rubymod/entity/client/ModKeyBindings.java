package com.darkwixxard.rubymod.entity.client;

import com.darkwixxard.rubymod.RubyMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.settings.KeyConflictContext;

public final class ModKeyBindings {
    /** Own category "Ruby Mod" in the controls menu. */
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(RubyMod.MODID, "abilities"));

    /** Default key: V (changeable in Options -> Controls). */
    public static final KeyMapping MIST_STEP = new KeyMapping(
            "key.rubymod.mist_step",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_V,
            CATEGORY,
            0);

    private ModKeyBindings() {
    }
}
