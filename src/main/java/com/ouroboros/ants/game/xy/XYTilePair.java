package com.ouroboros.ants.game.xy;

import com.google.common.base.Objects;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
public class XYTilePair {
    private XYTile tile1;
    private XYTile tile2;

    public XYTilePair(XYTile t1, XYTile t2) {
        this.tile1 = t1;
        this.tile2 = t2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XYTilePair that = (XYTilePair) o;
        return (Objects.equal(tile1, that.tile1) && Objects.equal(tile2, that.tile2))
                || (Objects.equal(tile1, that.tile2) && Objects.equal(tile2, that.tile1)) ;
    }

    @Override
    public int hashCode() {
        int c1 = Objects.hashCode(tile1);
        int c2 = Objects.hashCode(tile2);
        return c1 > c2 ? Objects.hashCode(c1, c2) : Objects.hashCode(c2, c1);
    }
}
