package com.ouroboros.ants.strategy.xy;

import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Global;
import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.game.xy.XYTileMove;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.strategy.AbstractStrategy;
import com.ouroboros.ants.utils.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
@Profile("default")
@Component
public class XYMainStrategy extends AbstractStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(XYMainStrategy.class);

    private static final BiConsumer<XYTileMove, Consumer<Move>> MOVE = (m, o) -> {
        o.accept(new Move(m.getTile().getX(), m.getTile().getY(), Direction.getOppoDir(m.getDir()).getChar()));
        m.getTile().getStatus().setMyAnt(false);
        XYTile nt = XYTile.getTile(Direction.getOppoDir(m.getDir()).getNeighbour(m.getTile().getX(), m.getTile().getY(), XYTile.getXt(), XYTile.getYt()));
        nt.getStatus().setMyAnt(true);
        nt.getStatus().setMoved(true);
    };

    private int xt;
    private int yt;
    private int cnt;
    private int pCnt;

    @Override
    protected void setupStrategy(Global gameStates) {
        xt = gameStates.xt;
        yt = gameStates.yt;
        cnt = xt * yt;
        pCnt = cnt * (cnt - 1) / 2;

        XYTile.initTiles(xt, yt);



    }

    @Override
    protected void executeStrategy(Turn turnInfo, Global gameStates, Consumer<Move> output) {
        XYTile.resetStatus();

        XYTurnUpdate.updateWater(turnInfo.water);
        Set<XYTile> myAnts = XYTurnUpdate.getMyAnts(turnInfo.myAnts);
        XYAttackStrategy.calcOppInfArea(turnInfo.oppAnts);
        XYAttackStrategy.attackHills(turnInfo.oppHills, myAnts.size(), MOVE, output);
        XYDefenseStrategy.havFood(turnInfo.food, MOVE, output);



    }


}
