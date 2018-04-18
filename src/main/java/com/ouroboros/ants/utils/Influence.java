package com.ouroboros.ants.utils;

import com.google.common.collect.Lists;
import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Tile;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by zhanxies on 4/13/2018.
 *
 */
public class Influence {

    @FunctionalInterface
    public interface InfUpdate {
        void update(int x, int y, int i);
    }

    public static void infUpdate(Tile tile, int depth, int xt, int yt, InfUpdate inUpdate, boolean[][] searched) {
        searched[tile.x][tile.y] = true;

        List<Tile> origins = Lists.newArrayList(tile);
        for (int i = 0; i < depth; i++) {
            List<Tile> nextLayer = new LinkedList<>();

            for (Tile o : origins) {
                inUpdate.update(o.x, o.y, i);

                for (Direction d : Direction.values()) {
                    int[] co = d.getNeighbour(o.x, o.y, xt, yt);
                    int x = co[0];
                    int y = co[1];

                    if (searched[x][y]) {
                        continue;
                    }

                    nextLayer.add(Tile.getTile(x, y));
                    searched[x][y] = true;
                }
            }

            origins = nextLayer;
        }
    }


}
