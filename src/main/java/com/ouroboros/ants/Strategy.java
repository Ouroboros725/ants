package com.ouroboros.ants;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public interface Strategy {
    void prepare(InfoMap mapInfo, Consumer<Move> output, Runnable finish, GameStates gameStates);

    void apply(InfoTurn turnInfo, Consumer<Move> output, Runnable finish, GameStates gameStates);

    static void startTimer(String name, Runnable task, long time) {
        TimerTask timeTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        };

        Timer timer = new Timer(name);
        timer.schedule(timeTask, time);
    }
}
