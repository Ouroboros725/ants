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

    void init(InfoMap mapInfo) {
        xt = mapInfo.cols;
        yt = mapInfo.rows;

        tiles = new Tile[mapInfo.cols][mapInfo.rows];
        for (int x = 0; x < mapInfo.cols; x++) {
            for (int y = 0; y < mapInfo.rows; y++) {
                tiles[x][y] = new Tile(x, y);
            }
        }
    }

    void timeSetup(InfoMap mapInfo) {
        this.loadTime = mapInfo.loadtime;
        this.turnTime = mapInfo.turntime;
    }
}
