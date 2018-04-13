package com.ouroboros.ants;

import com.ouroboros.ants.exec.StrategyExecutor;
import com.ouroboros.ants.game.Global;
import com.ouroboros.ants.info.Map;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.strategy.Strategy;
import com.ouroboros.ants.utils.Input;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
@Component
public class Botxy implements Bot {

    private static final Logger LOGGER = LoggerFactory.getLogger(Botxy.class);

    @Autowired
    Strategy strategy;

    @Autowired
    StrategyExecutor turnExec;

    @Autowired
    Global gameStates;

    @Override
    public void run() {
        Input input;
        do {
            List<String> states = new ArrayList<>();
            input = turnExec.update(states);

            switch (input) {
                case ready:
                    Map mapInfo = new Map(states);
                    strategy.prepare(mapInfo, gameStates, turnExec);
                    break;
                case go:
                    Turn turnInfo = new Turn(states);
                    if (!turnInfo.gameEnd) {
                        strategy.apply(turnInfo, gameStates, turnExec);
                    } else {
                        input = Input.end;
                        LOGGER.debug("end of game");
                    }
                    break;
                default:
                    LOGGER.debug("end of game");
                    break;
            }
        } while (input != Input.end);

    }
}
