package com.ouroboros.ants.strategy.agg;

import com.ouroboros.ants.game.Situation;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.strategy.AbstractStrategy;
import com.ouroboros.ants.utils.Move;
import org.springframework.context.annotation.Profile;

import java.util.function.Consumer;

/**
 * Created by zhanxies on 4/10/2018.
 *
 */
@Profile("agg")
public class AggStrategy extends AbstractStrategy {
    @Override
    protected void setupStrategy(Situation gameStates) {

    }

    @Override
    protected void executeStrategy(Turn turnInfo, Situation gameStates, Consumer<Move> output) {

    }
}
