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
    EAST('e', (x, y, xt, yt) -> Tile.getTile(nc(x + 1, xt), y)),
    SOUTH('s', (x, y, xt, yt) -> Tile.getTile(x, nc(y + 1, yt))),
    WEST('w', (x, y, xt, yt) -> Tile.getTile(nc(x - 1, xt), y)),
    NORTH('n', (x, y, xt, yt) -> Tile.getTile(x, nc(y - 1, yt)));

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

    public static Direction getDirection(Tile t1, Tile t2, int xt, int yt) {
        return getDirection(t1.x, t1.y, t2.x, t2.y, xt, yt);
    }

    public static Direction getDirection(int t1x, int t1y, int t2x, int t2y, int xt, int yt) {
        if (t1x == t2x) {
            int yd = t1y - t2y;
            if (yd == 1 || yd == 1 - yt) {
                return NORTH;
            } else if (yd == -1 || yd == yt - 1) {
                return SOUTH;
            }
        } else if (t1y == t2y) {
            int xd = t1x - t2x;
            if (xd == 1 || xd == 1 - xt) {
                return WEST;
            } else if (xd == -1 || xd == xt - 1) {
                return EAST;
            }
        }

        return null;
    }

    private static final List<Direction> SHUFFLED_VALUES = Arrays.asList(Direction.values());

    public static List<Direction> getValuesRandom() {
        if (ThreadLocalRandom.current().nextDouble() < 0.20d) {
            Collections.shuffle(SHUFFLED_VALUES, ThreadLocalRandom.current());
        }
        return SHUFFLED_VALUES;
    }

    private char c;
    private NeighbourGetter getter;

    Direction(char c, NeighbourGetter neighbourGetter) {
        this.c = c;
        this.getter = neighbourGetter;
    }

    public char getChar() {
        return c;
    }

    public Tile getNeighbour(int x, int y, int xt, int yt) {
        return getter.get(x, y, xt, yt);
    }

    private interface NeighbourGetter {
        Tile get(int x, int y, int xt, int yt);
    }

    @Override
    public String toString() {
        return "Direction{" +
                "c=" + c +
                '}';
    }
}


