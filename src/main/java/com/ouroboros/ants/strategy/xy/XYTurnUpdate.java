package com.ouroboros.ants.strategy.xy;

import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.xy.XYTile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
public class XYTurnUpdate {

    static void updateWater(List<Tile> water) {
        water.parallelStream().forEach(w -> XYTile.getTile(w).removeFromNB());
    }

    static Set<XYTile> getMyAnts(List<Tile> ants) {
        return ants.parallelStream().map(XYTile::getTile).collect(Collectors.toSet());
    }
}
