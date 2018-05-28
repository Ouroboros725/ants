package com.ouroboros.ants.strategy.xy;

import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Global;
import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.game.xy.XYTileMv;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.strategy.AbstractStrategy;
import com.ouroboros.ants.utils.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
@Profile("default")
@Component
public class XYMainStrategy extends AbstractStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(XYMainStrategy.class);

    private static final Lock MOVE_LOCK = new ReentrantLock();

    private static final BiFunction<XYTileMv, Consumer<Move>, Boolean> MOVE = (m, o) -> {
        MOVE_LOCK.lock();
        try {
            if (m.getDir() != null) {
                XYTile nt = XYTile.getTile(Direction.getOppoDir(m.getDir()).getNeighbour(m.getTile().getX(), m.getTile().getY(), XYTile.getXt(), XYTile.getYt()));

                if (!nt.getStatus().isMyAnt()) {
                    o.accept(new Move(m.getTile().getX(), m.getTile().getY(), Direction.getOppoDir(m.getDir()).getChar()));

                    m.getTile().getStatus().setMyAnt(false);
                    m.getTile().getStatus().setMoved(true);

                    nt.getStatus().setMyAnt(true);
                    nt.getStatus().setMoved(true);

                    return true;
                }
            } else {
                m.getTile().getStatus().setMoved(true);
            }
        } finally {
            MOVE_LOCK.unlock();
        }

        return false;
    };

    private int xt;
    private int yt;
    private int cnt;
    private int pCnt;

    @Override
    protected void setupStrategy(Global gameStates, AtomicBoolean terminator) {
        xt = gameStates.xt;
        yt = gameStates.yt;
        cnt = xt * yt;
        pCnt = cnt * (cnt - 1) / 2;

        XYTile.initTiles(xt, yt);



    }

    @Override
    protected void executeStrategy(Turn turnInfo, Global gameStates, Consumer<Move> output, AtomicBoolean terminator) {
        execute(() -> XYTile.resetStatus(), terminator);

        execute(() -> XYTurnUpdate.updateWater(turnInfo.water), terminator);
        execute(() -> XYTurnUpdate.updateMyAnts(turnInfo.myAnts), terminator);
        execute(() -> XYTurnUpdate.updateOppAnts(turnInfo.oppAnts), terminator);

        execute(() -> XYAttackStrategy.calcOppInfArea(turnInfo.oppAnts), terminator);
        execute(() -> XYAttackStrategy.attackHills(turnInfo.oppHills, turnInfo.myAnts.size(), MOVE, output), terminator);
        execute(() -> XYDefenseStrategy.havFood(turnInfo.food, MOVE, output), terminator);
        execute(() -> XYAttackStrategy.attackEnemy(turnInfo.oppAnts, MOVE, output, terminator), terminator);
        execute(() -> XYDefenseStrategy.defendHill(turnInfo.myHills, turnInfo.myAnts.size(), MOVE, output), terminator);
        execute(() -> XYAttackStrategy.explore(turnInfo.myAnts, MOVE, output), terminator);


    }

    private void execute(Runnable runnable, AtomicBoolean terminator) {
        if (!terminator.get()) {
            runnable.run();
        } else {
            LOGGER.info("xy unfinished");
        }
    }


}
