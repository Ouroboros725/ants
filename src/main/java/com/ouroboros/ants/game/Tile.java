package com.ouroboros.ants.game;

/**
 * Created by zhanxies on 4/3/2018.
 *
 */
public class Tile {
    public int x;
    public int y;

    private Tile(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private static Tile[][] cache;

    public static void init(int xt, int yt) {
        cache = new Tile[xt][yt];

        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                cache[x][y] = new Tile(x, y);
            }
        }
    }

    public static Tile getTile(int x, int y) {
        return cache[x][y];
    }

    @Override
    public String toString() {
        return "Tile{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
