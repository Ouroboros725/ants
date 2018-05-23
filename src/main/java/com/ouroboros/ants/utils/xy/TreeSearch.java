package com.ouroboros.ants.utils.xy;

import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.game.xy.XYTileMv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(TreeSearch.class);

    public static void depthFirstFill(XYTile tile, BiPredicate<XYTile, Integer> visit,
                                      Predicate<Integer> cont, BiConsumer<XYTile, Integer> update, Integer level) {
        if (visit.test(tile, level)) {
            update.accept(tile, level);

            if (cont.test(level)) {
                tile.getNb().parallelStream().forEach(t -> depthFirstFill(t, visit, cont, update, level + 1));
            }
        }
    }

    public static void breadthFirstMultiSearch(Collection<XYTileMv> tiles, BiPredicate<XYTileMv, Integer> visit,
                                               BiPredicate<Integer, AtomicInteger> cont, BiConsumer<XYTileMv, AtomicInteger> update,
                                               AtomicInteger cnt, Integer level) {
        List<XYTileMv> rt = tiles.parallelStream().filter(t -> visit.test(t, level)).collect(Collectors.toList());
        if (!rt.isEmpty()) {
            rt.parallelStream().forEach(t -> update.accept(t, cnt));

            if (cont.test(level, cnt)) {
                breadthFirstMultiSearch(rt.parallelStream().flatMap(t -> t.getTile().getNbDir().stream()).collect(Collectors.toList()),
                        visit, cont, update, cnt, level + 1);
            }
        }

    }

    public static XYTileMv breadthFirstLink(Map<XYTileMv, XYTileMv> start, XYTile goal, BiPredicate<XYTile, Integer> visit, List<XYTileMv> links, int level) {
        List<Map.Entry<XYTileMv, XYTileMv>> targets = start.entrySet().parallelStream()
                .filter(entry -> visit.test(entry.getKey().getTile(), level)).collect(Collectors.toList());

        AtomicBoolean find = new AtomicBoolean(false);
        targets.parallelStream().filter(entry -> entry.getKey().getTile().equals(goal)).findAny().ifPresent(entry -> {
            links.add(entry.getKey());
            find.set(true);
        });

        if (find.get()) {
            return start.get(links.get(0));
        } else {
            Map<XYTileMv, XYTileMv> ns = new ConcurrentHashMap<>();
            targets.parallelStream().forEach(entry -> {
                XYTileMv mv = entry.getKey();
                mv.getTile().getNbDir().parallelStream().forEach(nbt -> ns.put(nbt, mv));
            });

            XYTileMv nx = breadthFirstLink(ns, goal, visit, links, level + 1);
            links.add(nx);

            return start.get(nx);
        }
    }
}
