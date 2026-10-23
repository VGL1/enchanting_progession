package com.vgl.disenchanting.registry;

import com.vgl.disenchanting.DisenChantingMod;
import com.vgl.disenchanting.skill.PlayerSkillData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, DisenChantingMod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerSkillData>> PLAYER_SKILL =
            ATTACHMENTS.register("player_skill", () -> AttachmentType
                    .<PlayerSkillData>builder(() -> new PlayerSkillData())  // явная лямбда-Supplier
                    .serialize(PlayerSkillData.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}