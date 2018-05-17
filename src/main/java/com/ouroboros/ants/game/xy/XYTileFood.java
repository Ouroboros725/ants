package com.ouroboros.ants.game.xy;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Sand on 5/2/2018.
 *
 */
public class XYTileFood {

    private AtomicInteger count;
    private AtomicInteger influence;

    public XYTileFood() {
        count = new AtomicInteger(0);
        influence = new AtomicInteger(0);
    }

    public int getCount() {
        return count.get();
    }

    public void setCount(int count) {
        this.count.set(count);
    }

    public void incCount() {
        this.count.incrementAndGet();
    }

    public int getInfluence() {
        return influence.get();
    }

    public void setInfluence(int influence) {
        this.influence.set(influence);
    }
}
