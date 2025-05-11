package dev.djefrey.colorwheel.accessors;

import net.irisshaders.iris.gl.texture.InternalTextureFormat;

import java.util.Map;

public interface PackDirectivesAccessor
{
    default Map<Integer, Integer> getCoefficientsRanks(boolean isShadow)
    {
        return isShadow ? colorwheel$getShadowCoefficientsRanks() : colorwheel$getGbuffersCoefficientsRanks();
    }

    default Map<Integer, Integer> getTranslucentCoefficients(boolean isShadow)
    {
        return isShadow ? colorwheel$getShadowTranslucentCoefficients() : colorwheel$getGbuffersTranslucentCoefficients();
    }

    default Map<Integer, InternalTextureFormat> getTranslucentAccumulateFormats(boolean isShadow)
    {
        return isShadow ? colorwheel$getShadowTranslucentAccumulateFormats() : colorwheel$getGbuffersTranslucentAccumulateFormats();
    }

    default Map<Integer, Integer> getTranslucentRenderTargets(boolean isShadow)
    {
        return isShadow ? colorwheel$getShadowTranslucentRenderTargets() : colorwheel$getGbuffersTranslucentRenderTargets();
    }

    default Map<Integer, InternalTextureFormat> getOpaqueAccumulateFormats(boolean isShadow)
    {
        return isShadow ? colorwheel$getShadowOpaqueAccumulateFormats() : colorwheel$getGbuffersOpaqueAccumulateFormats();
    }

    default Map<Integer, Integer> getOpaqueRenderTargets(boolean isShadow)
    {
        return isShadow ? colorwheel$getShadowOpaqueRenderTargets() : colorwheel$getGbuffersOpaqueRenderTargets();
    }

    Map<Integer, Integer> colorwheel$getGbuffersCoefficientsRanks();
    Map<Integer, Integer> colorwheel$getGbuffersTranslucentCoefficients();
    Map<Integer, InternalTextureFormat> colorwheel$getGbuffersTranslucentAccumulateFormats();
    Map<Integer, Integer> colorwheel$getGbuffersTranslucentRenderTargets();
    Map<Integer, InternalTextureFormat> colorwheel$getGbuffersOpaqueAccumulateFormats();
    Map<Integer, Integer> colorwheel$getGbuffersOpaqueRenderTargets();

    Map<Integer, Integer> colorwheel$getShadowCoefficientsRanks();
    Map<Integer, Integer> colorwheel$getShadowTranslucentCoefficients();
    Map<Integer, InternalTextureFormat> colorwheel$getShadowTranslucentAccumulateFormats();
    Map<Integer, Integer> colorwheel$getShadowTranslucentRenderTargets();
    Map<Integer, InternalTextureFormat> colorwheel$getShadowOpaqueAccumulateFormats();
    Map<Integer, Integer> colorwheel$getShadowOpaqueRenderTargets();
}
