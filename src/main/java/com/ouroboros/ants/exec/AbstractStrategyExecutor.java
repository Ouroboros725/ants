package com.ouroboros.ants.exec;

import com.ouroboros.ants.utils.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ouroboros.ants.Ants.strategyExecutor;
import static com.ouroboros.ants.Ants.watchDogExecutor;

/**
 * Created by zhanxies on 4/8/2018.
 *
 */
@Component
public abstract class AbstractStrategyExecutor implements StrategyExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStrategyExecutor.class);

    volatile CountDownLatch finishSwitch;

    @Override
    public void execute(StrategyConsumer function, long time) {
        AtomicBoolean outputSwitch = new AtomicBoolean(true);

        finishSwitch = new CountDownLatch(1);

        AtomicBoolean terminator = new AtomicBoolean(false);

        strategyExecutor.execute(() -> {
            try {
                function.apply(m -> this.stepOutput(m, outputSwitch), terminator);
                finishSwitch.countDown();
            } catch (Exception ex) {
                LOGGER.error("strategy unfinished.", ex);
            }
        });

        watchDogExecutor.execute(() -> {
            try {
                try {
                    boolean done = finishSwitch.await(time, TimeUnit.MILLISECONDS);
                    if (!done) {
                        LOGGER.debug("unfinished turn");
                    }
                } catch (InterruptedException e) {
//                LOGGER.error("error when waiting for turn finish", e);
                    Thread.currentThread().interrupt();
                    throw e;
                }
            } catch (Exception ex) {
                LOGGER.error("timer crashed.", ex);
            } finally {
                terminator.set(true);
                finishOutput(outputSwitch);
            }
        });
    }

    boolean stepOutput(Move move, AtomicBoolean outputSwitch) {
        if (outputSwitch.get()) {
            this.move(move);
            return true;
        }

        return false;
    }

    private void finishOutput(AtomicBoolean outputSwitch) {
        outputSwitch.set(false);
        finish();
    }

    abstract void move(Move move);

    abstract void finish();

}
