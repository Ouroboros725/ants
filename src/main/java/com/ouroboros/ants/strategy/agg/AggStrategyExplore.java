package com.ouroboros.ants.strategy.agg;

import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TileDir;
import com.ouroboros.ants.game.TileDirTgt;
import com.ouroboros.ants.utils.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static com.ouroboros.ants.strategy.agg.AggStrategySyncData.*;
import static com.ouroboros.ants.utils.Influence.infUpdate;
import static com.ouroboros.ants.utils.Search.aStar;
import static com.ouroboros.ants.utils.Search.shallowDFS;

/**
 * Created by zhanxies on 4/25/2018.
 *
 */
@Profile("agg")
@Component
public class AggStrategyExplore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggStrategyExplore.class);

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

    void explore(int[][] borderMap, boolean[][] blocks, Consumer<Move> output) {
        str.moveAnts((ants, movedAnts) -> {
            int[][] visitInfMap = getVisitInfMap();
            boolean[][] visitInf = new boolean[str.xt][str.yt];
            for (int x = 0; x < str.xt; x++) {
                for (int y = 0; y <str.yt; y++) {
                    if (visitInfMap[x][y] > 7) {
                        visitInf[x][y] = true;
                    }
                }
            }

            List<Tile> border = new LinkedList<>();
            for (int x = 0; x < str.xt; x++) {
                for (int y = 0; y < str.yt; y++) {
                    if (borderMap[x][y] > 0) {
                        border.add(Tile.getTile(x, y));
                    }
                }
            }

            List<Tile> antsList = new ArrayList<>();
            for (int x = 0; x < str.xt; x++) {
                for (int y = 0; y < str.yt; y++) {
                    if (ants[x][y] && !movedAnts[x][y]) {
                        antsList.add(Tile.getTile(x, y));
                    }
                }
            }

            Collections.shuffle(antsList, ThreadLocalRandom.current());

            for (Tile ant : antsList) {
                LOGGER.debug("ant explore: {}", ant );
                TileDir td = explore(ant, visitInf, border, blocks, movedAnts);
                if (td != null) {
                    str.moveAnt(td, ants, movedAnts, output);
                }
            }
        });
    }

    private TileDir explore(Tile ant, boolean[][] visitInf, List<Tile> border, boolean[][] blocks, boolean[][] movedAnts) {
        TileDirTgt td = shallowDFS(ant, visitInf, movedAnts, blocks, str.xt, str.yt, str.borderRadius);
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

        return td != null ? td.tileDir : null;
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
