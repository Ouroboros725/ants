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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by zhanxies on 5/18/2018.
 *
 */
public class Minimax {

    private static final Logger LOGGER = LoggerFactory.getLogger(Minimax.class);

    private static final int SIEGE_DIST2 = 5;
    private static final int BRANCH_THRESHOLD = 2999;

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

        @Override
        public String toString() {
            return "XYMove{" +
                    "destionation=" + destionation +
                    ", origin=" + origin +
                    '}';
        }
    }

    private static class MMMove {
        XYMove xyMove;
        Direction direction;
        MMMove lastMove;
        boolean myMove;
        Set<XYTile> destinations;
        Set<XYMove> moves;
        int myKilled;
        int oppKilled;
        int muDist;
        int killedDiff;

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

        @Override
        public String toString() {
            return "MMMove{" +
                    "myMove=" + myMove +
                    ", xyMove=" + xyMove +
                    ", lastMove=" + lastMove +
                    '}';
        }
    }

    private static class MMEva {
        int killDiffMin;
        int myKillMax;
        int oppKillMin;
        int distMax;

        @Override
        public String toString() {
            return "MMEva{" +
                    "distMax=" + distMax +
                    ", killDiffMin=" + killDiffMin +
                    ", myKillMax=" + myKillMax +
                    ", oppKillMin=" + oppKillMin +
                    '}';
        }
    }

    public static List<XYTileMv> minimax(Set<XYTile> myAnts, Set<XYTile> oppAnts, boolean aggressive) {
        LOGGER.info("minimax: {}", myAnts);

        List<XYTile> myAntsList = new ArrayList<>(myAnts);
        List<XYTile> oppAntsList = new ArrayList<>(oppAnts);

        Set<XYTile> myWarZone = underSiegeArea(oppAntsList);

        ConcurrentMap<MMMove, List<MMMove>> map = new ConcurrentHashMap<>();

        List<MMMove> branches = max(myAntsList, oppAntsList, null, myWarZone, map, new AtomicInteger(0));

        LOGGER.info("minimax branches: {} {} {}", branches.size(), myAnts.size(), oppAnts.size());

        branches.parallelStream().forEach(b -> calculateMove(b));

        List<XYTileMv> moves = evaluateMoves(map, aggressive);

        LOGGER.info("combat formation: {}, {}, {}", moves, myAntsList, oppAntsList);

        return moves;
    }

    private static void convertMoves(MMMove move, List<XYTileMv> moves) {
        if (move.myMove) {
            moves.add(new XYTileMv(move.xyMove.origin, move.direction != null ? Direction.getOppoDir(move.direction) : null));
        }
        if (move.lastMove != null) {
            convertMoves(move.lastMove, moves);
        }
    }

    private static void getDestAnts(MMMove move, List<XYTile> tiles, boolean myMove) {
        if (!(myMove ^ move.myMove)) {
            tiles.add(move.xyMove.destionation);
        }

        if (move.lastMove != null) {
            getDestAnts(move.lastMove, tiles, myMove);
        }
    }

    private static List<MMMove> max(List<XYTile> myAnts, List<XYTile> oppAnts, MMMove myMove, Set<XYTile> myWarZone,
                                                       ConcurrentMap<MMMove, List<MMMove>> map, AtomicInteger count) {
        if (myAnts.isEmpty()) {
            List<XYTile> destAnts = new ArrayList<>();
            getDestAnts(myMove, destAnts, true);
            Set<XYTile> oppWarZone = underSiegeArea(destAnts);
            List<MMMove> mm = min(oppAnts, myMove, oppWarZone, count);
            map.put(myMove, mm);
            return mm;

        } else {
            if (count.get() < BRANCH_THRESHOLD) {
                XYTile a = myAnts.get(0);
                List<XYTile> nma = new ArrayList<>(myAnts.subList(1, myAnts.size()));
                return generateMoves(a, myMove, true, myWarZone).parallelStream().flatMap(m -> {
                    return max(nma, oppAnts, m, myWarZone, map, count).stream();
                }).collect(Collectors.toList());
            } else {
                LOGGER.info("combat branches overwhelm max");
                return Collections.emptyList();
            }
        }
    }

    private static List<MMMove> min(List<XYTile> oppAnts, MMMove myMove, Set<XYTile> oppWarZone, AtomicInteger count) {
        if (oppAnts.isEmpty()) {
            count.incrementAndGet();
            return Lists.newArrayList(myMove);
        } else {
            if (count.get() < BRANCH_THRESHOLD) {
                XYTile a = oppAnts.get(0);
                List<XYTile> noa = new ArrayList<>(oppAnts.subList(1, oppAnts.size()));
                return generateMoves(a, myMove, false, oppWarZone).parallelStream().flatMap(m -> {
                    return min(noa, m, oppWarZone, count).stream();
                }).collect(Collectors.toList());
            } else {
                LOGGER.info("combat branches overwhelm min");
                return Collections.emptyList();
            }
        }
    }

    private static List<MMMove> generateMoves(XYTile tile, MMMove lastMove, boolean max, Set<XYTile> warZone) {
        List<MMMove> moves = new ArrayList<>(tile.getNbDir().size() + 1);

        boolean notOcc = true;
        if (lastMove == null || (notOcc = !lastMove.destinations.contains(tile))) {
            XYMove xyMove = new XYMove(tile, tile);
            MMMove move = new MMMove(xyMove, null, lastMove, max);
            moves.add(move);
        }

        boolean inWarZone = warZone.contains(tile);

        tile.getNbDir().parallelStream().filter(nbt -> {
            boolean s1 = lastMove == null || (!lastMove.destinations.contains(nbt.getTile()) &&
                    !lastMove.moves.contains(new XYMove(nbt.getTile(), tile)));
//            LOGGER.info("combat level 1 filter: {}", tile);
            return s1;
        }).filter(nbt -> {
            return max ? maxPrune(nbt, lastMove, warZone, inWarZone) : minPrune(nbt, lastMove, warZone, inWarZone);
        }).forEach(nbt -> {
            XYMove xyMove = new XYMove(tile, nbt.getTile());
            MMMove move = new MMMove(xyMove, nbt.getDir(), lastMove, max);
            moves.add(move);
        });

        if (moves.isEmpty() && !notOcc) {
            tile.getNbDir().parallelStream().filter(nbt -> {
                boolean s1 = lastMove == null || (!lastMove.destinations.contains(nbt.getTile()) &&
                        !lastMove.moves.contains(new XYMove(nbt.getTile(), tile)));
                return s1;
            }).findFirst().ifPresent(nbt -> {
                XYMove xyMove = new XYMove(tile, nbt.getTile());
                MMMove move = new MMMove(xyMove, nbt.getDir(), lastMove, max);
                moves.add(move);
            });
        }

        if (moves.isEmpty()) {
            LOGGER.info("combat no moves: {}", tile);
        }

        return moves;
    }

    private static Set<XYTile> underSiegeArea(List<XYTile> ants) {
        Set<XYTile> underSiege = Collections.newSetFromMap(new ConcurrentHashMap<>());

        int dist = SIEGE_DIST2;

        ants.parallelStream().forEach(tile -> {
            List<XYTile> centers = new ArrayList<>(tile.getNbDir().size() + 1);
            tile.getNbDir().stream().map(nbt -> nbt.getTile()).forEach(t -> {centers.add(t);});
            centers.add(tile);

            Map<XYTile, Integer> searched = new ConcurrentHashMap<>(25);

            TreeSearch.depthFirstFill(tile,
                    (t, l) -> {
                        Integer lv = searched.get(t);
                        if (lv == null || l < lv) {
                            searched.put(t, l);
                            return centers.parallelStream().anyMatch(c ->
                                    Utils.distEucl2(c.getX(), c.getY(), t.getX(), t.getY(), XYTile.getXt(), XYTile.getYt()) <= dist);
                        }

                        return false;
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
                LOGGER.info("combat filter not in max war zone: {}", nbt.getTile());
            }
            return inWarZone;
        }

        return true;
    }

    private static boolean minPrune(XYTileMv nbt, MMMove move, Set<XYTile> warZone, boolean currentInZone) {
        if (!currentInZone) {
            boolean inWarZone = warZone.contains(nbt.getTile());
            if (!inWarZone) {
                LOGGER.info("combat filter not in min war zone: {}", nbt.getTile());
            }
            return inWarZone;
        }

        return true;
    }

    private static void calculateMove(MMMove myMove) {
        List<XYTile> myAnts = new ArrayList<>();
        getDestAnts(myMove, myAnts, true);

        List<XYTile> oppAnts = new ArrayList<>();
        getDestAnts(myMove, oppAnts, false);

        if (oppAnts.isEmpty()) {
            LOGGER.info("combat branch invalid: {}", myMove);
            return;
        }

        AtomicInteger dist = new AtomicInteger();

        ConcurrentMap<XYTile, Set<XYTile>> enemyMap = new ConcurrentHashMap<>(myAnts.size() + oppAnts.size());
        myAnts.parallelStream().forEach(mt -> {
            AtomicInteger md = new AtomicInteger(Integer.MAX_VALUE);

            enemyMap.put(mt, oppAnts.parallelStream().filter(ot -> {
                int d = Utils.distEucl2(mt.getX(), mt.getY(), ot.getX(), ot.getY(), XYTile.getXt(), XYTile.getYt());
                md.updateAndGet(value -> value > d ? d : value);
                return d <= SIEGE_DIST2;
            }).collect(Collectors.toSet()));

            dist.addAndGet(md.get());
        });

        oppAnts.parallelStream().forEach(ot -> {
            enemyMap.put(ot, myAnts.parallelStream().filter(mt -> {
                return enemyMap.get(mt).contains(ot);
            }).collect(Collectors.toSet()));
        });

        AtomicInteger myKilled = new AtomicInteger(0);
        myAnts.parallelStream().forEach(mt -> {
            Set<XYTile> enemy = enemyMap.get(mt);
            int me = enemy.size();
            enemy.parallelStream().filter(ot -> enemyMap.get(ot).size() <= me).findAny().ifPresent(t -> myKilled.incrementAndGet());
        });

        AtomicInteger oppKilled = new AtomicInteger(0);
        oppAnts.parallelStream().forEach(ot -> {
            Set<XYTile> enemy = enemyMap.get(ot);
            int oe = enemy.size();
            enemy.parallelStream().filter(mt -> enemyMap.get(mt).size() <= oe).findAny().ifPresent(t -> oppKilled.incrementAndGet());
        });

        LOGGER.info("combat dist: {}", dist.get());

        myMove.myKilled = myKilled.get();
        myMove.oppKilled = oppKilled.get();
        myMove.muDist = dist.get();
        myMove.killedDiff = myMove.oppKilled - myMove.myKilled;
    }

    private static List<XYTileMv>  evaluateMoves(ConcurrentMap<MMMove, List<MMMove>> map, boolean aggressive) {
        List<XYTileMv> moves = new ArrayList<>();

        if (map.size() > 1) {
            ConcurrentMap<MMMove, MMEva> evaMap = new ConcurrentHashMap<>();

            map.entrySet().parallelStream().forEach(entry -> {
                MMEva eva = new MMEva();

                eva.killDiffMin = entry.getValue().parallelStream().mapToInt(m -> m.killedDiff).min().orElseGet(() -> Integer.MIN_VALUE);
                eva.myKillMax = entry.getValue().parallelStream().mapToInt(m -> m.myKilled).max().orElseGet(() -> Integer.MAX_VALUE);
                eva.oppKillMin = entry.getValue().parallelStream().mapToInt(m -> m.oppKilled).min().orElseGet(() -> Integer.MIN_VALUE);
                eva.distMax = entry.getValue().parallelStream().mapToInt(m -> m.muDist).max().orElseGet(() -> Integer.MAX_VALUE);

                evaMap.put(entry.getKey(), eva);
            });

            int maxKillDiff = evaMap.entrySet().parallelStream().mapToInt(entry -> entry.getValue().killDiffMin).max().orElseGet(() -> Integer.MIN_VALUE);
            List<Map.Entry<MMMove, MMEva>> maxKilledList = evaMap.entrySet().parallelStream().filter(entry -> entry.getValue().killDiffMin == maxKillDiff).collect(Collectors.toList());

            if (maxKilledList.size() > 1) {
                if (aggressive) {
                    int maxOppKill = maxKilledList.parallelStream().mapToInt(entry -> entry.getValue().oppKillMin).max().orElseGet(() -> Integer.MIN_VALUE);
                    List<Map.Entry<MMMove, MMEva>> maxOppKilledList = maxKilledList.parallelStream().filter(entry -> entry.getValue().oppKillMin == maxOppKill).collect(Collectors.toList());

                    if (maxOppKilledList.size() > 1) {
                        int minMyKill = maxOppKilledList.parallelStream().mapToInt(entry -> entry.getValue().myKillMax).min().orElseGet(() -> Integer.MAX_VALUE);
                        List<Map.Entry<MMMove, MMEva>> minMyKilledList = maxOppKilledList.parallelStream().filter(entry -> entry.getValue().myKillMax == minMyKill).collect(Collectors.toList());

                        if (minMyKilledList.size() > 1) {

                            int minDist = minMyKilledList.parallelStream().mapToInt(entry -> entry.getValue().distMax).min().orElseGet(() -> Integer.MAX_VALUE);
                            List<Map.Entry<MMMove, MMEva>> minDistList = minMyKilledList.parallelStream().filter(entry -> entry.getValue().distMax == minDist).collect(Collectors.toList());

                            if (minDistList.size() > 1) {
                                int index = ThreadLocalRandom.current().nextInt(minDistList.size());
                                convertMoves(minDistList.get(index).getKey(), moves);
                                LOGGER.info("combat evaluation max dist: {} {} {}", aggressive, minDistList.get(index).getKey(), minDistList.get(index).getValue());
                            } else if (minDistList.size() == 1) {
                                convertMoves(minDistList.get(0).getKey(), moves);
                                LOGGER.info("combat evaluation max dist: {} {} {}", aggressive, minDistList.get(0).getKey(), minDistList.get(0).getValue());
                            }
                        } else if (minMyKilledList.size() == 1) {
                            convertMoves(minMyKilledList.get(0).getKey(), moves);
                            LOGGER.info("combat evaluation max my: {} {} {}", aggressive, minMyKilledList.get(0).getKey(), minMyKilledList.get(0).getValue());
                        }

                    } else if (maxOppKilledList.size() == 1) {
                        convertMoves(maxOppKilledList.get(0).getKey(), moves);
                        LOGGER.info("combat evaluation max opp: {} {} {}", aggressive, maxOppKilledList.get(0).getKey(), maxOppKilledList.get(0).getValue());
                    }

                } else {
                    int minMyKill = maxKilledList.parallelStream().mapToInt(entry -> entry.getValue().myKillMax).min().orElseGet(() -> Integer.MAX_VALUE);
                    List<Map.Entry<MMMove, MMEva>> minMyKilledList = maxKilledList.parallelStream().filter(entry -> entry.getValue().myKillMax == minMyKill).collect(Collectors.toList());

                    if (minMyKilledList.size() > 1) {

                        int maxOppKill = minMyKilledList.parallelStream().mapToInt(entry -> entry.getValue().oppKillMin).max().orElseGet(() -> Integer.MIN_VALUE);
                        List<Map.Entry<MMMove, MMEva>> maxOppKilledList = minMyKilledList.parallelStream().filter(entry -> entry.getValue().oppKillMin == maxOppKill).collect(Collectors.toList());

                        if (maxOppKilledList.size() > 1) {
                            int minDist = maxOppKilledList.parallelStream().mapToInt(entry -> entry.getValue().distMax).min().orElseGet(() -> Integer.MAX_VALUE);
                            List<Map.Entry<MMMove, MMEva>> minDistList = maxOppKilledList.parallelStream().filter(entry -> entry.getValue().distMax == minDist).collect(Collectors.toList());

                            if (minDistList.size() > 1) {
                                int index = ThreadLocalRandom.current().nextInt(minDistList.size());
                                convertMoves(minDistList.get(index).getKey(), moves);
                                LOGGER.info("combat evaluation max dist: {} {} {}", aggressive, minDistList.get(index).getKey(), minDistList.get(index).getValue());
                            } else if (minDistList.size() == 1) {
                                convertMoves(minDistList.get(0).getKey(), moves);
                                LOGGER.info("combat evaluation max dist: {} {} {}", aggressive, minDistList.get(0).getKey(), minDistList.get(0).getValue());
                            }
                        } else if (maxOppKilledList.size() == 1) {
                            convertMoves(maxOppKilledList.get(0).getKey(), moves);
                            LOGGER.info("combat evaluation max opp: {} {} {}", aggressive, maxOppKilledList.get(0).getKey(), maxOppKilledList.get(0).getValue());
                        }

                    } else if (minMyKilledList.size() == 1) {
                        convertMoves(minMyKilledList.get(0).getKey(), moves);
                        LOGGER.info("combat evaluation max my: {} {} {}", aggressive, minMyKilledList.get(0).getKey(), minMyKilledList.get(0).getValue());
                    }
                }
            } else if (maxKilledList.size() == 1) {
                convertMoves(maxKilledList.get(0).getKey(), moves);
                LOGGER.info("combat evaluation max diff: {} {} {}", aggressive, maxKilledList.get(0).getKey(), maxKilledList.get(0).getValue());
            }
        } else if (map.size() == 1) {
            convertMoves(map.entrySet().iterator().next().getKey(), moves);
            LOGGER.info("combat evaluation single: {} {} {}", aggressive, map.entrySet().iterator().next().getKey(), map.entrySet().iterator().next().getValue());
        }

        return moves;
    }

}
