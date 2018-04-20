package com.ouroboros.ants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public class Ants {

    private static final Logger LOGGER = LoggerFactory.getLogger(Ants.class);

    public static final Executor executor = Executors.newFixedThreadPool(125, r -> {
        Thread t = new Thread(r);
        t.setPriority(Thread.MAX_PRIORITY);
        return t;
    });

    public static final Executor daemonExecutor = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    public static void main(String[] args) {
        try {
            ApplicationContext ctx = new AnnotationConfigApplicationContext(AntsConfig.class);

            Bot bot = ctx.getBean(Bot.class);
            bot.run();
        } catch (Exception ex) {
            LOGGER.error("program crash.", ex);
        }
    }
}
