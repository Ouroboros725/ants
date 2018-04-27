package com.ouroboros.ants.game.xy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
public class XYTileStatus {
    private AtomicBoolean myAnt;
    private AtomicBoolean oppAnt;
    private AtomicBoolean taboo;
    private AtomicBoolean moved;

    XYTileStatus() {
        myAnt = new AtomicBoolean();
        oppAnt = new AtomicBoolean();
        taboo = new AtomicBoolean();
        moved = new AtomicBoolean();
    }

    void reset() {
        myAnt.set(false);
        oppAnt.set(false);
        taboo.set(false);
        moved.set(false);
    }

    public boolean isMyAnt() {
        return myAnt.get();
    }

    public void setMyAnt(boolean myAnt) {
        this.myAnt.set(myAnt);
    }

    public boolean isOppAnt() {
        return oppAnt.get();
    }

    public void setOppAnt(boolean oppAnt) {
        this.oppAnt.set(oppAnt);
    }

    public boolean isTaboo() {
        return taboo.get();
    }

    public void setTaboo(boolean taboo) {
        this.taboo.set(taboo);
    }

    public boolean isMoved() {
        return moved.get();
    }

    public void setMoved(boolean moved) {
        this.moved.set(moved);
    }
}
