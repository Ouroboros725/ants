package com.ouroboros.ants.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TileDir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.ouroboros.ants.game.Direction.getOppoDir;

/**
 * Created by zhanxies on 4/11/2018.
 *
 */
public class Search {

    public static Multimap<Direction, Tile> floodFill(Tile origin, boolean[][] blocks, Tile[][] tiles, int xt, int yt, int depth) {
        Multimap<Direction, Tile> dirTargets = MultimapBuilder.enumKeys(Direction.class).arrayListValues().build();
        for (Direction d : Direction.values()) {
            int[] co = d.getNeighbour(origin.x, origin.y, xt, yt);
            int x = co[0];
            int y = co[1];
            dirTargets.put(d, tiles[x][y]);
        }

        for (int i = 0; i < depth; i++) {
            for (Direction d : Direction.values()) {
                Collection<Tile> origins = dirTargets.get(d);
                List<Tile> nextLayer = new ArrayList<>();

                for (Tile o : origins) {
                    for (Direction od : Direction.values()) {
                        int[] co = od.getNeighbour(o.x, o.y, xt, yt);
                        int x = co[0];
                        int y = co[1];

                        if (blocks[x][y]) {
                            continue;
                        }

                        nextLayer.add(tiles[x][y]);
                    }
                }

                dirTargets.replaceValues(d, nextLayer);
            }
        }

        return dirTargets;
    }

    public static TileDir shallowDFS(Tile origin, boolean[][] targets, boolean[][] blocks,
                                         Tile[][] tiles, int xt, int yt, int depth) {
        boolean[][] searched = new boolean[xt][yt];

        Multimap<Direction, Tile> dirTargets = MultimapBuilder.enumKeys(Direction.class).arrayListValues().build();
        for (Direction d : Direction.values()) {
            int[] co = d.getNeighbour(origin.x, origin.y, xt, yt);
            int x = co[0];
            int y = co[1];
            dirTargets.put(d, tiles[x][y]);
        }

        for (int i = 0; i < depth; i++) {
            for (Direction d : Direction.values()) {
                Collection<Tile> origins = dirTargets.get(d);
                List<Tile> nextLayer = new ArrayList<>();

                TileDir dt = nextLayer(origins, targets, blocks, tiles, xt, yt, searched, nextLayer);
                if (dt != null) {
                    return new TileDir(origin, d);
                }

                dirTargets.replaceValues(d, nextLayer);
            }
        }

        return null;
    }

    public static TileDir shallowDFSBack(Tile origin, boolean[][] targets, boolean[][] blocks,
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

    private static TileDir nextLayer(Collection<Tile> origins, boolean[][] targets, boolean[][] blocks,
                                     Tile[][] tiles, int xt, int yt, boolean[][] searched, List<Tile> nextLayer) {

        for (Tile origin : origins) {
            for (Direction d : Direction.values()) {
                int[] co = d.getNeighbour(origin.x, origin.y, xt, yt);
                int x = co[0];
                int y = co[1];

                if (searched[x][y]) {
                    continue;
                }

                if (targets[x][y]) {
                    return new TileDir(tiles[x][y], getOppoDir(d));
                }

                if (blocks[x][y]) {
                    continue;
                }

                searched[x][y] = true;

                nextLayer.add(tiles[x][y]);
            }
        }

        return null;
    }

    public interface DistCalc {
        int dist(int x1, int y1, int x2, int y2);
    }

    public static TileDir aStar(Tile origin, List<Tile> targets, Tile[][] tiles, int xt, int yt, DistCalc distCalc) {
        int minDist = Integer.MAX_VALUE;
        TileDir td = null;

        for (Tile tile : targets) {
            for (Direction d : Direction.values()) {
                int[] co = d.getNeighbour(origin.x, origin.y, xt, yt);
                int x = co[0];
                int y = co[1];

                int dist = distCalc.dist(tile.x, tile.y, x, y);

                if (dist < minDist) {
                    minDist = dist;
                    td = new TileDir(tiles[origin.x][origin.y], d);
                }
            }
        }

        return td;
    }

}
