package com.ouroboros.ants;

import java.util.function.Consumer;

/**
 * Created by zhanxies on 4/3/2018.
 *
 */
public abstract class StrategyAbstract implements Strategy {

    @Override
    public void prepare(InfoMap mapInfo, GameStates gameStates, TurnExec turnExec) {
        gameStates.warmup(mapInfo);
        turnExec.execute(o -> { gameStates.init(mapInfo); setupStrategy(gameStates); }, (long) (gameStates.loadTime * 0.8));
    }

    abstract void setupStrategy(GameStates gameStates);

    @Override
    public void apply(InfoTurn turnInfo, GameStates gameStates, TurnExec turnExec) {
        turnExec.execute(o -> { gameStates.update(turnInfo); executeStrategy(turnInfo, gameStates, o); }, (long) (gameStates.turnTime * 0.8));
    }

    abstract void executeStrategy(InfoTurn turnInfo, GameStates gameStates, Consumer<Move> output);
}
