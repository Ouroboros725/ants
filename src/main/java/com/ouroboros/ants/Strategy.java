package com.ouroboros.ants;

import java.util.function.Consumer;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public interface Strategy {
    void prepare(MapInfo mapInfo, Consumer<Move> output);

    void apply(Consumer<Move> output);
}
