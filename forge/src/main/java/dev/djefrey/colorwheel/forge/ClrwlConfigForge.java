package dev.djefrey.colorwheel.forge;

import dev.djefrey.colorwheel.ClrwlConfig;
import dev.djefrey.colorwheel.Colorwheel;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class ClrwlConfigForge implements ClrwlConfig
{
    public static final ClrwlConfigForge INSTANCE = new ClrwlConfigForge();

    public final ClientConfig client;
    private final ForgeConfigSpec clientSpec;

    private ClrwlConfigForge()
    {
        Pair<ClientConfig, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
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

    public void registerSpecs(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, clientSpec);
    }

    public static class ClientConfig
    {
        public final ForgeConfigSpec.BooleanValue alertIncompatiblePack;
        public final ForgeConfigSpec.BooleanValue alertBrokenPack;

        private ClientConfig(ForgeConfigSpec.Builder builder)
        {
            alertIncompatiblePack = builder.comment("Should display a message when an incompatible shaderpack is used.")
                    .define("alertIncompatiblePack", true);

            alertBrokenPack = builder.comment("Should display a message when a broken shaderpack is used.")
                    .define("alertBrokenPack", true);
        }
    }
}
