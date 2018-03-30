package com.ouroboros.ants;

import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
@Component
public class StrategyDumb implements Strategy {
    @Override
    public void prepare(GlobalInfo mapInfo, Consumer<Move> output) {

    }

    @Override
    public void apply(Consumer<Move> output) {

    }
}
