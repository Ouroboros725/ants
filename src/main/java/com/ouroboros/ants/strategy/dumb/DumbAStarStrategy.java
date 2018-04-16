package com.ouroboros.ants.strategy.dumb;

import com.google.common.collect.EvictingQueue;
import com.ouroboros.ants.game.Global;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TilePlayer;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.strategy.AbstractStrategy;
import com.ouroboros.ants.utils.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

import static com.ouroboros.ants.utils.Utils.distEucl2;
import static com.ouroboros.ants.utils.Utils.nc;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public abstract class DumbAStarStrategy extends AbstractStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DumbAStarStrategy.class);
    private static final Random RANDOM = new Random();

    boolean[][] water;
    boolean[][] ants;
    Set<Tile> ownHills;
    EvictingQueue[][] antsQueue;
    Map<Tile, Integer> enemyHills;

    @Override
    protected void setupStrategy(Global gameStates) {
        water = new boolean[gameStates.xt][gameStates.yt];
        antsQueue = new EvictingQueue[gameStates.xt][gameStates.yt];
        ownHills = new HashSet<>();
        enemyHills = new HashMap<>();
    }

    @Override
    protected void executeStrategy(Turn turnInfo, Global gameStates, Consumer<Move> output) {
        for (Tile t : turnInfo.water) {
            water[t.x][t.y] = true;
        }

        ants = new boolean[gameStates.xt][gameStates.yt];
        for (Tile t : turnInfo.oppAnts) {
            ants[t.x][t.y] = true;
        }

        for (Tile t : turnInfo.myAnts) {
            ants[t.x][t.y] = true;
        }

        for (TilePlayer h : turnInfo.hill) {
            if (h.player == 0) {
                ownHills.add(h.tile);
            } else {
                enemyHills.merge(h.tile, 2, Integer::sum);
            }
        }

        enemyHills.replaceAll((k, v) -> v - 1);

        char[] dir = new char[4];
        dir[0] = 'n';
        dir[1] = 's';
        dir[2] = 'w';
        dir[3] = 'e';

        for (Tile tile : turnInfo.myAnts) {
            if (enemyHills.containsKey(tile)) {
                enemyHills.remove(tile);
            }

            if (antsQueue[tile.x][tile.y] == null) {
                antsQueue[tile.x][tile.y] = EvictingQueue.<Tile>create(25);
            }
            EvictingQueue queue = antsQueue[tile.x][tile.y];

            int n = nc(tile.y - 1, gameStates.yt);
            int s = nc(tile.y + 1, gameStates.yt);
            int w = nc(tile.x - 1, gameStates.xt);
            int e = nc(tile.x + 1, gameStates.xt);

            Tile[] t4 = new Tile[4];
            t4[0] = Tile.getTile(tile.x, n);
            t4[1] = Tile.getTile(tile.x, s);
            t4[2] = Tile.getTile(w, tile.y);
            t4[3] = Tile.getTile(e, tile.y);

            int minDist = Integer.MAX_VALUE;
            List<Integer> indexes = new ArrayList<>(4);

            for (int i = 0; i < 4; i++) {
                Tile tt = t4[i];

                if (ownHills.contains(tt)) {
                    continue;
                }
                if (water[tt.x][tt.y]) {
                    continue;
                }
                if (ants[tt.x][tt.y]) {
                    continue;
                }
                if (queue.contains(tt)) {
                    continue;
                }

                int dist = findMinMaxDist(tt, turnInfo, gameStates, RANDOM.nextInt(4) < 3);
                if (dist < minDist) {
                    minDist = dist;
                    indexes.clear();
                    indexes.add(i);
                } else if (dist == minDist && Math.random() < 0.5) {
                    indexes.add(i);
                }
            }

            int index = -1;
            if (indexes.size() == 1) {
                index = indexes.get(0);
            } else if (indexes.size() > 1) {
                index = indexes.get(RANDOM.nextInt(indexes.size()));
            }

            while (index < 0) {
                index = RANDOM.nextInt(5);

                if (index == 4) {
                    index = -1;
                    break;
                } else {
                    Tile tt = t4[index];
                    if (ownHills.contains(tt)) {
                        index = -1;
                    }
                    if (water[tt.x][tt.y]) {
                        index = -1;
                    }
                    if (ants[tt.x][tt.y]) {
                        index = -1;
                    }
                }
            }

            if (index < 0) {
                continue;
            }

            Move move = new Move(tile.x, tile.y, dir[index]);
            output.accept(move);

            ants[t4[index].x][t4[index].y] = true;
            ants[tile.x][tile.y] = false;

            antsQueue[tile.x][tile.y].add(t4[index]);
            antsQueue[t4[index].x][t4[index].y] = antsQueue[tile.x][tile.y];
            antsQueue[tile.x][tile.y] = null;
        }
    }

    private int findMinMaxDist(Tile tile, Turn turnInfo, Global gameStates, boolean min) {
        int dt = min ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        boolean attack = false;
        for (Map.Entry<Tile, Integer> entry : enemyHills.entrySet()) {
            if (entry.getValue() < 25) {
                Tile t = entry.getKey();
                int d = distEucl2(t.x, t.y, tile.x, tile.y, gameStates.xt, gameStates.yt);
                dt = min ? Math.min(dt, d) : Math.max(dt, d);
                attack = true;
            }
        }
        if (attack) {
            return dt;
        }

        for (Tile t : turnInfo.food) {
            int d = dist(t.x, t.y, tile.x, tile.y, gameStates.xt, gameStates.yt);
            dt = min ? Math.min(dt, d) : Math.max(dt, d);
        }
        return dt;
    }

    abstract protected int dist(int x1, int y1, int x2, int y2, int xt, int yt);
}
