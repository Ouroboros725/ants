package com.ouroboros.ants.strategy;

import com.ouroboros.ants.exec.StrategyExecutor;
import com.ouroboros.ants.game.Situation;
import com.ouroboros.ants.info.Map;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.utils.Move;

import java.util.function.Consumer;

/**
 * Created by zhanxies on 4/3/2018.
 *
 */
public abstract class AbstractStrategy implements Strategy {

    @Override
    public void prepare(Map mapInfo, Situation gameStates, StrategyExecutor turnExec) {
        gameStates.warmup(mapInfo);
        turnExec.execute(o -> { gameStates.init(mapInfo); setupStrategy(gameStates); }, (long) (gameStates.loadTime * 0.8));
    }

    protected abstract void setupStrategy(Situation gameStates);

    @Override
    public void apply(Turn turnInfo, Situation gameStates, StrategyExecutor turnExec) {
        turnExec.execute(o -> { gameStates.update(turnInfo); executeStrategy(turnInfo, gameStates, o); }, (long) (gameStates.turnTime * 0.8));
    }

    protected abstract void executeStrategy(Turn turnInfo, Situation gameStates, Consumer<Move> output);
}
