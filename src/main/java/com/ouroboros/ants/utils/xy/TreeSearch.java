package com.ouroboros.ants.utils.xy;

import com.ouroboros.ants.game.xy.XYTile;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
public class TreeSearch {

    public static void depthFirst(XYTile tile, XYTile orgTile, BiPredicate<XYTile, XYTile> visit, Consumer<XYTile> update) {
        if (visit.test(tile, orgTile)) {
            update.accept(tile);
            tile.getNb().parallelStream().forEach(t -> depthFirst(t, orgTile, visit, update));
        }
    }

//    public static void depthFirst(List<XYTile> tiles, XYTile orgTile, BiPredicate<XYTile, XYTile> visit, ) {
//        tiles.parallelStream();
//
//    }
}
