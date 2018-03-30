package com.ouroboros.ants;

import java.util.List;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public interface Comm {

    Input update(List<String> states);

    void finish();

    void move(Move move);
}
