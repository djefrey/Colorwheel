package dev.djefrey.colorwheel;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.*;
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
    private final Map<ClrwlProgramId, ClrwlBlendModeOverride> programBlendOverrides = new HashMap<>();
    private final Map<ClrwlProgramId, ArrayList<BufferBlendInformation>> bufferBlendOverrides = new HashMap<>();

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
                if (path.length == 2)
                {
                    var maybeProgramId = ClrwlProgramId.fromName(path[1]);

                    if (maybeProgramId.isEmpty())
                    {
                        Colorwheel.LOGGER.warn("Unknown program: {}", path[1]);
                        continue;
                    }

                    var programId = maybeProgramId.get();

                    if (value == "off")
                    {
                        programBlendOverrides.put(programId, new ClrwlBlendModeOverride(null));
                    }
                    else
                    {
                        parseBlendMode(value).ifPresent(bm -> programBlendOverrides.put(programId, new ClrwlBlendModeOverride(bm)));
                    }
                }
                else if (path.length == 3)
                {
                    if (!IrisRenderSystem.supportsBufferBlending())
                    {
                        throw new RuntimeException("Buffer blending not supported");
                    }

                    var maybeProgramId = ClrwlProgramId.fromName(path[1]);

                    if (maybeProgramId.isEmpty())
                    {
                        Colorwheel.LOGGER.warn("Unknown program: {}", path[1]);
                        continue;
                    }

                    var programId = maybeProgramId.get();
                    String buffer = path[2];

                    int id;

                    if (programId.group() == ClrwlProgramGroup.SHADOW)
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

                    var list = bufferBlendOverrides.computeIfAbsent(programId, (s) -> new ArrayList<>());

                    if (value.equals("off"))
                    {
                        list.add(new BufferBlendInformation(id, null));
                    }
                    else
                    {
                        parseBlendMode(value).ifPresent(bm -> list.add(new BufferBlendInformation(id, bm)));
                    }
                }

                continue;
            }
        }
    }

    private Optional<BlendMode> parseBlendMode(String value)
    {
        String[] modeStrs = value.split(" ");
        int[] modeFcts = new int[modeStrs.length];

        if (modeFcts.length != 4)
        {
            Colorwheel.LOGGER.error("Invalid blend function count: {} (expected 4)", modeFcts.length);
            return Optional.empty();
        }

        for (int i = 0; i < modeStrs.length; i++)
        {
            var maybeFct = BlendModeFunction.fromString(modeStrs[i]);

            if (maybeFct.isEmpty())
            {
                Colorwheel.LOGGER.error("Unknown blend function: {}", modeStrs[i]);
                continue;
            }

            modeFcts[i] = maybeFct.get().getGlId();
        }

        return Optional.of(new BlendMode(modeFcts[0], modeFcts[1], modeFcts[2], modeFcts[3]));
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
}
