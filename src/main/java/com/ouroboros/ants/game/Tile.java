package com.ouroboros.ants.game;

/**
 * Created by zhanxies on 4/3/2018.
 *
 */
public class Tile {
    public int x;
    public int y;

    Tile(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Tile{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
