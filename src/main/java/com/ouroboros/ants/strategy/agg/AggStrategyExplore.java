package com.ouroboros.ants.strategy.agg;

import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TileDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ouroboros.ants.strategy.agg.AggStrategySyncData.*;
import static com.ouroboros.ants.utils.Influence.infUpdate;
import static com.ouroboros.ants.utils.Search.aStar;
import static com.ouroboros.ants.utils.Search.shallowDFS;

/**
 * Created by zhanxies on 4/25/2018.
 *
 */
@Component
public class AggStrategyExplore {

    @Autowired
    AggStrategy str;

    int[][] calcBorder(List<Tile> antsT) {
        int[][] borderMap = new int[str.xt][str.yt];
        boolean[][] searched = new boolean[str.xt][str.yt];

        for (Tile at : antsT) {
            infUpdate(Tile.getTile(at.x, at.y), str.borderRadius, str.xt, str.yt, (ox, oy, i) -> {
                if (str.euclDist[at.x][at.y][ox][oy] <= str.viewRadius2) {
                    borderMap[ox][oy] = -1;
                } else {
                    borderMap[ox][oy] = 1;
                }
            }, searched);
        }

        return borderMap;
    }

    void updateVisitInfMap(int[][] borderMap) {
        int[][] visitInfMap = getVisitInfMap();

        for (int x = 0; x < str.xt; x++) {
            for (int y = 0; y < str.yt; y++) {
                if (borderMap[x][y] < 0) {
                    visitInfMap[x][y] = 0;
                } else {
                    visitInfMap[x][y]++;
                }
            }
        }

        setVisitInfMap(visitInfMap);
    }

    void explore() {

    }

    private TileDir explore(Tile ant, boolean[][] visitInf, List<Tile> border, boolean[][] blocks, boolean[][] movedAnts) {
        TileDir td = shallowDFS(ant, visitInf, movedAnts, blocks, str.xt, str.yt, str.borderRadius);
        if (td == null) {
            Set<Direction> excludes = new HashSet<>(4);
            for (Direction d : Direction.values()) {
                Tile t = d.getNeighbour(ant.x, ant.y, str.xt, str.yt);
                if (blocks[t.x][t.y] || movedAnts[t.x][t.y]) {
                    excludes.add(d);
                }
            }

            td = aStar(ant, border, excludes, str.xt, str.yt, str.manhDistCalc);
        }

        return td;
    }


    void updateLand(int[][] borders) {
        boolean[][] land = getLand();
        boolean[][] water = getWater();

        for (int x = 0; x < str.xt; x++) {
            for (int y = 0; y < str.yt; y++) {
                if (borders[x][y] < 0 && !water[x][y]) {
                    land[x][y] = true;
                }
            }
        }

        setLand(land);
    }
}
