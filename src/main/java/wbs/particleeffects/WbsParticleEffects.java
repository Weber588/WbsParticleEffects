package wbs.particleeffects;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import wbs.utils.util.plugin.WbsPlugin;

import java.util.Objects;

public class WbsParticleEffects extends WbsPlugin {

    public ParticleEffectSettings settings;

    @Override
    public void onEnable() {
        settings = new ParticleEffectSettings(this);

        settings.reload();

        Objects.requireNonNull(getCommand("particleeffect")).setExecutor(new PersistentEffectCommand(this));

        PluginManager pm = Bukkit.getPluginManager();

        pm.registerEvents(new CoordSelectionListener(this), this);

    //    settings.enableAllEffects();
    }

    @Override
    public void onDisable() {
        settings.saveAllInstances(false);
        settings.disableAllEffects();
    }
}
