package com.ouroboros.ants.strategy.dumb;

import com.google.common.collect.EvictingQueue;
import com.ouroboros.ants.game.Situation;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TilePlayer;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.strategy.AbstractStrategy;
import com.ouroboros.ants.utils.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

import static com.ouroboros.ants.utils.Utils.dist;
import static com.ouroboros.ants.utils.Utils.nc;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
@Component
public class DumbStrategy extends AbstractStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DumbStrategy.class);
    private static final Random RANDOM = new Random();

    Tile[][] ants;
    Set<Tile> ownHills;
    EvictingQueue[][] antsQueue;
    Map<Tile, Integer> enemyHills;

    @Override
    protected void setupStrategy(Situation gameStates) {
        antsQueue = new EvictingQueue[gameStates.xt][gameStates.yt];
        ownHills = new HashSet<>();
        enemyHills = new HashMap<>();
    }

    @Override
    protected void executeStrategy(Turn turnInfo, Situation gameStates, Consumer<Move> output) {
        ants = new Tile[gameStates.xt][gameStates.yt];
        for (TilePlayer t : turnInfo.liveAnts) {
            ants[t.tile.x][t.tile.y] = t.tile;
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

        for (TilePlayer player : turnInfo.liveAnts) {
            if (player.player == 0) {
                Tile tile = player.tile;

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
                t4[0] = gameStates.tiles[tile.x][n];
                t4[1] = gameStates.tiles[tile.x][s];
                t4[2] = gameStates.tiles[w][tile.y];
                t4[3] = gameStates.tiles[e][tile.y];

                int minDist = Integer.MAX_VALUE;
                List<Integer> indexes = new ArrayList<>(4);

                for (int i = 0; i < 4; i++) {
                    Tile tt = t4[i];

                    if (ownHills.contains(tt)) {
                        continue;
                    }
                    if (gameStates.water[tt.x][tt.y] != null) {
                        continue;
                    }
                    if (ants[tt.x][tt.y] != null) {
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
                        if (gameStates.water[tt.x][tt.y] != null) {
                            index = -1;
                        }
                        if (ants[tt.x][tt.y] != null) {
                            index = -1;
                        }
                    }
                }

                if (index < 0) {
                    continue;
                }

                Move move = new Move(tile.x, tile.y, dir[index]);
                output.accept(move);

                ants[t4[index].x][t4[index].y] = t4[index];

                antsQueue[tile.x][tile.y].add(t4[index]);
                antsQueue[t4[index].x][t4[index].y] = antsQueue[tile.x][tile.y];
                antsQueue[tile.x][tile.y] = null;
            }
        }
    }

    private int findMinMaxDist(Tile tile, Turn turnInfo, Situation gameStates, boolean min) {
        int dt = min ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        boolean attack = false;
        for (Map.Entry<Tile, Integer> entry : enemyHills.entrySet()) {
            if (entry.getValue() < 25) {
                Tile t = entry.getKey();
                int d = dist(t.x, t.y, tile.x, tile.y, gameStates.xt, gameStates.yt);
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
}
