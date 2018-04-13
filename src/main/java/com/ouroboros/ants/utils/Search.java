package com.ouroboros.ants.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TileDir;

import java.util.*;

import static com.ouroboros.ants.game.Direction.getOppoDir;
import static com.ouroboros.ants.utils.Utils.getRandomElement;

/**
 * Created by zhanxies on 4/11/2018.
 *
 */
public class Search {

    public static Multimap<Direction, Tile> floodFill(Tile origin, boolean[][] blocks, int xt, int yt, int depth) {
        boolean[][] searched = new boolean[xt][yt];
        searched[origin.x][origin.y] = true;

        Multimap<Direction, Tile> dirTargets = MultimapBuilder.enumKeys(Direction.class).arrayListValues().build();
        for (Direction d : Direction.values()) {
            int[] co = d.getNeighbour(origin.x, origin.y, xt, yt);
            int x = co[0];
            int y = co[1];
            dirTargets.put(d, Tile.getTile(x, y));
            searched[x][y] = true;
        }

        for (int i = 1; i < depth; i++) {
            for (Direction d : Direction.values()) {
                Collection<Tile> origins = dirTargets.get(d);
                List<Tile> nextLayer = new ArrayList<>(2 * i + 1);

                for (Tile o : origins) {
                    for (Direction od : Direction.values()) {
                        int[] co = od.getNeighbour(o.x, o.y, xt, yt);
                        int x = co[0];
                        int y = co[1];

                        if (blocks[x][y]) {
                            continue;
                        }

                        if (searched[x][y]) {
                            continue;
                        }

                        nextLayer.add(Tile.getTile(x, y));
                    }
                }

                dirTargets.replaceValues(d, nextLayer);
            }

            if (i != depth - 1) {
                for (Direction d : Direction.values()) {
                    Collection<Tile> origins = dirTargets.get(d);
                    for (Tile o : origins) {
                        searched[o.x][o.y] = true;
                    }
                }
            }
        }

        return dirTargets;
    }

    public static TileDir shallowDFS(Tile origin, boolean[][] targets, boolean[][] blocks,
                                         int xt, int yt, int depth) {
        boolean[][] searched = new boolean[xt][yt];
        searched[origin.x][origin.y] = true;

        List<TileDir> results = new ArrayList<>();

        Multimap<Direction, Tile> dirTargets = MultimapBuilder.enumKeys(Direction.class).arrayListValues().build();
        for (Direction d : Direction.values()) {
            int[] co = d.getNeighbour(origin.x, origin.y, xt, yt);
            int x = co[0];
            int y = co[1];

            if (targets[x][y]) {
                results.add(TileDir.getTileDir(origin, d));
                continue;
            }

            if (results.isEmpty()) {
                searched[x][y] = true;
                dirTargets.put(d, Tile.getTile(x, y));
            }
        }

        if (results.isEmpty()) {
            for (int i = 1; i < depth; i++) {
                for (Direction d : Direction.values()) {
                    Collection<Tile> origins = dirTargets.get(d);
                    List<Tile> nextLayer = new ArrayList<>(2 * i + 1);

                    List<TileDir> dt = nextLayer(origins, targets, blocks, xt, yt, searched, nextLayer);
                    if (!dt.isEmpty()) {
                        results.add(TileDir.getTileDir(origin, d));
                        continue;
                    }

                    if (results.isEmpty()) {
                        dirTargets.replaceValues(d, nextLayer);
                    }
                }

                if (!results.isEmpty()) {
                    break;
                }
            }
        }

        if (!results.isEmpty()) {
            return getRandomElement(results);
        }

        return null;
    }

    public static TileDir shallowDFSBack(Tile origin, boolean[][] targets, boolean[][] blocks,
                                  int xt, int yt, int depth) {
        boolean[][] searched = new boolean[xt][yt];
        searched[origin.x][origin.y] = true;

        List<Tile> origins = Lists.newArrayList(origin);

        for (int i = 0; i < depth; i++) {
            List<Tile> nextLayer = new ArrayList<>(i * 4);

            List<TileDir> dt = nextLayer(origins, targets, blocks, xt, yt, searched, nextLayer);
            if (!dt.isEmpty()) {
                return getRandomElement(dt);
            }

            origins = nextLayer;
        }

        return null;
    }

    /**
     * TODO shuffle the directions instead of searching the whole layer and randomly picking one
     * TODO use set instead of sparse vector for tracking searched
     */
    private static List<TileDir> nextLayer(Collection<Tile> origins, boolean[][] targets, boolean[][] blocks,
                                     int xt, int yt, boolean[][] searched, List<Tile> nextLayer) {
        List<TileDir> results = new ArrayList<>();

        for (Tile origin : origins) {
            for (Direction d : Direction.values()) {
                int[] co = d.getNeighbour(origin.x, origin.y, xt, yt);
                int x = co[0];
                int y = co[1];

                if (targets[x][y]) {
                    results.add(TileDir.getTileDir(Tile.getTile(x, y), getOppoDir(d)));
                    continue;
                }

                if (searched[x][y]) {
                    continue;
                }

                if (blocks[x][y]) {
                    continue;
                }

                searched[x][y] = true;

                nextLayer.add(Tile.getTile(x, y));
            }
        }

        return results;
    }

    public interface DistCalc {
        int dist(int x1, int y1, int x2, int y2);
    }

    public static TileDir aStar(Tile origin, List<Tile> targets, int xt, int yt, DistCalc distCalc) {
        int minDist = Integer.MAX_VALUE;
        List<TileDir> td = new ArrayList<>();

        for (Tile tile : targets) {
            for (Direction d : Direction.values()) {
                int[] co = d.getNeighbour(origin.x, origin.y, xt, yt);
                int x = co[0];
                int y = co[1];

                int dist = distCalc.dist(tile.x, tile.y, x, y);

                if (dist < minDist) {
                    minDist = dist;
                    td.clear();
                    td.add(TileDir.getTileDir(origin.x, origin.y, d));
                } else if (dist == minDist) {
                    td.add(TileDir.getTileDir(origin.x, origin.y, d));
                }
            }
        }

        return getRandomElement(td);
    }

}
