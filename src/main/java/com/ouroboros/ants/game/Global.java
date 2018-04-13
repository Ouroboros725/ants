package com.ouroboros.ants.game;

import com.ouroboros.ants.info.Map;
import org.springframework.stereotype.Component;

/**
 * Created by zhanxies on 4/3/2018.
 *
 */
@Component
public class Global {

    public long loadTime;
    public long turnTime;

    public int xt;
    public int yt;

    public int viewRadius2;
    public int attackRadius2;
    public int spawnRadius2;

    public void warmup(Map mapInfo) {
        this.loadTime = mapInfo.loadtime;
        this.turnTime = mapInfo.turntime;

        xt = mapInfo.cols;
        yt = mapInfo.rows;

    }

    public void init(Map mapInfo) {
        Tile.init(xt, yt);

        viewRadius2 = mapInfo.viewradius2;
        attackRadius2 = mapInfo.attackradius2;
        spawnRadius2 = mapInfo.spawnradius2;

        TileDir.init(xt, yt);
    }
}
