package dev.djefrey.colorwheel.fabric;

import com.google.gson.*;
import dev.djefrey.colorwheel.ClrwlConfig;
import dev.djefrey.colorwheel.Colorwheel;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ClrwlConfigFabric implements ClrwlConfig
{
    public static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("colorwheel.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final ClrwlConfigFabric INSTANCE = new ClrwlConfigFabric(PATH.toFile());

    private final File file;

    private final String ALERT_INCOMPATIBLE_KEY = "alertIncompatiblePack";
    private final String ALERT_BROKEN_KEY = "alertBrokenPack";

    public boolean alertIncompatiblePack = true;
    public boolean alertBrokenPack = true;

    public ClrwlConfigFabric(File file)
    {
        this.file = file;
        Colorwheel.CONFIG = this;
    }

    @Override
    public boolean shouldAlertIncompatiblePack()
    {
        return alertIncompatiblePack;
    }

    @Override
    public boolean shouldAlertBrokenPack()
    {
        return alertBrokenPack;
    }

    public void load()
    {
        if (file.exists())
        {
            try (FileReader reader = new FileReader(file))
            {
                JsonElement json = JsonParser.parseReader(reader);

                if (json instanceof JsonObject jsonObj)
                {
                    alertIncompatiblePack = readBoolean(jsonObj, ALERT_INCOMPATIBLE_KEY, true);
                    alertBrokenPack = readBoolean(jsonObj, ALERT_BROKEN_KEY, true);
                }
            }
            catch (Exception e)
            {
                Colorwheel.LOGGER.error("Config: could not read config file", e);
                alertIncompatiblePack = true;
                alertBrokenPack = true;
            }
        }

        save();
    }

    public void save()
    {
        try (FileWriter writer = new FileWriter(file))
        {
            GSON.toJson(toJson(), writer);
        }
        catch (Exception e)
        {
            Colorwheel.LOGGER.warn("Config: could not save config to file '{}'", file.getAbsolutePath(), e);
        }
    }

    private boolean readBoolean(JsonObject json, String key, boolean deflt)
    {
        var jsonBool = json.get(key);

        if (jsonBool instanceof JsonPrimitive primitive && primitive.isBoolean())
        {
            return primitive.getAsBoolean();
        }
        else
        {
            Colorwheel.LOGGER.error("Config: '{}' is not a boolean", key);
            return deflt;
        }
    }

    private JsonElement toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty(ALERT_INCOMPATIBLE_KEY, alertIncompatiblePack);
        json.addProperty(ALERT_BROKEN_KEY, alertBrokenPack);

        return json;
    }
}
