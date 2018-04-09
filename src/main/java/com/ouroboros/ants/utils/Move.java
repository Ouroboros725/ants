package com.ouroboros.ants.utils;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public class Move {
    public int x;
    public int y;
    public char d;

    public Move(int x, int y, char d) {
        this.x = x;
        this.y = y;
        this.d = d;
    }

    @Override
    public String toString() {
        return "x:" + x + ",y:" + y + ",d:" + d;
    }
}
