package com.ouroboros.ants.game.xy;

public class XYTileMvWt {

    private int weight;
    private XYTileMv tile;

    public XYTileMvWt(XYTileMv tile, int weight) {
        this.tile = tile;
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public XYTileMv getTile() {
        return tile;
    }

    public void setTile(XYTileMv tile) {
        this.tile = tile;
    }
}
