package wbs.particleeffects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;
import wbs.particleeffects.instance.AmbientEffectGroupInstance;
import wbs.utils.util.plugin.WbsMessenger;
import wbs.utils.util.plugin.WbsPlugin;

import java.util.Set;

public class CoordSelectionListener extends WbsMessenger implements Listener {
    public CoordSelectionListener(WbsPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlaceLapis(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (event.getBlock().getType() == Material.LAPIS_BLOCK) {
            if (PlayerData.playerDataMap.containsKey(player)) {
                PlayerData data = PlayerData.playerDataMap.get(player);

                if (data.selectingFor != null) {
                    event.setCancelled(true);
                    if (data.pos1 == null) {
                        sendMessage("Place a Lapis Block to select the second corner of the area.", player);
                        data.pos1 = event.getBlock().getLocation();
                    } else {
                        Location pos1 = data.pos1;
                        Location pos2 = event.getBlock().getLocation();

                        World world = pos1.getWorld();
                        assert world != null;

                        Vector max = Vector.getMaximum(pos1.toVector(), pos2.toVector());
                        Vector min = Vector.getMinimum(pos1.toVector(), pos2.toVector());

                        max.add(new Vector(1, 1, 1));
                        min.add(new Vector(0, -0.05, 0)); // So walking on areas is easier

                        pos1 = max.toLocation(world);
                        pos2 = min.toLocation(world);

                        if (data.selectingFor.setBoundingBox(pos1, pos2)) {
                            sendMessage("Outline updated for &h" + data.selectingFor.getId() + "&r!", player);
                            data.selectingFor = null;
                            data.pos1 = null;
                        } else {
                            sendMessage("The worlds of each location must be the same. Place a Lapis Block to select the first corner again.", player);
                        }
                    }
                }
            }
        }
    }
}
