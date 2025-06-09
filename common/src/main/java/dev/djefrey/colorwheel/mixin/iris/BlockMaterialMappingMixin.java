package dev.djefrey.colorwheel.mixin.iris;

import net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping;
import net.irisshaders.iris.shaderpack.materialmap.BlockRenderType;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(BlockMaterialMapping.class)
public class BlockMaterialMappingMixin
{
    @Inject(method = "createBlockTypeMap",
            at = @At(value = "RETURN"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            remap = false)
    private static void fixTranslucentWaterwheels(Map<NamespacedId, BlockRenderType> blockPropertiesMap, CallbackInfoReturnable<Map<Block, BlockRenderType>> cir, Map<Block, BlockRenderType> blockTypeIds)
    {
        blockTypeIds.remove(Blocks.AIR);
    }
}
