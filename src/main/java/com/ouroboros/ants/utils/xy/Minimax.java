package com.ouroboros.ants.utils.xy;

import com.google.common.collect.Lists;
import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.game.xy.XYTileMv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by zhanxies on 5/18/2018.
 *
 */
public class Minimax {

    private static final Logger LOGGER = LoggerFactory.getLogger(Minimax.class);

    private static class MMMove {
        XYTile origin;
        XYTile destination;
        Direction direction;
        MMMove lastMove;
        boolean myMove;
        int value;

        MMMove(XYTile origin, XYTile destination, Direction direction, MMMove lastMove, boolean myMove) {
            this.origin = origin;
            this.destination = destination;
            this.direction = direction;
            this.lastMove = lastMove;
            this.myMove = myMove;
        }
    }

    public static List<XYTileMv> minimax(Set<XYTile> myAnts, Set<XYTile> oppAnts) {
        LOGGER.info("minimax: {}", myAnts);

        List<XYTile> myAntsList = new ArrayList<>(myAnts);
        List<XYTile> oppAntsList = new ArrayList<>(oppAnts);

        List<MMMove> branches = max(myAntsList, oppAntsList, null);

        branches.parallelStream().forEach(b -> evaluation(b));

        List<XYTileMv> moves = new ArrayList<>(myAnts.size());
//        branches.parallelStream().max((b1, b2) -> {return b1.value - b2.value;}).ifPresent(b -> {
//            convertMoves(b, moves);
//        });

        return moves;
    }

    private static void convertMoves(MMMove move, List<XYTileMv> moves) {
        moves.add(new XYTileMv(move.origin, Direction.getOppoDir(move.direction)));
        if (move.lastMove != null) {
            convertMoves(move.lastMove, moves);
        }
    }

    private static List<MMMove> max(List<XYTile> myAnts, List<XYTile> oppAnts, MMMove myMove) {
        if (myAnts.isEmpty()) {
            return min(oppAnts, myMove);
        } else {
            XYTile a = myAnts.get(0);
            List<XYTile> nma = new ArrayList<>(myAnts.subList(1, myAnts.size()));
            return a.getNbDir().parallelStream().filter(nbt -> {
                return maxPrune(nbt, myMove);
            }).flatMap(nbt -> {
                MMMove nextMove = new MMMove(a, nbt.getTile(), nbt.getDir(), myMove, true);
                return max(nma, oppAnts, nextMove).stream();
            }).collect(Collectors.toList());
        }
    }

    private static List<MMMove> min(List<XYTile> oppAnts, MMMove myMove) {
        if (oppAnts.isEmpty()) {
            return Lists.newArrayList(myMove);
        } else {
            XYTile a = oppAnts.get(0);
            List<XYTile> noa = new ArrayList<>(oppAnts.subList(1, oppAnts.size()));
            return a.getNbDir().parallelStream().filter(nbt -> {
                return minPrune(nbt, myMove);
            }).flatMap(nbt -> {
                MMMove nextMove = new MMMove(a, nbt.getTile(), nbt.getDir(), myMove, false);
                return min(noa, nextMove).stream();
            }).collect(Collectors.toList());
        }
    }

    private static boolean maxPrune(XYTileMv nbt, MMMove move) {
        return true;
    }

    private static boolean minPrune(XYTileMv nbt, MMMove move) {
        return true;
    }

    private static int evaluation(MMMove myMove) {
        return 0;
    }
}
