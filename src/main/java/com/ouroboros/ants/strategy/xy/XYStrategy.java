package com.ouroboros.ants.strategy.xy;

import com.ouroboros.ants.game.Global;
import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.strategy.AbstractStrategy;
import com.ouroboros.ants.utils.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
@Profile("default")
@Component
public class XYStrategy extends AbstractStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(XYStrategy.class);

    private int xt;
    private int yt;
    private int cnt;
    private int pCnt;

    private int viewRadius2;

    @Override
    protected void setupStrategy(Global gameStates) {
        xt = gameStates.xt;
        yt = gameStates.yt;
        cnt = xt * yt;
        pCnt = cnt * (cnt - 1) / 2;

        XYTile.initTiles(xt, yt);

        viewRadius2 = gameStates.viewRadius2;



    }

    @Override
    protected void executeStrategy(Turn turnInfo, Global gameStates, Consumer<Move> output) {
        XYTile.resetStatus();

        XYTurnUpdate.updateWater(turnInfo.water);
        Set<XYTile> myAnts = XYTurnUpdate.getMyAnts(turnInfo.myAnts);
        XYOppStrategy.calcOppInfArea(turnInfo.oppAnts, viewRadius2);

    }
}
