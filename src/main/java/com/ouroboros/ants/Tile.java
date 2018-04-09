package com.ouroboros.ants;

/**
 * Created by zhanxies on 4/3/2018.
 *
 */
public class Tile {
    int x;
    int y;

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
