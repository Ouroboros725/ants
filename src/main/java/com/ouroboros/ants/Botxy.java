package com.ouroboros.ants;

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
    Comm comm;

    @Override
    public void run() {
        Input input;
        do {
            List<String> states = new ArrayList<>();
            input = comm.update(states);

            switch (input) {
                case ready:
                    MapInfo mapInfo = new MapInfo(states);
                    strategy.prepare(mapInfo, comm::move);
                    break;
                case go:
                    strategy.apply(comm::move);
                    break;
                default:
                    LOGGER.debug("end of game");
                    break;
            }

            comm.finish();
        } while (input != Input.end);

        System.out.println("123");
        LOGGER.info("123");
    }
}
