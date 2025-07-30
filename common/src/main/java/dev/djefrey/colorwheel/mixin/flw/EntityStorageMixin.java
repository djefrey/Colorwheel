package dev.djefrey.colorwheel.mixin.flw;

import dev.djefrey.colorwheel.engine.ClrwlEngine;
import dev.engine_room.flywheel.api.visual.EntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.impl.visualization.storage.EntityStorage;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityStorage.class)
public abstract class EntityStorageMixin
{
    @Shadow
    protected abstract EntityVisual<?> createRaw(VisualizationContext context, Entity obj, float partialTick);

    @Inject(method = "createRaw(Ldev/engine_room/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/entity/Entity;F)Ldev/engine_room/flywheel/api/visual/EntityVisual;",
            at = @At("HEAD"),
            cancellable = true)
    private void injectCreateRaw(VisualizationContext context, Entity obj, float partialTick, CallbackInfoReturnable<EntityVisual<?>> cir)
    {
        if (context instanceof ClrwlEngine.ClrwlMainVisualizationContext mainCtx)
        {
            var entityIds = WorldRenderingSettings.INSTANCE.getEntityIds();

            if (entityIds != null)
            {
                var entityId = BuiltInRegistries.ENTITY_TYPE.getKey(obj.getType());
                var namespace = new NamespacedId(entityId.getNamespace(), entityId.getPath());
                var id = entityIds.applyAsInt(namespace);
                var res = createRaw(mainCtx.getEntityVisualCtx(id), obj, partialTick);

                cir.setReturnValue(res);
            }
        }
    }
}
