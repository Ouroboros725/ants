package com.ouroboros.ants.strategy.xy;

import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.utils.xy.TreeSearch;

import java.util.List;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
public class XYOppStrategy {

    static void calcOppInfArea(List<Tile> oppAnts, int attackRadius2) {
        int dist = attackRadius2 + 1;

        oppAnts.parallelStream().map(XYTile::getTile)
                .forEach(tile -> tile.getNb().parallelStream()
                        .forEach(nbt -> TreeSearch.depthFirst(nbt, tile,
                        (t1, t2) -> !t1.getStatus().isTaboo() && t1.getDist(t2) < dist,
                        t -> t.getStatus().setTaboo(true))));
    }

//    static void attackHills(List<Tile> hills, int antsNum, Consumer<Move> output) {
//        int hillNum = hills.size();
//
//        if (hillNum > 0) {
//            int pAttNum = (int) (antsNum * 0.1);
//            int attPHill = pAttNum / hillNum;
//            int attNum = attPHill < 5 ? attPHill : 5;
//
//            for (Tile hill : hills) {
//                List<TileDir> moves = shallowDFSBack(hill, ants, movedAnts, blocks, str.xt, str.yt, str.attackHillRadius, attNum);
//                for (TileDir td : moves) {
//                    str.moveAnt(td, ants, movedAnts, output);
//                }
//            }
//        }
//
//
//        int hillNum = hills.size();
//
//        if (hillNum > 0) {
//            str.moveAnts((ants, movedAnts) -> {
//                int pAttNum = (int) (antsNum * 0.1);
//                int attPHill = pAttNum / hillNum;
//                int attNum = attPHill < 5 ? attPHill : 5;
//
//                for (Tile hill : hills) {
//                    List<TileDir> moves = shallowDFSBack(hill, ants, movedAnts, blocks, str.xt, str.yt, str.attackHillRadius, attNum);
//                    for (TileDir td : moves) {
//                        str.moveAnt(td, ants, movedAnts, output);
//                    }
//                }
//            });
//        }
//    }
}
