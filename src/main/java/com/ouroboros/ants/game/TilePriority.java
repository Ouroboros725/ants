package com.ouroboros.ants.game;

/**
 * Created by zhanxies on 4/20/2018.
 *
 */
public class TilePriority implements Comparable<TilePriority> {
    public int priority;
    public Tile tile;

    public TilePriority(Tile tile, int priority) {
        this.tile = tile;
        this.priority = priority;
    }

    @Override
    public int compareTo(TilePriority o) {
        return this.priority - o.priority;
    }
}
