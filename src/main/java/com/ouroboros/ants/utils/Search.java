package com.ouroboros.ants.utils;

import com.google.common.collect.Lists;
import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TileDir;

import java.util.ArrayList;
import java.util.List;

import static com.ouroboros.ants.game.Direction.getOppoDir;

/**
 * Created by zhanxies on 4/11/2018.
 *
 */
public class Search {

    public static TileDir shallowDFS(Tile origin, Tile[][] targets, Tile[][] blocks,
                                  Tile[][] tiles, int xt, int yt, int depth) {
        boolean[][] searched = new boolean[xt][yt];

        List<Tile> origins = Lists.newArrayList(origin);

        for (int i = 0; i < depth; i++) {
            List<Tile> nextLayer = new ArrayList<>();

            TileDir dt = nextLayer(origins, targets, blocks, tiles, xt, yt, searched, nextLayer);
            if (dt != null) {
                return dt;
            }

            origins = nextLayer;
        }

        return null;
    }

    private static TileDir nextLayer(List<Tile> origins, Tile[][] targets, Tile[][] blocks,
                                     Tile[][] tiles, int xt, int yt, boolean[][] searched, List<Tile> nextLayer) {

        for (Tile origin : origins) {
            for (Direction d : Direction.values()) {
                int[] co = d.getNeighbour(origin.x, origin.y, xt, yt);
                int x = co[0];
                int y = co[1];

                if (searched[x][y]) {
                    continue;
                }

                if (targets[x][y] != null) {
                    return new TileDir(tiles[x][y], getOppoDir(d));
                }

                if (blocks[x][y] != null) {
                    continue;
                }

                searched[x][y] = true;

                nextLayer.add(tiles[x][y]);
            }
        }

        return null;
    }
}
