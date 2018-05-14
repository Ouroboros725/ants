package com.ouroboros.ants.game.xy;

import com.google.common.base.Objects;
import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
public class XYTile {

    private static final Logger LOGGER = LoggerFactory.getLogger(XYTile.class);

    private static XYTile[][] tiles;
    private static List<XYTile> tileList;
    private static int xt;
    private static int yt;

    public static void initTiles(int xt, int yt) {
        XYTile.xt = xt;
        XYTile.yt = yt;
        tiles = new XYTile[xt][yt];
        tileList = new ArrayList<>(xt * yt);

        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                tiles[x][y] = new XYTile(x, y);
                tileList.add(tiles[x][y]);
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

    public static void updateVisitInfluence() {
        tileList.parallelStream().forEach(t -> t.getVisit().getInfluence().getAndIncrement());
    }

    public static int getXt() {
        return xt;
    }

    public static int getYt() {
        return yt;
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
    private XYTileFood food;
    private XYTileVisit visit;

    private Map<XYTile, XYTileMv> nb;

    private XYTile(int x, int y) {
        this.x = x;
        this.y = y;
        this.status = new XYTileStatus();
        this.food = new XYTileFood();
        this.visit = new XYTileVisit();
    }

    private void initNB() {
        nb = new ConcurrentHashMap<>(4);
        {
            int e = Utils.nc(x + 1, xt);
            nb.put(tiles[e][y], new XYTileMv(tiles[e][y], Direction.EAST));
        }
        {
            int s = Utils.nc(y + 1, yt);
            nb.put(tiles[x][s], new XYTileMv(tiles[x][s], Direction.SOUTH));
        }
        {
            int w = Utils.nc(x - 1, xt);
            nb.put(tiles[w][y], new XYTileMv(tiles[w][y], Direction.WEST));
        }
        {
            int n = Utils.nc(y - 1, yt);
            nb.put(tiles[x][n], new XYTileMv(tiles[x][n],  Direction.NORTH));
        }
    }

    public void removeFromNB() {
        for (XYTile tile : nb.keySet()) {
            LOGGER.info("remove tile {} from {}", this, tile);
            tile.nb.remove(this);
            LOGGER.info("nb of tile {}: {}", tile, tile.getNbDir());
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
        return nb.keySet();
    }

    public Collection<XYTileMv> getNbDir() {
        return nb.values();
    }

    public XYTileStatus getStatus() {
        return status;
    }

    public XYTileFood getFood() {
        return food;
    }

    public XYTileVisit getVisit() {
        return visit;
    }

    public int getDist(int x1, int y1) {
        return Utils.distManh(x, y, x1, y1, xt, yt);
    }

    public int getDist(Tile tile) {
        return Utils.distManh(x, y, tile.x, tile.y, xt, yt);
    }

    public int getDist(XYTile tile) {
        return Utils.distManh(x, y, tile.x, tile.y, xt, yt);
    }

    public XYTileMv getMove(XYTile tile) {
        Direction dir = Direction.getDirection(x, y, tile.x, tile.y, xt, yt);
        return new XYTileMv(this, dir);
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

    @Override
    public String toString() {
        return "XYTile{" +
                "y=" + y +
                ", x=" + x +
                '}';
    }
}
