package dev.djefrey.colorwheel.fabric.mixin.flw;

import com.llamalad7.mixinextras.sugar.Local;
import dev.djefrey.colorwheel.accessors.flw.MeshEmitterManagerAccessor;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.model.baked.BlockStateModelBufferer;
import dev.engine_room.flywheel.lib.model.baked.FabricMeshEmitterManager;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStateModelBufferer.class)
public class BlockStateModelBuffererMixin
{
    @Inject(method = "bufferBlocks",
            at = @At(value = "INVOKE",
                    target = "Ldev/engine_room/flywheel/lib/model/baked/FabricMeshEmitterManager;prepare(Ldev/engine_room/flywheel/lib/model/baked/BlockMaterialFunction;)V"))
    private static void injectBeginTerrain(CallbackInfoReturnable<SimpleModel> cir, @Local(name = "emitters") FabricMeshEmitterManager emitters)
    {
        if (emitters instanceof MeshEmitterManagerAccessor accessor)
        {
            accessor.colorwheel$beginTerrain();
        }
    }

    @Inject(method = "bufferBlocks",
            at = @At(value = "RETURN"))
    private static void injectEndTerrain(CallbackInfoReturnable<SimpleModel> cir, @Local(name = "emitters") FabricMeshEmitterManager emitters)
    {
        if (emitters instanceof MeshEmitterManagerAccessor accessor)
        {
            accessor.colorwheel$endTerrain();
        }
    }

    @Inject(method = "bufferBlocks",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/block/FluidRenderer;tesselate(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/renderer/block/FluidRenderer$Output;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V"))
    private static void injectBeginFluid(CallbackInfoReturnable<SimpleModel> cir, @Local(name = "emitters") FabricMeshEmitterManager emitters, @Local(name = "pos") BlockPos pos, @Local(name = "state") BlockState state, @Local(name = "fluidState") FluidState fluidState)
    {
        if (emitters instanceof MeshEmitterManagerAccessor accessor)
        {
            if (WorldRenderingSettings.INSTANCE.getBlockStateIds() != null)
            {
                int id = WorldRenderingSettings.INSTANCE.getBlockStateIds().getOrDefault(fluidState.createLegacyBlock(), -1);
                accessor.beginBlock(id, (byte) ExtendedDataHelper.FLUID_RENDER_TYPE, (byte) state.getLightEmission(), pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }

    @Inject(method = "bufferBlocks",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/block/FluidRenderer;tesselate(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/renderer/block/FluidRenderer$Output;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V",
                     shift = At.Shift.AFTER))
    private static void injectEndFluid(CallbackInfoReturnable<SimpleModel> cir, @Local(name = "emitters") FabricMeshEmitterManager emitters)
    {
        if (emitters instanceof MeshEmitterManagerAccessor accessor)
        {
            accessor.endBlock();
        }
    }

    @Inject(method = {"bufferBlocks", "bufferModel"},
            at = @At(value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/api/client/renderer/v1/render/AltModelBlockRenderer;tesselateBlock(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"))
    private static void injectBeginBlock(CallbackInfoReturnable<SimpleModel> cir, @Local(name = "emitters") FabricMeshEmitterManager emitters, @Local(name = "pos") BlockPos pos, @Local(name = "state") BlockState state)
    {
        if (emitters instanceof MeshEmitterManagerAccessor accessor)
        {
            if (WorldRenderingSettings.INSTANCE.getBlockStateIds() != null)
            {
                int id = WorldRenderingSettings.INSTANCE.getBlockStateIds().getOrDefault(state, -1);
                accessor.beginBlock(id, (byte) ExtendedDataHelper.BLOCK_RENDER_TYPE, (byte) state.getLightEmission(), pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }

    @Inject(method = {"bufferBlocks", "bufferModel"},
            at = @At(value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/api/client/renderer/v1/render/AltModelBlockRenderer;tesselateBlock(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V",
                    shift = At.Shift.AFTER))
    private static void injectEndBlock(CallbackInfoReturnable<SimpleModel> cir, @Local(name = "emitters") FabricMeshEmitterManager emitters)
    {
        if (emitters instanceof MeshEmitterManagerAccessor accessor)
        {
            accessor.endBlock();
        }
    }
}
