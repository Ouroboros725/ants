package com.ouroboros.ants;

import java.util.function.Consumer;

/**
 * Created by zhanxies on 4/8/2018.
 *
 */
@FunctionalInterface
public interface StrategyFunction {

    void apply(Consumer<Move> output);
}
