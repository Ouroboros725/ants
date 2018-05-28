package com.ouroboros.ants.game.xy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
public class XYTileStatus {
    private AtomicBoolean myAnt;
    private AtomicBoolean oppAnt;
    private AtomicBoolean taboo;
    private AtomicInteger tabooDist;
    private AtomicBoolean moved;
    private AtomicInteger enemyCnt;
    private AtomicInteger attackInf;

    XYTileStatus() {
        myAnt = new AtomicBoolean();
        oppAnt = new AtomicBoolean();
        taboo = new AtomicBoolean();
        tabooDist = new AtomicInteger();
        moved = new AtomicBoolean();
        enemyCnt = new AtomicInteger();
        attackInf = new AtomicInteger();
    }

    void reset() {
        myAnt.set(false);
        oppAnt.set(false);
        taboo.set(false);
        tabooDist.set(0);
        moved.set(false);
        enemyCnt.set(0);
        attackInf.set(0);
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

    public int getTabooDist() {
        return tabooDist.get();
    }

    public void setTabooDist(int dist) {
        tabooDist.set(dist);
    }

    public boolean isMoved() {
        return moved.get();
    }

    public void setMoved(boolean moved) {
        this.moved.set(moved);
    }

    public AtomicBoolean getMoved() {
        return moved;
    }

    public int getEnemyCnt() {
        return enemyCnt.get();
    }

    public int incEnemyCnt() {
        return enemyCnt.incrementAndGet();
    }

    public int getAttackInf() {
        return attackInf.get();
    }

    public void setAttackInf(int attackInf) {
        this.attackInf.set(attackInf);
    }
}
