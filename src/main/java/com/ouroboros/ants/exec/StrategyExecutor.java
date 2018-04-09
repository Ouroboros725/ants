package com.ouroboros.ants.exec;

import com.ouroboros.ants.utils.Input;

import java.util.List;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public interface StrategyExecutor {

    Input update(List<String> states);

    void execute(StrategyConsumer function, long time);
}
