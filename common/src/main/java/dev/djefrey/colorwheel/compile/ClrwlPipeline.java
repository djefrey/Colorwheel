package dev.djefrey.colorwheel.compile;

import dev.engine_room.flywheel.backend.glsl.GlslVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public record ClrwlPipeline(String id,
                            GlslVersion minVersion,
                            List<String> extensions,
                            ClrwlPipelineStage<ClrwlShaderKey> vertex,
                            ClrwlPipelineStage<ClrwlShaderKey> fragment)
{
    public static ClrwlPipelineStage.Builder<ClrwlShaderKey> stage()
    {
        return new ClrwlPipelineStage.Builder<>();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private String id;
        private GlslVersion minVersion;
        private final List<String> extensions = new ArrayList<>();
        private ClrwlPipelineStage<ClrwlShaderKey> vertex;
        private ClrwlPipelineStage<ClrwlShaderKey> fragment;

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

        public Builder requireExtension(String extension)
        {
            this.extensions.add(extension);
            return this;
        }

        public Builder onSetup(Consumer<Builder> consume)
        {
            consume.accept(this);
            return this;
        }

        public Builder vertex(ClrwlPipelineStage<ClrwlShaderKey> stage)
        {
            this.vertex = stage;
            return this;
        }

        public Builder fragment(ClrwlPipelineStage<ClrwlShaderKey> stage)
        {
            this.fragment = stage;
            return this;
        }

        public ClrwlPipeline build()
        {
            Objects.requireNonNull(id);
            Objects.requireNonNull(minVersion);
            Objects.requireNonNull(extensions);
            Objects.requireNonNull(vertex);
            Objects.requireNonNull(fragment);

            return new ClrwlPipeline(id, minVersion, extensions, vertex, fragment);
        }
    }
}
