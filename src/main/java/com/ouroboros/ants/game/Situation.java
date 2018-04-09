package com.ouroboros.ants.game;

import com.ouroboros.ants.info.Map;
import com.ouroboros.ants.info.Turn;
import org.springframework.stereotype.Component;

/**
 * Created by zhanxies on 4/3/2018.
 *
 */
@Component
public class Situation {

    public long loadTime;
    public long turnTime;

    public Tile[][] tiles;

    public int xt;
    public int yt;

    public Tile[][] water;

    public void warmup(Map mapInfo) {
        this.loadTime = mapInfo.loadtime;
        this.turnTime = mapInfo.turntime;

        xt = mapInfo.cols;
        yt = mapInfo.rows;

    }

    public void init(Map mapInfo) {
        tiles = new Tile[mapInfo.cols][mapInfo.rows];
        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                tiles[x][y] = new Tile(x, y);
            }
        }

        water = new Tile[xt][yt];
    }

    public void update(Turn infoTurn) {
        for (Tile t : infoTurn.water) {
            water[t.x][t.y] = t;
        }
    }

}
