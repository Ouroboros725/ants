package com.ouroboros.ants;

import java.util.function.Consumer;

/**
 * Created by zhanxies on 4/3/2018.
 *
 */
public abstract class StrategyAbstract implements Strategy {

    @Override
    public void prepare(InfoMap mapInfo, Consumer<Move> output, Runnable finish, GameStates gameStates) {
        gameStates.timeSetup(mapInfo);

        TurnWatchDog watchDog = new TurnWatchDog(finish, (long) (gameStates.loadTime * 0.8));

        gameStates.init(mapInfo);

        setupStrategy();

        watchDog.finishTurn();
    }

    abstract void setupStrategy();

    @Override
    public void apply(InfoTurn turnInfo, Consumer<Move> output, Runnable finish, GameStates gameStates) {
        TurnWatchDog watchDog = new TurnWatchDog(finish, (long) (gameStates.turnTime * 0.9));

        executeStrategy(turnInfo, output, gameStates);

        watchDog.finishTurn();
    }

    abstract void executeStrategy(InfoTurn turnInfo, Consumer<Move> output, GameStates gameStates);
}
