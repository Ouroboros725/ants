package com.ouroboros.ants.game;

import static com.ouroboros.ants.utils.Utils.nc;

/**
 * Created by zhanxies on 4/11/2018.
 *
 */
public enum Direction {
    EAST('e', (x, y, xt, yt) -> {int[] n = new int[2]; n[0] = nc(x + 1, xt); n[1] = y; return n;}),
    SOUTH('s', (x, y, xt, yt) -> {int[] n = new int[2]; n[0] = x; n[1] = nc(y + 1, yt); return n;}),
    WEST('w', (x, y, xt, yt) -> {int[] n = new int[2]; n[0] = nc(x - 1, xt); n[1] = y; return n;}),
    NORTH('n', (x, y, xt, yt) -> {int[] n = new int[2]; n[0] = x; n[1] = nc(y - 1, yt); return n;});

    public char c;
    private NeighbourGetter getter;

    Direction(char c, NeighbourGetter neighbourGetter) {
        this.c = c;
        this.getter = neighbourGetter;
    }

    public static Direction getOppoDir(Direction dir) {
        switch (dir) {
            case EAST:
                return WEST;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            case NORTH:
                return SOUTH;
        }

        return null;
    }

    public int[] getNeighbour(int x, int y, int xt, int yt) {
        return getter.get(x, y, xt, yt);
    }

    private interface NeighbourGetter {
        int[] get(int x, int y, int xt, int yt);
    }
}


