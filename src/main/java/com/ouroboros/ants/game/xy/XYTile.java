package com.ouroboros.ants.game.xy;

import com.google.common.base.Objects;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.utils.Utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
public class XYTile {

    private static XYTile[][] tiles;
    private static int xt;
    private static int yt;

    public static void initTiles(int xt, int yt) {
        XYTile.xt = xt;
        XYTile.yt = yt;
        tiles = new XYTile[xt][yt];

        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                tiles[x][y] = new XYTile(x, y);
            }
        }

        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                tiles[x][y].initNB();
            }
        }
    }

    public static void resetStatus() {
        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                tiles[x][y].status.reset();
            }
        }
    }

    public static XYTile getTile(int x, int y) {
        return tiles[x][y];
    }

    public static XYTile getTile(Tile tile) {
        return tiles[tile.x][tile.y];
    }

    private int x;
    private int y;
    private XYTileStatus status;

    private Set<XYTile> nb;

    private XYTile(int x, int y) {
        this.x = x;
        this.y = y;
        this.status = new XYTileStatus();
    }

    private void initNB() {
        nb = new HashSet<>(4);
        {
            int e = Utils.nc(x + 1, xt);
            nb.add(tiles[e][y]);
        }
        {
            int s = Utils.nc(y + 1, yt);
            nb.add(tiles[x][s]);
        }
        {
            int w = Utils.nc(x - 1, xt);
            nb.add(tiles[w][y]);
        }
        {
            int n = Utils.nc(y - 1, yt);
            nb.add(tiles[x][n]);
        }
    }

    public void removeFromNB() {
        for (XYTile tile : nb) {
            tile.nb.remove(this);
        }

        nb.clear();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Set<XYTile> getNb() {
        return nb;
    }

    public XYTileStatus getStatus() {
        return status;
    }

    public int getDist(int x1, int y1) {
        return Utils.distEucl2(x, y, x1, y1, xt, yt);
    }

    public int getDist(Tile tile) {
        return Utils.distEucl2(x, y, tile.x, tile.y, xt, yt);
    }

    public int getDist(XYTile tile) {
        return Utils.distEucl2(x, y, tile.x, tile.y, xt, yt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XYTile xyTile = (XYTile) o;
        return Objects.equal(x, xyTile.x) &&
                Objects.equal(y, xyTile.y);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x, y);
    }
}
