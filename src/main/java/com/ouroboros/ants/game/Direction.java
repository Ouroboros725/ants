package com.ouroboros.ants.game;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    private char c;
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

    private static final List<Direction> SHUFFLED_VALUES = Arrays.asList(Direction.values());

    public static List<Direction> getValuesRandom() {
        if (ThreadLocalRandom.current().nextDouble() < 0.1d) {
            Collections.shuffle(SHUFFLED_VALUES, ThreadLocalRandom.current());
        }
        return SHUFFLED_VALUES;
    }

    public char getChar() {
        return c;
    }

    public int[] getNeighbour(int x, int y, int xt, int yt) {
        return getter.get(x, y, xt, yt);
    }

    private interface NeighbourGetter {
        int[] get(int x, int y, int xt, int yt);
    }
}


