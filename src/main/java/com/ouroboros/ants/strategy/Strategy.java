package com.ouroboros.ants.strategy;

import com.ouroboros.ants.exec.StrategyExecutor;
import com.ouroboros.ants.game.Global;
import com.ouroboros.ants.info.Map;
import com.ouroboros.ants.info.Turn;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public interface Strategy {
    void prepare(Map mapInfo, Global gameStates, StrategyExecutor turnExec);

    void apply(Turn turnInfo, Global gameStates, StrategyExecutor turnExec);
}
