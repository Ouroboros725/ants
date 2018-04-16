package com.ouroboros.ants.exec;

import com.ouroboros.ants.utils.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.ouroboros.ants.Ants.executor;

/**
 * Created by zhanxies on 4/8/2018.
 *
 */
@Component
public abstract class AbstractStrategyExecutor implements StrategyExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStrategyExecutor.class);

    volatile AtomicBoolean outputSwitch = new AtomicBoolean();

    volatile CountDownLatch finishSwitch;

    @Override
    public void execute(StrategyConsumer function, long time) {
        outputSwitch.set(true);
        finishSwitch = new CountDownLatch(1);

        executor.execute(() -> {
            try {
                function.apply(this::stepOutput);
                finishSwitch.countDown();
            } catch (Exception ex) {
                LOGGER.error("strategy crashed.", ex);
            }
        });

        executor.execute(() -> {
            try {
                try {
                    boolean done = finishSwitch.await(time, TimeUnit.MILLISECONDS);
                    if (!done) {
                        LOGGER.debug("unfinished turn");
                    }
                } catch (InterruptedException e) {
//                LOGGER.error("error when waiting for turn finish", e);
                    Thread.currentThread().interrupt();
                }

                finishOutput();
            } catch (Exception ex) {
                LOGGER.error("timer crashed.", ex);
            }
        });
    }

    boolean stepOutput(Move move) {
        if (outputSwitch.get()) {
            this.move(move);
            return true;
        }

        return false;
    }

    private void finishOutput() {
        outputSwitch.set(false);
        finish();
    }

    abstract void move(Move move);

    abstract void finish();

}
