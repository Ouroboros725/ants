package com.ouroboros.ants.strategy.dumb;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ouroboros.ants.utils.Utils.distEucl2;

/**
 * Created by zhanxies on 4/12/2018.
 *
 */
@Profile("DAE")
@Component
public class DumbAStarEuclStrategy extends DumbAStarStrategy {
    @Override
    protected int dist(int x1, int y1, int x2, int y2, int xt, int yt) {
        return distEucl2(x1, y1, x2, y2, xt, yt);
    }
}
