package com.ouroboros.ants.game;

/**
 * Created by zhanxies on 4/11/2018.
 *
 */
public class TileDir {
    public Direction direction;
    public Tile tile;

    public TileDir(Tile tile, Direction direction) {
        this.tile = tile;
        this.direction = direction;
    }
}
