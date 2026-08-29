package dev.djefrey.colorwheel.shaderpack;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.accessors.iris.ProgramSetAccessor;
import dev.djefrey.colorwheel.engine.ClrwlBlendModeOverride;
import dev.djefrey.colorwheel.engine.ShadowCulling;
import net.irisshaders.iris.gl.blending.BufferBlendInformation;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.properties.ShadowCullState;

import java.util.*;

public class ClrwlPackDirectives
{
    private final Map<ClrwlProgramId, ClrwlBlendModeOverride> programBlendOverrides;
    private final Map<ClrwlProgramId, List<BufferBlendInformation>> bufferBlendOverrides;

    private final boolean shadowEnabled;

    private final ClrwlOitConfig gbuffersOit;
    private final ClrwlOitConfig shadowOit;

    private ShadowCullState shadowCullState;

    private boolean gbuffersOcclusionCulling;
    private boolean gbuffersFrustumCulling;

    private boolean shadowOcclusionCulling;
    private boolean shadowFrustumCulling;

    public ClrwlPackDirectives(ProgramSet programSet, ClrwlShaderProperties clrwlProperties)
    {
        var directives = programSet.getPackDirectives();

        programBlendOverrides = clrwlProperties.getBlendModeOverride();
        bufferBlendOverrides = clrwlProperties.getBufferBlendModeOverrides();

        shadowEnabled = clrwlProperties.shouldRenderShadow();

        gbuffersOit = new ClrwlOitConfig(clrwlProperties.isOitEnabled(ClrwlProgramGroup.GBUFFERS),
                                         clrwlProperties.getOitCoeffRanks(ClrwlProgramGroup.GBUFFERS),
                                         clrwlProperties.getOitAccumulateOverrides(ClrwlProgramGroup.GBUFFERS));

        shadowOit = new ClrwlOitConfig(clrwlProperties.isOitEnabled(ClrwlProgramGroup.SHADOW),
                                       clrwlProperties.getOitCoeffRanks(ClrwlProgramGroup.SHADOW),
                                       clrwlProperties.getOitAccumulateOverrides(ClrwlProgramGroup.SHADOW));

        shadowCullState = clrwlProperties.getShadowCullState();

        if (shadowCullState == ShadowCullState.DEFAULT)
        {
            shadowCullState = directives.getShadowDirectives().getCullingState();
        }

        gbuffersOcclusionCulling = clrwlProperties.getShadowOcclusionCulling().orElse(directives.shouldUseOcclusionCulling());
        gbuffersFrustumCulling = clrwlProperties.getFrustumCulling().orElse(directives.shouldUseFrustumCulling());

        shadowOcclusionCulling = clrwlProperties.getShadowOcclusionCulling().orElse(false)
                             && ((ProgramSetAccessor) programSet).colorwheel$getShadowDistortSource().isPresent();
        shadowFrustumCulling = ShadowCulling.useFrustumCulling(programSet, shadowCullState);
    }

    public Optional<ClrwlBlendModeOverride> getBlendModeOverride(ClrwlProgramId programId)
    {
        return Optional.ofNullable(programBlendOverrides.get(programId));
    }

    public List<BufferBlendInformation> getBufferBlendModeOverrides(ClrwlProgramId programId)
    {
        var list = bufferBlendOverrides.get(programId);

        if (list == null)
        {
            return Collections.emptyList();
        }

        return ImmutableList.copyOf(list);
    }

    public boolean shouldRenderShadow()
    {
        return shadowEnabled;
    }

    public ClrwlOitConfig getOitConfig(ClrwlProgramGroup group)
    {
        switch (group)
        {
            case GBUFFERS ->
            {
                return gbuffersOit;
            }
            case SHADOW ->
            {
                return shadowOit;
            }
        }

        throw new RuntimeException("Unknown program group: " + group);
    }

    public ShadowCullState getShadowCullState()
    {
        return shadowCullState;
    }

    public boolean getOcclusionCulling(ClrwlProgramGroup programGroup)
    {
        switch (programGroup)
        {
            case GBUFFERS -> {
                return gbuffersOcclusionCulling;
            }

            case SHADOW -> {
                return shadowOcclusionCulling;
            }
        }

        throw new RuntimeException("Unknown program group: " + programGroup);
    }

    public boolean getFrustumCulling(ClrwlProgramGroup programGroup)
    {
        switch (programGroup)
        {
            case GBUFFERS -> {
                return gbuffersFrustumCulling;
            }

            case SHADOW -> {
                return shadowFrustumCulling;
            }
        }

        throw new RuntimeException("Unknown program group: " + programGroup);
    }
}
