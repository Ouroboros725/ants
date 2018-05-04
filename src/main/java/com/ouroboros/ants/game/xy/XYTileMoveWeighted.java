package com.ouroboros.ants.game.xy;

public class XYTileMoveWeighted {

    private int weight;
    private XYTileMove tile;

    public XYTileMoveWeighted(XYTileMove tile, int weight) {
        this.tile = tile;
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public XYTileMove getTile() {
        return tile;
    }

    public void setTile(XYTileMove tile) {
        this.tile = tile;
    }
}
