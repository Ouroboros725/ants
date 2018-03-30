package com.ouroboros.ants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
@Component
public class Botxy implements Bot {

    @Autowired
    Strategy strategy;

    @Autowired
    Comm comm;

    @Override
    public void run() {
        System.out.println("123");
    }
}
