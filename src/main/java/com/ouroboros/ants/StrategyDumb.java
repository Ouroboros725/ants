package com.ouroboros.ants;

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

    @Override
    void setupStrategy() {

    }

    @Override
    void executeStrategy(InfoTurn turnInfo, Consumer<Move> output, GameStates gameStates) {
        for (TilePlayer player : turnInfo.liveAnts) {
            if (player.player == 0) {
                Tile tile = player.tile;

                int n = nc(tile.y - 1, gameStates.yt);
                int s = nc(tile.y + 1, gameStates.yt);
                int w = nc(tile.x - 1, gameStates.xt);
                int e = nc(tile.x + 1, gameStates.xt);

                Tile[] t4 = new Tile[4];
                t4[0] = gameStates.tiles[tile.x][n];
                t4[1] = gameStates.tiles[tile.x][s];
                t4[2] = gameStates.tiles[w][tile.y];
                t4[3] = gameStates.tiles[e][tile.y];



                Move move = new Move(tile.x, tile.y, ' ');
                output.accept(move);
            }
        }
    }

    private int findMinDist(Tile tile, InfoTurn turnInfo, GameStates gameStates) {
        int dt = -1;
        for (Tile t : turnInfo.food) {
            int d = dist(t.x, t.y, tile.x, tile.y, gameStates.xt, gameStates.yt);
            dt = Math.min(dt, d);
        }
        for (TilePlayer p : turnInfo.hill) {
            int d = dist(p.tile.x, p.tile.y, tile.x, tile.y, gameStates.xt, gameStates.yt);
            dt = Math.min(dt, d);
        }
        return dt;
    }
}
