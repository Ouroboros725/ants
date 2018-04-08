package com.ouroboros.ants;

import java.util.List;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public interface TurnExec {

    Input update(List<String> states);

    void execute(StrategyFunction function, long time);
}
