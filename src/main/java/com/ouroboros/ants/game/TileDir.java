package com.ouroboros.ants.game;

/**
 * Created by zhanxies on 4/11/2018.
 *
 */
public class TileDir {
    public Direction direction;
    public Tile tile;

    private TileDir(Tile tile, Direction direction) {
        this.tile = tile;
        this.direction = direction;
    }

    private static TileDir[][][] cache;

    public static void init(int xt, int yt) {
        cache = new TileDir[xt][yt][Direction.values().length];

        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                for (Direction d : Direction.values()) {
                    cache[x][y][d.ordinal()] = new TileDir(Tile.getTile(x, y), d);
                }
            }
        }
    }

    public static TileDir getTileDir(Tile tile, Direction direction) {
        return cache[tile.x][tile.y][direction.ordinal()];
    }

    public static TileDir getTileDir(int x, int y, Direction direction) {
        return cache[x][y][direction.ordinal()];
    }
}
