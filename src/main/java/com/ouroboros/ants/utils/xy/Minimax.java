package com.ouroboros.ants.utils.xy;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.game.xy.XYTileMv;
import com.ouroboros.ants.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by zhanxies on 5/18/2018.
 *
 */
public class Minimax {

    private static final Logger LOGGER = LoggerFactory.getLogger(Minimax.class);

    private static final int SIEGE_DIST2 = 5;

    private static class XYMove {
        XYTile origin;
        XYTile destionation;

        XYMove(XYTile origin, XYTile destionation) {
            this.origin = origin;
            this.destionation = destionation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            XYMove xyMove = (XYMove) o;
            return Objects.equal(origin, xyMove.origin) &&
                    Objects.equal(destionation, xyMove.destionation);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(origin, destionation);
        }
    }

    private static class MMMove {
        XYMove xyMove;
        Direction direction;
        MMMove lastMove;
        boolean myMove;
        int value;
        Set<XYTile> destinations;
        Set<XYMove> moves;

        MMMove(XYMove xyMove, Direction direction, MMMove lastMove, boolean myMove) {
            this.xyMove = xyMove;
            this.direction = direction;
            this.lastMove = lastMove;
            this.myMove = myMove;

            this.destinations = new HashSet<>(lastMove != null ? (lastMove.destinations.size() + 1) : 1);
            if (lastMove != null) {
                this.destinations.addAll(lastMove.destinations);
            }
            this.destinations.add(this.xyMove.destionation);

            this.moves = new HashSet<>(lastMove != null ? (lastMove.moves.size() + 1) : 1);
            if (lastMove != null) {
                this.moves.addAll(lastMove.moves);
            }
            this.moves.add(this.xyMove);
        }
    }

    public static List<XYTileMv> minimax(Set<XYTile> myAnts, Set<XYTile> oppAnts) {
        LOGGER.info("minimax: {}", myAnts);

        List<XYTile> myAntsList = new ArrayList<>(myAnts);
        List<XYTile> oppAntsList = new ArrayList<>(oppAnts);

        Set<XYTile> myWarZone = underSiegeArea(oppAntsList);

        List<MMMove> branches = max(myAntsList, oppAntsList, null, myWarZone);

        LOGGER.info("minimax branches: {} {} {}", branches.size(), myAnts.size(), oppAnts.size());

        branches.parallelStream().forEach(b -> evaluation(b));

        List<XYTileMv> moves = new ArrayList<>(myAnts.size());
//        branches.parallelStream().max((b1, b2) -> {return b1.value - b2.value;}).ifPresent(b -> {
//            convertMoves(b, moves);
//        });

        return moves;
    }

    private static void convertMoves(MMMove move, List<XYTileMv> moves) {
        if (move.myMove && move.direction != null) {
            moves.add(new XYTileMv(move.xyMove.origin, Direction.getOppoDir(move.direction)));
        }
        if (move.lastMove != null) {
            convertMoves(move.lastMove, moves);
        }
    }

    private static List<MMMove> max(List<XYTile> myAnts, List<XYTile> oppAnts, MMMove myMove, Set<XYTile> myWarZone) {
        if (myAnts.isEmpty()) {
            return min(oppAnts, myMove, myWarZone);
        } else {
            XYTile a = myAnts.get(0);
            List<XYTile> nma = new ArrayList<>(myAnts.subList(1, myAnts.size()));
            return generateMoves(a, myMove, true, myWarZone).parallelStream().flatMap(m -> {
                return max(nma, oppAnts, m, myWarZone).stream();
            }).collect(Collectors.toList());
        }
    }

    private static List<MMMove> min(List<XYTile> oppAnts, MMMove myMove, Set<XYTile> myWarZone) {
        if (oppAnts.isEmpty()) {
            return Lists.newArrayList(myMove);
        } else {
            XYTile a = oppAnts.get(0);
            List<XYTile> noa = new ArrayList<>(oppAnts.subList(1, oppAnts.size()));
            return generateMoves(a, myMove, false, myWarZone).parallelStream().flatMap(m -> {
                return min(noa, m, myWarZone).stream();
            }).collect(Collectors.toList());
        }
    }

    private static List<MMMove> generateMoves(XYTile tile, MMMove lastMove, boolean max, Set<XYTile> myWarZone) {
        List<MMMove> moves = new ArrayList<>(tile.getNbDir().size() + 1);

        if (lastMove == null || !lastMove.destinations.contains(tile)) {
            XYMove xyMove = new XYMove(tile, tile);
            MMMove move = new MMMove(xyMove, null, lastMove, true);
            moves.add(move);
        }

        boolean inWarZone = myWarZone.contains(tile);

        tile.getNbDir().parallelStream().filter(nbt -> {
            boolean s1 = lastMove == null || (!lastMove.destinations.contains(nbt.getTile()) &&
                    !lastMove.moves.contains(new XYMove(nbt.getTile(), tile)));
//            LOGGER.info("combat level 1 filter: {}", tile);
            return s1;
        }).filter(nbt -> {
            return max ? maxPrune(nbt, lastMove, myWarZone, inWarZone) : minPrune(nbt, lastMove);
        }).forEach(nbt -> {
            XYMove xyMove = new XYMove(tile, nbt.getTile());
            MMMove move = new MMMove(xyMove, nbt.getDir(), lastMove, max);
            moves.add(move);
        });

        return moves;
    }

    private static Set<XYTile> underSiegeArea(List<XYTile> ants) {
        Set<XYTile> underSiege = Collections.newSetFromMap(new ConcurrentHashMap<>());

        int dist = SIEGE_DIST2;

        ants.parallelStream().forEach(tile -> {
            List<XYTile> centers = new ArrayList<>(tile.getNbDir().size() + 1);
            tile.getNbDir().stream().map(nbt -> nbt.getTile()).forEach(t -> {centers.add(t);});
            centers.add(tile);

            Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>(25));

            TreeSearch.depthFirstFill(tile,
                    (t, l) -> {
                        return searched.add(t)
                                && centers.parallelStream().anyMatch(c ->
                                Utils.distEucl2(c.getX(), c.getY(), t.getX(), t.getY(), XYTile.getXt(), XYTile.getYt()) < dist);
                    },
                    l -> true,
                    (t, l) -> {
                        underSiege.add(t);
                    },
                    0);
        });

        return underSiege;
    }

    private static boolean maxPrune(XYTileMv nbt, MMMove move, Set<XYTile> warZone, boolean currentInZone) {
        if (!currentInZone) {
            boolean inWarZone = warZone.contains(nbt.getTile());
            if (!inWarZone) {
                LOGGER.info("combat filter not in war zone: {}", nbt.getTile());
            }
            return inWarZone;
        }

        return true;
    }

    private static boolean minPrune(XYTileMv nbt, MMMove move) {
        return true;
    }

    private static int evaluation(MMMove myMove) {
        return 0;
    }
}
