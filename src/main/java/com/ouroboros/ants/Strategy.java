package com.ouroboros.ants;

import java.util.function.Consumer;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public interface Strategy {
    void prepare(InfoMap mapInfo, Consumer<Move> output, Runnable finish, GameStates gameStates);

    void apply(InfoTurn turnInfo, Consumer<Move> output, Runnable finish, GameStates gameStates);
}
