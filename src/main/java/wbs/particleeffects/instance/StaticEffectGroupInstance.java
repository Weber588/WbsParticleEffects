package wbs.particleeffects.instance;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import wbs.particleeffects.ParticleEffectSettings;
import wbs.particleeffects.PersistentEffect;
import wbs.particleeffects.PersistentEffectGroup;
import wbs.utils.util.configuration.WbsConfigReader;
import wbs.utils.util.particles.WbsParticleEffect;

import java.util.Objects;

public class StaticEffectGroupInstance extends EffectGroupInstance {

    private Location loc;

    public StaticEffectGroupInstance(PersistentEffectGroup type, String id, Location loc, boolean enabled) {
        super(type, id, enabled);
        this.loc = loc;
    }

    public StaticEffectGroupInstance(PersistentEffectGroup type, ConfigurationSection section, ParticleEffectSettings settings, String directory) {
        super(type, section, settings, directory);
        WbsConfigReader.requireNotNull(section, "location", settings, directory);
        loc = loadLocationFromString(section.getString("location"), settings, directory + "/location");

    }

    @Override
    public void teleportPlayer(Player player) {
        player.teleport(loc);
    }

    public void runEffect(PersistentEffect persistentEffect) {
        WbsParticleEffect effect = persistentEffect.getEffect();
        effect.play(
                persistentEffect.getParticle(),
                loc.clone().add(persistentEffect.getOffset().val())
        );
    }

    public Location getLocation() {
        return loc;
    }

    @Override
    public boolean move(Location location) {
        if (!super.move(location)) return false;
        loc = location;
        return true;
    }

    @Override
    public void writeToConfig(ConfigurationSection effectsConfig) {
        super.writeToConfig(effectsConfig);
        effectsConfig.set(id + ".location", loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + Objects.requireNonNull(loc.getWorld()).getName());
    }
}
