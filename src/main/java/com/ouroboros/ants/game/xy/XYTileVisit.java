package com.ouroboros.ants.game.xy;

import java.util.concurrent.atomic.AtomicInteger;

public class XYTileVisit {

    private AtomicInteger influence;

    public XYTileVisit() {
        influence = new AtomicInteger(0);
    }

    public AtomicInteger getInfluence() {
        return influence;
    }

    public void setInfluence(int influence) {
        this.influence.set(influence);
    }
}
