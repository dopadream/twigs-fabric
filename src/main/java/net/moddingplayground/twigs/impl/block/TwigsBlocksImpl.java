package net.moddingplayground.twigs.impl.block;

import com.google.common.collect.Maps;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.fabricmc.fabric.api.registry.TillableBlockRegistry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.moddingplayground.frame.api.loottables.v0.LootTableAdditions;
import net.moddingplayground.twigs.api.Twigs;
import net.moddingplayground.twigs.api.block.StrippedBambooBlock;
import net.moddingplayground.twigs.api.block.TwigsBlocks;
import net.moddingplayground.twigs.api.sound.TwigsSoundEvents;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public class TwigsBlocksImpl implements TwigsBlocks {
    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            ItemStack stack = player.getStackInHand(hand);
            BlockPos pos = hit.getBlockPos();
            BlockState state = world.getBlockState(pos);

            Optional<BlockState> nu = Optional.empty();

            if (state.isOf(Blocks.FLOWERING_AZALEA) && stack.getItem() instanceof ShearsItem) {
                Block.dropStack(world, pos.up(), new ItemStack(AZALEA_FLOWERS, world.random.nextInt(2) + 1));
                world.playSound(player, pos, TwigsSoundEvents.BLOCK_FLOWERING_AZALEA_SHEAR, SoundCategory.BLOCKS, 1.0f, 1.0f);
                nu = Optional.of(Blocks.AZALEA.getDefaultState());
            } if (state.isOf(Blocks.BAMBOO) && stack.getItem() instanceof AxeItem) {
                if (!world.getBlockState(pos.up()).isOf(Blocks.BAMBOO)) {
                    int leaves = state.get(Properties.BAMBOO_LEAVES).ordinal();
                    if (leaves > 0) {
                        int drop = world.random.nextInt(leaves * (leaves + 1));
                        if (drop > 0) {
                            Block.dropStack(world, pos, new ItemStack(BAMBOO_LEAVES, drop));
                            world.playSound(player, pos, TwigsSoundEvents.BLOCK_BAMBOO_STRIP_SHEAR, SoundCategory.BLOCKS, 1.0f, 1.0f);
                        }
                    }

                    world.playSound(player, pos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    nu = Optional.of(STRIPPED_BAMBOO.getDefaultState().with(StrippedBambooBlock.FROM_BAMBOO, true));
                }
            }

            if (nu.isPresent()) {
                if (player instanceof ServerPlayerEntity serverPlayer) Criteria.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
                world.setBlockState(pos, nu.get(), Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
                stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
                return ActionResult.success(world.isClient);
            }

            return ActionResult.PASS;
        });

        LootTableAdditions.of(Blocks.BAMBOO).defaulted(Twigs.MOD_ID).register();

        FlammableBlockRegistry flamReg = FlammableBlockRegistry.getDefaultInstance();
        flamReg.add(AZALEA_FLOWERS,30, 60);
        flamReg.add(TWIG,30, 60);
        flamReg.add(BAMBOO_LEAVES,30, 60);
        flamReg.add(BAMBOO_THATCH,30, 60);
        flamReg.add(BAMBOO_THATCH_SLAB, 30, 60);
        flamReg.add(BAMBOO_THATCH_STAIRS, 30, 60);
        flamReg.add(STRIPPED_BAMBOO, 5, 20);

        FuelRegistry fuelReg = FuelRegistry.INSTANCE;
        fuelReg.add(STRIPPED_BAMBOO, 50);
        fuelReg.add(BUNDLED_BAMBOO, 450);
        fuelReg.add(STRIPPED_BUNDLED_BAMBOO, 450);

        CompostingChanceRegistry compReg = CompostingChanceRegistry.INSTANCE;
        compReg.add(BAMBOO_LEAVES, 0.5F);

        TillableBlockRegistry.register(ROCKY_DIRT, ctx -> true, Blocks.COARSE_DIRT.getDefaultState(), PEBBLE);
        StrippableBlockRegistry.register(BUNDLED_BAMBOO, STRIPPED_BUNDLED_BAMBOO);

        LinkedHashMap<Block, Block> copperPillars = Maps.newLinkedHashMap();
        copperPillars.put(COPPER_PILLAR, WAXED_COPPER_PILLAR);
        copperPillars.put(EXPOSED_COPPER_PILLAR, WAXED_EXPOSED_COPPER_PILLAR);
        copperPillars.put(WEATHERED_COPPER_PILLAR, WAXED_WEATHERED_COPPER_PILLAR);
        copperPillars.put(OXIDIZED_COPPER_PILLAR, WAXED_OXIDIZED_COPPER_PILLAR);

        copperPillars.forEach(OxidizableBlocksRegistry::registerWaxableBlockPair);

        List<Block> unwaxedCopperPillars = List.copyOf(copperPillars.keySet());
        for (int i = 0, l = copperPillars.size() - 1; i < l; i++) OxidizableBlocksRegistry.registerOxidizableBlockPair(unwaxedCopperPillars.get(i), unwaxedCopperPillars.get(i + 1));
    }
}