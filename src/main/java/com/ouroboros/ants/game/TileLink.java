package com.ouroboros.ants.game;

/**
 * Created by zhanxies on 4/19/2018.
 *
 */
public class TileLink {
    public Tile current;
    public TileLink next;
    public TileLink last;

    public TileLink(Tile current, TileLink last, TileLink next) {
        this.current = current;
        this.last = last;
        this.next = next;
    }
}
