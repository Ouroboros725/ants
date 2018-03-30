package com.ouroboros.ants;

import java.util.List;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public interface Comm {

    List<String> input();

    void output(List<String> output);

    void stageOutput(String output);
}
