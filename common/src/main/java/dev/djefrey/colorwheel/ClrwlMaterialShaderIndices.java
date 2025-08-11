package dev.djefrey.colorwheel;

import dev.djefrey.colorwheel.compile.ClrwlPrograms;
import dev.engine_room.flywheel.api.material.CutoutShader;
import dev.engine_room.flywheel.api.material.FogShader;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class ClrwlMaterialShaderIndices
{
    private static final ClrwlMaterialShaderIndices.Index fogSources = new ClrwlMaterialShaderIndices.Index();
    private static final ClrwlMaterialShaderIndices.Index cutoutSources = new ClrwlMaterialShaderIndices.Index();

    private ClrwlMaterialShaderIndices() {
    }

    public static ClrwlMaterialShaderIndices.Index fogSources() {
        return fogSources;
    }

    public static ClrwlMaterialShaderIndices.Index cutoutSources() {
        return cutoutSources;
    }

    public static int fogIndex(FogShader fogShader) {
        return fogSources().index(fogShader.source());
    }

    public static int cutoutIndex(CutoutShader cutoutShader) {
        return cutoutSources().index(cutoutShader.source());
    }

    public static class Index {
        private final Object2IntMap<ResourceLocation> sources2Index;
        private final ObjectList<ResourceLocation> sources;

        private Index()
        {
            this.sources2Index = new Object2IntOpenHashMap<>();
            sources2Index.defaultReturnValue(-1);
            this.sources = new ObjectArrayList<>();
        }

        public ResourceLocation get(int index) {
            return sources.get(index);
        }

        public int index(ResourceLocation source)
        {
            var out = sources2Index.getInt(source);

            if (out == -1)
            {
                add(source);

                ClrwlPrograms.handleUberShaderUpdate();

                return sources2Index.getInt(source);
            }

            return out;
        }

        @Unmodifiable
        public List<ResourceLocation> all() {
            return sources;
        }

        private void add(ResourceLocation source) {
            if (sources2Index.putIfAbsent(source, sources.size()) == -1) {
                sources.add(source);
            }
        }
    }
}

