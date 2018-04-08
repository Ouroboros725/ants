package com.ouroboros.ants;

import org.springframework.stereotype.Component;

/**
 * Created by zhanxies on 4/3/2018.
 *
 */
@Component
public class GameStates {

    long loadTime;
    long turnTime;

    Tile[][] tiles;

    int xt;
    int yt;

    Tile[][] water;

    void warmup(InfoMap mapInfo) {
        this.loadTime = mapInfo.loadtime;
        this.turnTime = mapInfo.turntime;

        xt = mapInfo.cols;
        yt = mapInfo.rows;

    }

    void init(InfoMap mapInfo) {
        tiles = new Tile[mapInfo.cols][mapInfo.rows];
        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                tiles[x][y] = new Tile(x, y);
            }
        }

        water = new Tile[xt][yt];
    }

    void update(InfoTurn infoTurn) {
        for (Tile t : infoTurn.water) {
            water[t.x][t.y] = t;
        }
    }

}
