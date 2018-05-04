package com.ouroboros.ants.game.xy;

import java.util.concurrent.atomic.AtomicInteger;

public class XYTileMoveAggWt {
    private AtomicInteger weight;
    private XYTileMove move;

    public XYTileMoveAggWt(XYTileMove move) {
        this.weight = new AtomicInteger();
        this.move = move;
    }

    public AtomicInteger getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight.set(weight);
    }

    public XYTileMove getMove() {
        return move;
    }

    public void setMove(XYTileMove move) {
        this.move = move;
    }
}
