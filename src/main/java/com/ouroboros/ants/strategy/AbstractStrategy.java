package com.ouroboros.ants.strategy;

import com.ouroboros.ants.exec.StrategyExecutor;
import com.ouroboros.ants.game.Global;
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
    public void prepare(Map mapInfo, Global gameStates, StrategyExecutor turnExec) {
        gameStates.warmup(mapInfo);
        turnExec.execute(o -> { gameStates.init(mapInfo); setupStrategy(gameStates); }, (gameStates.loadTime - 25));
    }

    protected abstract void setupStrategy(Global gameStates);

    @Override
    public void apply(Turn turnInfo, Global gameStates, StrategyExecutor turnExec) {
        turnExec.execute(o -> executeStrategy(turnInfo, gameStates, o), (gameStates.turnTime - 25));
    }

    protected abstract void executeStrategy(Turn turnInfo, Global gameStates, Consumer<Move> output);
}
