package dev.djefrey.colorwheel.compile.oit;

import dev.djefrey.colorwheel.ShaderType;
import dev.djefrey.colorwheel.compile.ClrwlPipelineStage;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;

import java.util.Objects;
import java.util.function.Consumer;

public record ClrwlOitDepthPipeline(String id,
                                    GlslVersion minVersion,
                                    ClrwlPipelineStage<ClrwlOitDepthShaderKey> vertex,
                                    ClrwlPipelineStage<ClrwlOitDepthShaderKey> fragment)
{
    public static ClrwlPipelineStage.Builder<ClrwlOitDepthShaderKey> vertexStage()
    {
        return new ClrwlPipelineStage.Builder<>(ShaderType.VERTEX);
    }

    public static ClrwlPipelineStage.Builder<ClrwlOitDepthShaderKey> fragmentStage()
    {
        return new ClrwlPipelineStage.Builder<>(ShaderType.FRAGMENT);
    }

    public static ClrwlOitDepthPipeline.Builder builder()
    {
        return new ClrwlOitDepthPipeline.Builder();
    }

    public static class Builder
    {
        private String id;
        private GlslVersion minVersion;
        private ClrwlPipelineStage<ClrwlOitDepthShaderKey> vertex;
        private ClrwlPipelineStage<ClrwlOitDepthShaderKey> fragment;

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

        public Builder vertex(ClrwlPipelineStage<ClrwlOitDepthShaderKey> stage)
        {
            this.vertex = stage;
            return this;
        }

        public Builder fragment(ClrwlPipelineStage<ClrwlOitDepthShaderKey> stage)
        {
            this.fragment = stage;
            return this;
        }

        public ClrwlOitDepthPipeline build()
        {
            Objects.requireNonNull(id);
            Objects.requireNonNull(minVersion);
            Objects.requireNonNull(vertex);
            Objects.requireNonNull(fragment);

            return new ClrwlOitDepthPipeline(id, minVersion, vertex, fragment);
        }
    }
}
