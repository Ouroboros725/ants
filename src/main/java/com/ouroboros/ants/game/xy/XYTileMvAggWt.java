package com.ouroboros.ants.game.xy;

import java.util.concurrent.atomic.AtomicInteger;

public class XYTileMvAggWt {
    private AtomicInteger weight;
    private XYTileMv move;

    public XYTileMvAggWt(XYTileMv move) {
        this.weight = new AtomicInteger();
        this.move = move;
    }

    public AtomicInteger getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight.set(weight);
    }

    public XYTileMv getMove() {
        return move;
    }

    public void setMove(XYTileMv move) {
        this.move = move;
    }
}
