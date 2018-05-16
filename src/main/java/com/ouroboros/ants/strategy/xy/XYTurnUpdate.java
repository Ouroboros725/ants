package com.ouroboros.ants.strategy.xy;

import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.xy.XYTile;

import java.util.List;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
public class XYTurnUpdate {

    static void updateWater(List<Tile> water) {
        water.parallelStream().forEach(w -> XYTile.getTile(w).removeFromNB());
    }

    static void updateMyAnts(List<Tile> ants) {
        ants.parallelStream().map(XYTile::getTile).forEach(t -> t.getStatus().setMyAnt(true));
    }

    static void updateOppAnts(List<Tile> ants) {
        ants.parallelStream().map(XYTile::getTile).forEach(t -> t.getStatus().setOppAnt(true));
    }
}
