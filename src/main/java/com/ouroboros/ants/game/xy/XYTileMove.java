package com.ouroboros.ants.game.xy;

import com.ouroboros.ants.game.Direction;

/**
 * Created by Sand on 5/2/2018.
 *
 */
public class XYTileMove {
    private XYTile tile;
    private Direction dir;

    public XYTileMove(XYTile tile, Direction dir) {
        this.tile = tile;
        this.dir = dir;
    }

    public XYTile getTile() {
        return tile;
    }

    public Direction getDir() {
        return dir;
    }
}
