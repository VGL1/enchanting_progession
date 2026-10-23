package com.vgl.disenchanting.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.vgl.disenchanting.cast.CastManager;
import com.vgl.disenchanting.enchant.EnchantmentHelper;
import com.vgl.disenchanting.enchant.EnchantmentPool;
import com.vgl.disenchanting.registry.ModItems;
import com.vgl.disenchanting.skill.PlayerSkillData;
import com.vgl.disenchanting.skill.SkillManager;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ModCommands {

    private static final int CAST_TICKS = 60;
    private static final int OFFHAND_SLOT = -1;

    private ModCommands() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("disenchant")
                .requires(src -> src.hasPermission(0))

                .then(Commands.literal("apply")
                    .then(Commands.argument("enchant_id", StringArgumentType.string())
                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                var player = ctx.getSource().getPlayerOrException();
                                String idStr = StringArgumentType.getString(ctx, "enchant_id");
                                int level = IntegerArgumentType.getInteger(ctx, "level");

                                ResourceLocation id;
                                try {
                                    id = ResourceLocation.parse(idStr);
                                } catch (Exception ex) {
                                    ctx.getSource().sendFailure(Component.literal("Bad id: " + idStr));
                                    return 0;
                                }

                                boolean started = CastManager.startEnchant(player, id, level,
                                        CAST_TICKS, OFFHAND_SLOT, true);
                                if (started) {
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Casting..."), false);
                                }
                                return started ? 1 : 0;
                            })
                        )
                    )
                )

                .then(Commands.literal("pool")
                    .executes(ctx -> {
                        var src = ctx.getSource();
                        src.sendSuccess(() -> Component.literal(
                                "Pool size: " + EnchantmentPool.size()), false);
                        EnchantmentPool.all().forEach((id, e) ->
                                src.sendSuccess(() -> Component.literal(
                                        " - " + id + " max=" + e.maxLevel()
                                                + " rarity=" + e.rarity()
                                                + " cat=" + e.category()
                                                + " tier=" + e.materialTier()
                                                + " dust/lvl=" + e.dustPerLevel()
                                                + " shards=" + e.shardCost()), false));
                        return EnchantmentPool.size();
                    })
                )

                .then(Commands.literal("items")
                    .executes(ctx -> {
                        var player = ctx.getSource().getPlayerOrException();
                        int dC = EnchantmentHelper.countItem(player, ModItems.ENCHANT_DUST.get());
                        int sC = EnchantmentHelper.countItem(player, ModItems.ENCHANT_SHARD.get());
                        int dR = EnchantmentHelper.countItem(player, ModItems.SPECTRAL_DUST.get());
                        int sR = EnchantmentHelper.countItem(player, ModItems.SPIRIT_SHARD.get());
                        int dE = EnchantmentHelper.countItem(player, ModItems.VISION_DUST.get());
                        int sE = EnchantmentHelper.countItem(player, ModItems.RADIANT_SHARD.get());
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "Common: dust=" + dC + " shards=" + sC
                                        + " | Rare: dust=" + dR + " shards=" + sR
                                        + " | Epic: dust=" + dE + " shards=" + sE), false);
                        return 1;
                    })
                )

                .then(Commands.literal("dust")
                    .executes(ctx -> {
                        var player = ctx.getSource().getPlayerOrException();
                        boolean started = CastManager.startDisenchant(player, CAST_TICKS, OFFHAND_SLOT);
                        if (started) {
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Casting..."), false);
                        }
                        return started ? 1 : 0;
                    })
                )

                .then(Commands.literal("skilldev")
                    .requires(src -> src.hasPermission(2))

                    .then(Commands.literal("info")
                        .executes(ctx -> {
                            var player = ctx.getSource().getPlayerOrException();
                            var data = SkillManager.get(player);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Enchanting: level=" + data.getLevel()
                                            + " / " + PlayerSkillData.MAX_LEVEL), false);
                            return 1;
                        })
                    )

                    .then(Commands.literal("set")
                        .then(Commands.argument("level",
                                IntegerArgumentType.integer(1, PlayerSkillData.MAX_LEVEL))
                            .executes(ctx -> {
                                var player = ctx.getSource().getPlayerOrException();
                                int level = IntegerArgumentType.getInteger(ctx, "level");
                                SkillManager.setLevel(player, level);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "Enchanting level set to " + level), false);
                                return 1;
                            })
                        )
                    )

                    .then(Commands.literal("add")
                        .then(Commands.argument("delta",
                                IntegerArgumentType.integer(-PlayerSkillData.MAX_LEVEL,
                                        PlayerSkillData.MAX_LEVEL))
                            .executes(ctx -> {
                                var player = ctx.getSource().getPlayerOrException();
                                int delta = IntegerArgumentType.getInteger(ctx, "delta");
                                SkillManager.addLevels(player, delta);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "Enchanting level changed by " + (delta >= 0 ? "+" : "") + delta
                                                + " → " + SkillManager.getLevel(player)), false);
                                return 1;
                            })
                        )
                    )
                )
        );

        event.getDispatcher().register(
            Commands.literal("skill")
                .requires(src -> src.hasPermission(0))
                .then(Commands.literal("enchanting")
                    .executes(ctx -> {
                        var player = ctx.getSource().getPlayerOrException();
                        var data = SkillManager.get(player);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "Enchanting skill: level " + data.getLevel()
                                        + " / " + PlayerSkillData.MAX_LEVEL), false);
                        return 1;
                    })
                )
        );
    }
}