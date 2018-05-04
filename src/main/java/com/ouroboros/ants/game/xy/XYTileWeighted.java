package com.ouroboros.ants.game.xy;

public class XYTileWeighted {
    private int weight;
    private XYTile tile;

    public XYTileWeighted(XYTile tile, int weight) {
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
