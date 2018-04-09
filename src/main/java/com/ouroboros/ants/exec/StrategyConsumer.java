package com.ouroboros.ants.exec;

import com.ouroboros.ants.utils.Move;

import java.util.function.Consumer;

/**
 * Created by zhanxies on 4/8/2018.
 *
 */
@FunctionalInterface
public interface StrategyConsumer {

    void apply(Consumer<Move> output);
}
