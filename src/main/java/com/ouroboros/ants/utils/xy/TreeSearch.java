package com.ouroboros.ants.utils.xy;

import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.game.xy.XYTileMove;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
public class TreeSearch {

    public static void depthFirstFill(XYTile tile, BiPredicate<XYTile, Integer> visit,
                                      Predicate<Integer> cont, BiConsumer<XYTile, Integer> update, Integer level) {
        if (visit.test(tile, level)) {
            update.accept(tile, level);

            if (cont.test(level)) {
                tile.getNb().parallelStream().forEach(t -> depthFirstFill(t, visit, cont, update, level + 1));
            }
        }
    }

    public static void breadthFirstMultiSearch(Collection<XYTileMove> tiles, BiPredicate<XYTileMove, Integer> visit,
                                               BiPredicate<Integer, AtomicInteger> cont, BiConsumer<XYTileMove, AtomicInteger> update,
                                               AtomicInteger cnt, Integer level) {
        List<XYTileMove> rt = tiles.parallelStream().filter(t -> visit.test(t, level)).collect(Collectors.toList());
        if (!rt.isEmpty()) {
            rt.parallelStream().forEach(t -> update.accept(t, cnt));

            if (cont.test(level, cnt)) {
                breadthFirstMultiSearch(rt.parallelStream().flatMap(t -> t.getTile().getNbDir().stream()).collect(Collectors.toList()),
                        visit, cont, update, cnt, level + 1);
            }
        }

    }
}
