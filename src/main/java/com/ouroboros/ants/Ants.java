package com.ouroboros.ants;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public class Ants {

    public static void main(String[] args) {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(AntsConfig.class);

        Bot bot = ctx.getBean(Bot.class);
        bot.run();
    }
}
