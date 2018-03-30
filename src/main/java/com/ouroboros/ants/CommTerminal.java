package com.ouroboros.ants;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
@Component
public class CommTerminal implements Comm {
    @Override
    public List<String> input() {
        return null;
    }

    @Override
    public void output(List<String> output) {

    }

    @Override
    public void stageOutput(String output) {

    }
}
