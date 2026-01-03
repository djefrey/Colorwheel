package dev.djefrey.colorwheel;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin
{
    Version FLW_VERSION;
    Version FLW_V10006;

    @Override
    public void onLoad(String mixinPackage)
    {
         FLW_VERSION = ClrwlXplat.INSTANCE.getFlywheelVersion();
         FLW_V10006 = new Version(1, 0, 6);
    }

    @Override
    @Nullable
    public String getRefMapperConfig()
    {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        if (mixinClassName.contains(".flw."))
        {
            if (FLW_VERSION == null)
            {
                return false;
            }

            if (mixinClassName.contains("v10000"))
            {
                return FLW_VERSION.compareTo(FLW_V10006) < 0;
            }
            else if (mixinClassName.contains("v10006"))
            {
                return FLW_VERSION.compareTo(FLW_V10006) >= 0;
            }
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
    {
    }

    @Override
    @Nullable
    public List<String> getMixins()
    {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {
    }
}
