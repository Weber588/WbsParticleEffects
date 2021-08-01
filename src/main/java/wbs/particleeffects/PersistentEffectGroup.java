package wbs.particleeffects;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wbs.particleeffects.instance.EffectGroupInstance;
import wbs.utils.util.configuration.WbsConfigReader;

public class PersistentEffectGroup {

    private final String id;
    private String creator;
    private String description;

    public PersistentEffectGroup(@NotNull String id, @NotNull ConfigurationSection section, @NotNull ParticleEffectSettings settings, @Nullable String directory) {
        this.id = id;

        WbsConfigReader.requireSection(section, "instances", settings, directory);
        WbsConfigReader.requireSection(section, "effects", settings, directory);

        ConfigurationSection effectsSection = section.getConfigurationSection("effects");
        ConfigurationSection instanceSection = section.getConfigurationSection("instances");

        assert effectsSection != null;
        assert instanceSection != null;

        for (String key : instanceSection.getKeys(false)) {
            ConfigurationSection effectSection = instanceSection.getConfigurationSection(key);
            assert effectSection != null;
            EffectGroupInstance instance = EffectGroupInstance.buildInstance(this, effectSection, settings, directory + "/instances/" + key);
            settings.addInstance(instance, directory);

            instance.readEffectsFromConfig(effectsSection, settings, directory);
        }
    }

    public String getId() {
        return id;
    }
}
