package dev.djefrey.colorwheel;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.shaderpack.option.OrderBackedProperties;
import net.irisshaders.iris.shaderpack.option.ShaderPackOptions;
import net.irisshaders.iris.shaderpack.preprocessor.PropertiesPreprocessor;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.List;

public class ClrwlShaderProperties
{
    private final Map<String, Set<Integer>> bufferBlendOff = new HashMap<>();

    public ClrwlShaderProperties()
    {
        // Empty
    }

    public ClrwlShaderProperties(String str, ShaderPackOptions options, Iterable<StringPair> environmentDefines)
    {
        String preprocessedStr = PropertiesPreprocessor.preprocessSource(str, options, environmentDefines);
        Properties properties = new OrderBackedProperties();

        try
        {
            properties.load(new StringReader(preprocessedStr));
        }
        catch (IOException e)
        {
            Colorwheel.LOGGER.error("Could not load colorwheel.properties", e);
        }

        for (Map.Entry<Object, Object> property : properties.entrySet())
        {
            String key = (String) property.getKey();
            String value = (String) property.getValue();
            String[] path = key.split("\\.");

            if (path.length == 0)
            {
                continue;
            }

            if (path[0].equals("blend"))
            {
                if (path.length == 3)
                {
                    if (!IrisRenderSystem.supportsBufferBlending())
                    {
                        throw new RuntimeException("Buffer blending not supported");
                    }

                    String program = path[1];
                    String buffer = path[2];

                    int id;

                    if (program.equals("clrwl_shadow"))
                    {
                        if (!buffer.startsWith("shadowcolor"))
                        {
                            Colorwheel.LOGGER.error("Attempt to disable blend on invalid buffer: {}", buffer);
                            continue;
                        }

                        try
                        {
                            id = Integer.parseInt(buffer.substring("shadowcolor".length()));
                        }
                        catch (NumberFormatException e)
                        {
                            Colorwheel.LOGGER.error("Attempt to disable blend on invalid buffer: {}", buffer);
                            continue;
                        }
                    }
                    else
                    {
                        if (!buffer.startsWith("colortex"))
                        {
                            Colorwheel.LOGGER.error("Attempt to disable blend on invalid buffer: {}", buffer);
                            continue;
                        }

                        try
                        {
                            id = Integer.parseInt(buffer.substring("colortex".length()));
                        }
                        catch (NumberFormatException e)
                        {
                            Colorwheel.LOGGER.error("Attempt to disable blend on invalid buffer: {}", buffer);
                            continue;
                        }
                    }

                    var set = bufferBlendOff.computeIfAbsent(program, (s) -> new HashSet<>());

                    if (value.equals("on"))
                    {
                        set.remove(id);
                    }
                    else if (value.equals("off"))
                    {
                        set.add(id);
                    }
                    else
                    {
                        Colorwheel.LOGGER.error("Unexpected value '{}' for {}", value, key);
                    }
                }

                continue;
            }
        }
    }

    public List<Integer> getGbuffersBufferBlendOff()
    {
        return getBufferBlendOff("clrwl_gbuffers");
    }

    public List<Integer> getShadowBufferBlendOff()
    {
        return getBufferBlendOff("clrwl_shadow");
    }

    public List<Integer> getDamagedblockBufferBlendOff()
    {
        return getBufferBlendOff("clrwl_damagedblock");
    }

    private List<Integer> getBufferBlendOff(String program)
    {
        var set = bufferBlendOff.get(program);

        if (set == null)
        {
            return Collections.emptyList();
        }

        return ImmutableList.copyOf(set);
    }
}
