package com.ouroboros.ants.strategy.agg;

import com.ouroboros.ants.game.Tile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ouroboros.ants.strategy.agg.AggStrategySyncData.*;
import static com.ouroboros.ants.utils.Influence.infUpdate;

/**
 * Created by zhanxies on 4/25/2018.
 *
 */
@Component
public class AggStrategyCommon {

    @Autowired
    AggStrategy str;

    void updateWater(List<Tile> waterT) {
        if (isMapKnown()) {
            return;
        }

        if (waterT.isEmpty()) {
            return;
        }

        boolean[][] water = getWater();
        for (Tile w : waterT) {
            water[w.x][w.y] = true;
        }
        setWater(water);
    }

    boolean[][] getAntsAttackArea(List<Tile> antsT) {
        boolean[][] inf = new boolean[str.xt][str.yt];
        boolean[][] searched = new boolean[str.xt][str.yt];

        for (Tile at : antsT) {
            infUpdate(Tile.getTile(at.x, at.y), str.defenceRadius + 1, str.xt, str.yt, (ox, oy, i) -> {
                if (str.euclDist[at.x][at.y][ox][oy] <= str.attackRadius2 + 1) {
                    inf[ox][oy] = true;
                }
            }, searched);
        }

        return inf;
    }

    boolean[][] getTabooArea(boolean[][] antsInf) {
        boolean[][] water = getWater();

        boolean[][] blocks = new boolean[str.xt][str.yt];

        for (int x = 0; x < str.xt; x++) {
            for (int y = 0; y < str.yt; y++) {
                if (water[x][y] || antsInf[x][y]) {
                    blocks[x][y] = true;
                }
            }
        }

        return blocks;
    }

    void updateAntsMap(List<Tile> ants) {
        boolean[][] antsMap = getAnts();
        for (Tile t : ants) {
            antsMap[t.x][t.y] = true;
        }
        setAnts(antsMap);
    }

    boolean[][] getAntsMap(List<Tile> ants) {
        boolean[][] antsMap = new boolean[str.xt][str.yt];
        for (Tile t : ants) {
            antsMap[t.x][t.y] = true;
        }
        return antsMap;
    }
}
