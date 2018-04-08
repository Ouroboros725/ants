package com.ouroboros.ants;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public interface Strategy {
    void prepare(InfoMap mapInfo, GameStates gameStates, TurnExec turnExec);

    void apply(InfoTurn turnInfo, GameStates gameStates, TurnExec turnExec);
}
