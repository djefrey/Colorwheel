package dev.djefrey.colorwheel.compile;

import dev.djefrey.colorwheel.accessors.ShaderPackAccessor;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;

public record ClrwlShaderKey(InstanceType<?> instanceType,
                             Material material,
                             ContextShader context,
                             ShaderPack pack,
                             NamespacedId dimension,
                             boolean isShadow,
                             ClrwlPipelineCompiler.OitMode oit)
{
    public String getPath()
    {
        var instanceName = ResourceUtil.toDebugFileNameNoExtension(instanceType.vertexShader());
        var materialName = ResourceUtil.toDebugFileNameNoExtension(material.shaders().vertexSource());
        var contextName = context.nameLowerCase();

        return ((ShaderPackAccessor) pack).colorwheel$getPackName()
                + '/' + (isShadow ? "shadow" : "color")
                + '/' + instanceName
                + '/' + materialName + '_' + contextName + oit.name;
    }

    public ProgramSet programSet()
    {
        return pack.getProgramSet(dimension);
    }

    public PackDirectives packDirectives()
    {
        return programSet().getPackDirectives();
    }
}
