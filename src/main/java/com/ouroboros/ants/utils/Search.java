package com.ouroboros.ants.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.ouroboros.ants.game.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.ouroboros.ants.game.Direction.getOppoDir;
import static com.ouroboros.ants.utils.Utils.getRandomElement;

/**
 * Created by zhanxies on 4/11/2018.
 *
 */
public class Search {

    private Search() {
        throw new IllegalStateException("Utility class");
    }

    public static Multimap<Direction, Tile> floodFill(Tile origin, boolean[][] blocks, int xt, int yt, int depth) {
        boolean[][] searched = new boolean[xt][yt];
        searched[origin.x][origin.y] = true;

        Multimap<Direction, Tile> dirTargets = MultimapBuilder.enumKeys(Direction.class).linkedListValues().build();
        for (Direction d : Direction.values()) {
            Tile dt = d.getNeighbour(origin.x, origin.y, xt, yt);
            dirTargets.put(d, Tile.getTile(dt.x, dt.y));
            searched[dt.x][dt.y] = true;
        }

        for (int i = 1; i < depth; i++) {
            for (Direction d : Direction.values()) {
                Collection<Tile> origins = dirTargets.get(d);
                List<Tile> nextLayer = new LinkedList<>();

                for (Tile o : origins) {
                    for (Direction od : Direction.values()) {
                        Tile dt = od.getNeighbour(o.x, o.y, xt, yt);

                        if (blocks[dt.x][dt.y] || searched[dt.x][dt.y]) {
                            continue;
                        }

                        nextLayer.add(Tile.getTile(dt.x, dt.y));
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

    public static TileDirTgt shallowBFS(Tile origin, boolean[][] targets, boolean[][] excludeTgts, boolean[][] blocks,
                                        int xt, int yt, int depth) {
        boolean[][] searched = new boolean[xt][yt];
        searched[origin.x][origin.y] = true;

        List<TileDirTgt> results = new LinkedList<>();

        Multimap<Direction, Tile> dirTargets = MultimapBuilder.enumKeys(Direction.class).linkedListValues().build();
        for (Direction d : Direction.getValuesRandom()) {
            Tile dt = d.getNeighbour(origin.x, origin.y, xt, yt);

            if (excludeTgts[dt.x][dt.y]) {
                continue;
            }

            if (targets[dt.x][dt.y]) {
                results.add(new TileDirTgt(TileDir.getTileDir(origin, d), dt));
                continue;
            }

            if (results.isEmpty()) {
                searched[dt.x][dt.y] = true;
                dirTargets.put(d, Tile.getTile(dt.x, dt.y));
            }
        }

        if (results.isEmpty()) {
            for (int i = 1; i < depth; i++) {
                if (dirTargets.isEmpty()) {
                    break;
                }

                for (Direction d : Direction.getValuesRandom()) {
                    Collection<Tile> origins = dirTargets.get(d);

                    if (origins.isEmpty()) {
                        dirTargets.removeAll(d);
                        continue;
                    }

                    List<Tile> nextLayer = new LinkedList<>();

                    List<TileDir> dt = nextLayer(origins, targets, excludeTgts, blocks, xt, yt, searched, nextLayer, origins.size() * 4);
                    if (!dt.isEmpty()) {
                        results.addAll(dt.stream().map(di -> new TileDirTgt(TileDir.getTileDir(origin, d), di.tile)).collect(Collectors.toList()));
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

    public static List<TileDir> shallowBFSBack(Tile origin, boolean[][] targets, boolean[][] excludeTgts, boolean[][] blocks,
                                               int xt, int yt, int depth, int count) {
        boolean[][] searched = new boolean[xt][yt];
        searched[origin.x][origin.y] = true;

        List<Tile> origins = Lists.newArrayList(origin);
        List<TileDir> results = new LinkedList<>();

        for (int i = 0; i < depth; i++) {
            if (origins.isEmpty()) {
                break;
            }

            List<Tile> nextLayer = new LinkedList<>();

            List<TileDir> dt = nextLayer(origins, targets, excludeTgts, blocks, xt, yt, searched, nextLayer, count - results.size());
            if (!dt.isEmpty()) {
                if (dt.size() + results.size() < count) {
                    results.addAll(dt);
                } else {
                    results.addAll(dt.subList(0, count - results.size()));
                    return results;
                }
            }

            origins = nextLayer;
        }

        return results;
    }

    /**
     * TODO shuffle the directions instead of searching the whole layer and randomly picking one
     * TODO use set instead of sparse vector for tracking searched
     */
    private static List<TileDir> nextLayer(Collection<Tile> origins, boolean[][] targets, boolean[][] excludeTgts, boolean[][] blocks,
                                     int xt, int yt, boolean[][] searched, List<Tile> nextLayer, int count) {
        List<TileDir> results = new LinkedList<>();

        for (Tile origin : origins) {
            for (Direction d : Direction.getValuesRandom()) {
                Tile dt = d.getNeighbour(origin.x, origin.y, xt, yt);

                if (searched[dt.x][dt.y] || blocks[dt.x][dt.y] || excludeTgts[dt.x][dt.y]) {
                    continue;
                }

                if (targets[dt.x][dt.y]) {
                    results.add(TileDir.getTileDir(Tile.getTile(dt.x, dt.y), getOppoDir(d)));

                    if (results .size() == count) {
                        return results;
                    }
                }

                searched[dt.x][dt.y] = true;

                nextLayer.add(Tile.getTile(dt.x, dt.y));
            }
        }

        return results;
    }

    public static TileLink findPath(Tile origin, Tile target, boolean[][] land, int xt, int yt) {
        boolean[][] searched = new boolean[xt][yt];
        FindPathResult result = findPath(Lists.newArrayList(origin), target, land, searched, xt, yt);
        return result.link;
    }

    private static class FindPathResult {
        TileLink link;
        Tile tile;

        FindPathResult(TileLink link, Tile tile) {
            this.link = link;
            this.tile = tile;
        }
    }

    private static FindPathResult findPath(List<Tile> origins, Tile target, boolean[][] land, boolean[][] searched, int xt, int yt) {
        List<Tile> tiles = new LinkedList<>();
        Map<Tile, Tile> backLookup = new HashMap<>();
        for (Tile tile : origins) {
            for (Direction d : Direction.getValuesRandom()) {
                Tile dt = d.getNeighbour(tile.x, tile.y, xt, yt);

                if (!searched[dt.x][dt.y] && land[dt.x][dt.y]) {
                    Tile t = Tile.getTile(dt.x, dt.y);
                    if (t == target) {
                        return new FindPathResult(new TileLink(t, null, null), tile);
                    } else {
                        tiles.add(t);
                    }
                    searched[dt.x][dt.y] = true;
                    backLookup.put(t, tile);
                }
            }
        }

        FindPathResult fr = findPath(tiles, target, land, searched, xt, yt);
        Tile last = backLookup.get(fr.tile);

        TileLink result = new TileLink(fr.tile, null, fr.link);
        fr.link.last = result;

        return new FindPathResult(result, last);
    }

    public interface DistCalc {
        int dist(int x1, int y1, int x2, int y2);
    }

    public static TileDirTgt aStar(Tile origin, List<Tile> targets, Set<Direction> excludes, int xt, int yt, DistCalc distCalc) {
        int minDist = Integer.MAX_VALUE;
        List<TileDirTgt> td = new LinkedList<>();

        for (Direction d : Direction.getValuesRandom()) {
            if (excludes.contains(d)) {
                continue;
            }

            for (Tile tile : targets) {
                Tile dt = d.getNeighbour(origin.x, origin.y, xt, yt);

                int dist = distCalc.dist(tile.x, tile.y, dt.x, dt.y);

                if (dist < minDist) {
                    minDist = dist;
                    td.clear();
                    td.add(new TileDirTgt(TileDir.getTileDir(origin.x, origin.y, d), tile));
                } else if (dist == minDist) {
                    td.add(new TileDirTgt(TileDir.getTileDir(origin.x, origin.y, d), tile));
                }
            }
        }

        return getRandomElement(td);
    }

}
