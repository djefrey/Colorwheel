package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.ShaderType;
import dev.djefrey.colorwheel.compile.ClrwlPipelineStage;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;

import java.util.Objects;
import java.util.function.Consumer;

public record ClrwlOitCompositePipeline(String id,
                                        GlslVersion minVersion,
                                        ClrwlPipelineStage<ClrwlOitCompositeShaderKey> vertex,
                                        ClrwlPipelineStage<ClrwlOitCompositeShaderKey> fragment)
{
    public static ClrwlPipelineStage.Builder<ClrwlOitCompositeShaderKey> vertexStage()
    {
        return new ClrwlPipelineStage.Builder<>(ShaderType.VERTEX);
    }

    public static ClrwlPipelineStage.Builder<ClrwlOitCompositeShaderKey> fragmentStage()
    {
        return new ClrwlPipelineStage.Builder<>(ShaderType.FRAGMENT);
    }

    public static ClrwlOitCompositePipeline.Builder builder()
    {
        return new ClrwlOitCompositePipeline.Builder();
    }

    public static class Builder
    {
        private String id;
        private GlslVersion minVersion;
        private ClrwlPipelineStage<ClrwlOitCompositeShaderKey> vertex;
        private ClrwlPipelineStage<ClrwlOitCompositeShaderKey> fragment;

        public Builder id(String id)
        {
            this.id = id;
            return this;
        }

        public Builder minVersion(GlslVersion minVersion)
        {
            this.minVersion = minVersion;
            return this;
        }

        public Builder onSetup(Consumer<Builder> consume)
        {
            consume.accept(this);
            return this;
        }

        public Builder vertex(ClrwlPipelineStage<ClrwlOitCompositeShaderKey> stage)
        {
            this.vertex = stage;
            return this;
        }

        public Builder fragment(ClrwlPipelineStage<ClrwlOitCompositeShaderKey> stage)
        {
            this.fragment = stage;
            return this;
        }

        public ClrwlOitCompositePipeline build()
        {
            Objects.requireNonNull(id);
            Objects.requireNonNull(minVersion);
            Objects.requireNonNull(vertex);
            Objects.requireNonNull(fragment);

            return new ClrwlOitCompositePipeline(id, minVersion, vertex, fragment);
        }
    }
}
