package com.ouroboros.ants;

import com.google.common.collect.EvictingQueue;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static com.ouroboros.ants.Utils.dist;
import static com.ouroboros.ants.Utils.nc;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
@Component
public class StrategyDumb extends StrategyAbstract {

    Tile[][] ants;

    EvictingQueue[][] antsQueue;

    @Override
    void setupStrategy(GameStates gameStates) {
        antsQueue = new EvictingQueue[gameStates.xt][gameStates.yt];
    }

    @Override
    void executeStrategy(InfoTurn turnInfo, GameStates gameStates, Consumer<Move> output) {
        ants = new Tile[gameStates.xt][gameStates.yt];
        for (TilePlayer t : turnInfo.liveAnts) {
            ants[t.tile.x][t.tile.y] = t.tile;
        }

        char[] dir = new char[4];
        dir[0] = 'n';
        dir[1] = 's';
        dir[2] = 'w';
        dir[3] = 'e';

        for (TilePlayer player : turnInfo.liveAnts) {
            if (player.player == 0) {
                Tile tile = player.tile;

                if (antsQueue[tile.x][tile.y] == null) {
                    antsQueue[tile.x][tile.y] = EvictingQueue.<Tile>create(20);
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

                int index = -1;
                int minDist = Integer.MAX_VALUE;

                for (int i = 0; i < 4; i++) {
                    Tile tt = t4[i];
                    if (gameStates.water[tt.x][tt.y] != null) {
                        continue;
                    }
                    if (ants[tt.x][tt.y] != null) {
                        continue;
                    }
                    if (queue.contains(tt)) {
                        continue;
                    }

                    int dist = findMinMaxDist(tt, turnInfo, gameStates, Math.random() < 0.5);
                    if (dist < minDist) {
                        minDist = dist;
                        index = i;
                    } else if (dist == minDist && Math.random() < 0.5) {
                        index = i;
                    }
                }

                if (index < 0) {
                    index = (int) Math.floor(Math.random() * 4);
                }

                Move move = new Move(tile.x, tile.y, dir[index]);
                output.accept(move);

                ants[tile.x][tile.y] = null;
                ants[t4[index].x][t4[index].y] = t4[index];

                antsQueue[tile.x][tile.y].add(t4[index]);
                antsQueue[t4[index].x][t4[index].y] = antsQueue[tile.x][tile.y];
                antsQueue[tile.x][tile.y] = null;
            }
        }
    }

    private int findMinMaxDist(Tile tile, InfoTurn turnInfo, GameStates gameStates, boolean min) {
        int dt = min ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        for (Tile t : turnInfo.food) {
            int d = dist(t.x, t.y, tile.x, tile.y, gameStates.xt, gameStates.yt);
            dt = min ? Math.min(dt, d) : Math.max(dt, d);
        }
        for (TilePlayer p : turnInfo.hill) {
            if (p.player != 0) {
                int d = dist(p.tile.x, p.tile.y, tile.x, tile.y, gameStates.xt, gameStates.yt);
                dt = min ? Math.min(dt, d) : Math.max(dt, d);
            }
        }
        return dt;
    }
}
