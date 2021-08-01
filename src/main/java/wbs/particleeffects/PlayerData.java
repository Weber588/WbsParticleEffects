package wbs.particleeffects;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import wbs.particleeffects.instance.AmbientEffectGroupInstance;

import java.util.HashMap;
import java.util.Map;

public class PlayerData {

    public static final Map<Player, PlayerData> playerDataMap = new HashMap<>();

    public PlayerData(Player player) {
        playerDataMap.put(player, this);
    }

    public AmbientEffectGroupInstance selectingFor;

    public Location pos1;
}
