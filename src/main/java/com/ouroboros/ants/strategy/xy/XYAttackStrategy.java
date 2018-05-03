package com.ouroboros.ants.strategy.xy;

import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.game.xy.XYTileMove;
import com.ouroboros.ants.utils.Move;
import com.ouroboros.ants.utils.xy.TreeSearch;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
public class XYAttackStrategy {

    private static final int ATTACK_DIST = 3;
    private static final int HILL_RAID_DIST = 7;

    static void calcOppInfArea(List<Tile> oppAnts) {
        int dist = ATTACK_DIST;
        oppAnts.parallelStream().map(XYTile::getTile)
                .flatMap(tile -> tile.getNb().parallelStream())
                .forEach(nbt -> TreeSearch.depthFirstFill(nbt,
                        (t, l) -> !t.getStatus().isTaboo() || t.getStatus().getTabooDist() < dist,
                        l -> l < dist,
                        (t, l) -> {
                            t.getStatus().setTaboo(true);
                            t.getStatus().setTabooDist(l);
                        },
                        0));
    }

    static void attackHills(List<Tile> hills, int antsNum, BiConsumer<XYTileMove, Consumer<Move>> op, Consumer<Move> move) {
        int hillNum = hills.size();

        if (hillNum > 0) {
            int pAttNum = (int) (antsNum * 0.1);
            int attPHill = pAttNum / hillNum;
            int attNum = attPHill < 5 ? attPHill : 5;

            int dist = HILL_RAID_DIST;

            hills.parallelStream().map(XYTile::getTile)
                    .forEach(h -> {
                        Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>(129));
                        searched.add(h);
                        TreeSearch.breadthFirstMultiSearch(h.getNbDir(),
                                (t, l) -> {
                                    if (!searched.contains(t.getTile())) {
                                        searched.add(t.getTile());
                                        return !t.getTile().getStatus().isTaboo();
                                    }
                                    return false;
                                },
                                (l, c) -> l < dist && c.get() > 0,
                                (t, c) -> {
                                    if (t.getTile().getStatus().isMyAnt() && !t.getTile().getStatus().getMoved().getAndSet(true)) {
                                        op.accept(t, move);
                                        c.getAndDecrement();
                                    }
                                },
                                new AtomicInteger(attNum),
                                0
                        );
                    });
        }
    }
}
