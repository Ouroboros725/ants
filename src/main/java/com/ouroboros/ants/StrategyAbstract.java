package com.ouroboros.ants;

import java.util.function.Consumer;

/**
 * Created by zhanxies on 4/3/2018.
 *
 */
public abstract class StrategyAbstract implements Strategy {

    @Override
    public void prepare(InfoMap mapInfo, Consumer<Move> output, Runnable finish, GameStates gameStates) {
        Strategy.startTimer("prepare", finish, (long) (gameStates.loadTime * 0.8));
    }

    @Override
    public void apply(InfoTurn turnInfo, Consumer<Move> output, Runnable finish, GameStates gameStates) {
        Strategy.startTimer("turn", finish, (long) (gameStates.turnTime * 0.9));
    }
}
