package com.ouroboros.ants.utils;

import com.google.common.collect.Lists;
import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanxies on 4/13/2018.
 *
 */
public class Influence {

    public static void incInf(Tile tile, int[][] infMap, int[] inc, int xt, int yt) {
        boolean[][] searched = new boolean[xt][yt];
        searched[tile.x][tile.y] = true;

        List<Tile> origins = Lists.newArrayList(tile);
        for (int i = 0; i < inc.length; i++) {
            List<Tile> nextLayer = new ArrayList<>(4 * (i + 1));

            for (Tile o : origins) {
                infMap[o.x][o.y] += inc[i];

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
