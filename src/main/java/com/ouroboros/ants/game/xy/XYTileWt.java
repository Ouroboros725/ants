package com.ouroboros.ants.game.xy;

public class XYTileWt {
    private int weight;
    private XYTile tile;

    public XYTileWt(XYTile tile, int weight) {
        this.tile = tile;
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public XYTile getTile() {
        return tile;
    }

    public void setTile(XYTile tile) {
        this.tile = tile;
    }
}
