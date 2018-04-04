package com.ouroboros.ants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhanxies on 4/4/2018.
 *
 */
public class TurnWatchDog {

    private static final Logger LOGGER = LoggerFactory.getLogger(TurnWatchDog.class);

    CountDownLatch trigger = new CountDownLatch(1);

    ExecutorService executor = Executors.newSingleThreadExecutor();

    TurnWatchDog(Runnable task, long time) {
//        LOGGER.debug("time for turn {}", time);

        executor.execute(() -> {
            try {
                boolean done = trigger.await(time, TimeUnit.MILLISECONDS);
                if (!done) {
                    LOGGER.debug("unfinished turn");
                }

            } catch (InterruptedException e) {
//                LOGGER.error("error when waiting for turn finish", e);
                Thread.currentThread().interrupt();
            }

            task.run();
        });
    }

    void finishTurn() {
//        LOGGER.debug("trigger turn finish");

        trigger.countDown();
    }
}
