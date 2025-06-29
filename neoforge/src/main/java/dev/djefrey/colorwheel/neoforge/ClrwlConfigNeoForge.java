package dev.djefrey.colorwheel.neoforge;

import dev.djefrey.colorwheel.ClrwlConfig;
import dev.djefrey.colorwheel.Colorwheel;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ClrwlConfigNeoForge implements ClrwlConfig
{
    public static final ClrwlConfigNeoForge INSTANCE = new ClrwlConfigNeoForge();

    public final ClientConfig client;
    private final ModConfigSpec clientSpec;

    private ClrwlConfigNeoForge()
    {
        Pair<ClientConfig, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        this.client = clientPair.getLeft();
        clientSpec = clientPair.getRight();

        Colorwheel.CONFIG = this;
    }

    @Override
    public boolean shouldAlertIncompatiblePack()
    {
        return client.alertIncompatiblePack.get();
    }

    @Override
    public boolean shouldAlertBrokenPack()
    {
        return client.alertBrokenPack.get();
    }

    public void registerSpecs(ModContainer context) {
        context.registerConfig(ModConfig.Type.CLIENT, clientSpec);
    }

    public static class ClientConfig
    {
        public final ModConfigSpec.BooleanValue alertIncompatiblePack;
        public final ModConfigSpec.BooleanValue alertBrokenPack;

        private ClientConfig(ModConfigSpec.Builder builder)
        {
            alertIncompatiblePack = builder.comment("Should display a message when an incompatible shaderpack is used.")
                    .define("alertIncompatiblePack", true);

            alertBrokenPack = builder.comment("Should display a message when a broken shaderpack is used.")
                    .define("alertBrokenPack", true);
        }
    }
}
