package com.ouroboros.ants.strategy.agg;

import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TileLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.ouroboros.ants.strategy.agg.AggStrategySyncData.*;
import static com.ouroboros.ants.strategy.agg.AggStrategySyncData.setPreCalcFinished;
import static com.ouroboros.ants.utils.Search.findPath;

/**
 * Created by zhanxies on 4/25/2018.
 *
 */
@Component
public class AggStrategyCalc {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggStrategyCalc.class);

    @Autowired
    AggStrategy str;

    AtomicBoolean readyInd = new AtomicBoolean(false);

    void prepareCalc() {
        setPathsDict(new TileLink[str.xt][str.yt][str.xt][str.yt]);
        setPathsDist(new int[str.xt][str.yt][str.xt][str.yt]);
        readyInd.set(true);
    }

    void preCalcEucl() {
        if (!readyInd.get()) {
            return;
        }

        boolean[][] land = getLand();
        boolean[][] water = getWater();

        TileLink[][][][] pathsDict = getPathsDict();
        int[][][][] pathsDist = getPathsDist();

        boolean ready = true;
        for (int x = 0; x < str.xt; x++) {
            for (int y = 0; y < str.yt; y++) {
                if (!land[x][y] && !water[x][y]) {
                    ready = false;
                    break;
                }
            }
        }

        if (ready) {
            LOGGER.debug("paths calculation starts.");

            setMapKnown(true);

            for (int x1 = 0; x1 < str.xt; x1++) {
                for (int y1 = 0; y1 < str.yt; y1++) {
                    for (int x2 = 0; x2 < str.xt; x2++) {
                        for (int y2 = 0; y2 < str.yt; y2++) {
                            if (!water[x1][y1] && !water[x2][y2] && pathsDict[x1][y1][x2][y2] == null) {
                                findShortestPath(Tile.getTile(x1, y1), Tile.getTile(x2, y2), land, pathsDict, pathsDist);
                            }
                        }
                    }
                }
            }

            setPathsDict(pathsDict);
            setPathsDist(pathsDist);

            setPreCalcFinished(true);

            LOGGER.debug("paths calculation finishes.");
        }
    }

    private void findShortestPath(Tile t1, Tile t2, boolean[][] land, TileLink[][][][] pathsDict, int[][][][] pathsDist) {
        TileLink start = findPath(t1, t2, land, str.xt, str.yt);

        TileLink reverse = reversePath(start);
        populatePathsDict(start, pathsDict, pathsDist);
        populatePathsDict(reverse, pathsDict, pathsDist);
    }

    private TileLink reversePath(TileLink link) {
        TileLink last = null;
        TileLink next = link;
        do {
            TileLink current = new TileLink(link.current, null, last);
            if (last != null) {
                last.last = current;
            }

            last = current;
            next = next.next;
        } while (next != null);

        return last;
    }

    private void populatePathsDict(TileLink link, TileLink[][][][] pathsDict, int[][][][] pathsDist) {
        while (link.next != null) {
            TileLink next = link.next;
            int d = 1;
            do {
                pathsDict[link.current.x][link.current.y][next.current.x][next.current.y] = next;
                pathsDist[link.current.x][link.current.y][next.current.x][next.current.y] = d;
                next = next.next;
                d++;
            } while (next != null);
            link = link.next;
        }
    }
}
