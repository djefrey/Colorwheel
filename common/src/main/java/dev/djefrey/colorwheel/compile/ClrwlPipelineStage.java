package dev.djefrey.colorwheel.compile;

import dev.engine_room.flywheel.backend.gl.shader.ShaderType;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import net.irisshaders.iris.helpers.StringPair;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public record ClrwlPipelineStage<K>(ShaderType type,
                                    List<String> extensions,
                                    List<StringPair> defines,
                                    List<BiFunction<K, ClrwlCompilation, SourceComponent>> fetchers,
                                    BiConsumer<K, ClrwlCompilation> compile)
{
    public static class Builder<K>
    {
        private final ShaderType type;
        private final List<String> extensions = new ArrayList<>();
        private final List<StringPair> defines = new ArrayList<>();
        private final List<BiFunction<K, ClrwlCompilation, SourceComponent>> fetchers = new ArrayList<>();
        private BiConsumer<K, ClrwlCompilation> compileCallbacks = ($, $$) -> {};

        public Builder(ShaderType type)
        {
            this.type = type;
        }

        public Builder<K> enableExtension(String ext)
        {
            extensions.add(ext);
            return this;
        }

        public Builder<K> define(StringPair pair)
        {
            this.defines.add(pair);
            return this;
        }

        public Builder<K> define(String define)
        {
            return define(new StringPair(define, ""));
        }

        public Builder<K> with(BiFunction<K, ClrwlCompilation, SourceComponent> fetch)
        {
            fetchers.add(fetch);
            return this;
        }

        public Builder<K> withLoader(BiFunction<K, ShaderSources, SourceComponent> fetch)
        {
            return with((k, c) -> fetch.apply(k, c.getLoader()));
        }

        public Builder<K> withComponent(SourceComponent component)
        {
            return with((key,  $) -> component);
        }

        public Builder<K> withComponent(Function<K, SourceComponent> sourceFetcher)
        {
            return with((key,  $) -> sourceFetcher.apply(key));
        }

        public Builder<K> withResource(ResourceLocation rl)
        {
            return withResource(($) -> rl);
        }

        public Builder<K> withResource(Function<K, ResourceLocation> sourceFetcher)
        {
            return withLoader((key,  loader) -> loader.get(sourceFetcher.apply(key)));
        }

        public Builder<K> onCompile(BiConsumer<K, ClrwlCompilation> cb)
        {
            compileCallbacks = compileCallbacks.andThen(cb);
            return this;
        }

        public ClrwlPipelineStage<K> build()
        {
            Objects.requireNonNull(type);
            Objects.requireNonNull(extensions);
            Objects.requireNonNull(defines);
            Objects.requireNonNull(fetchers);
            Objects.requireNonNull(compileCallbacks);

            return new ClrwlPipelineStage<>(type, extensions, defines, fetchers, compileCallbacks);
        }
    }
}
