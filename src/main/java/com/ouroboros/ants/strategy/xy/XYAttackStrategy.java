package com.ouroboros.ants.strategy.xy;

import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.game.xy.XYTileMv;
import com.ouroboros.ants.game.xy.XYTileMvAggWt;
import com.ouroboros.ants.game.xy.XYTileWt;
import com.ouroboros.ants.utils.Move;
import com.ouroboros.ants.utils.Utils;
import com.ouroboros.ants.utils.xy.Minimax;
import com.ouroboros.ants.utils.xy.TreeSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by zhanxies on 4/27/2018.
 *
 */
public class XYAttackStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(XYAttackStrategy.class);

    private static Map<XYTile, XYExploreTask> exploreTaskLink = new ConcurrentHashMap<>();

    private static final int ATTACK_DIST = 3;
    private static final int HILL_RAID_DIST = 10;
    private static final int EXPLORE_DIST = 10;

    private static final int ENEMY_DIST = 4;
    private static final int ALLY_DIST = 3;

    private static final int MAX_FIGHT = 10;
    private static final int MAX_ALLY = 50;
    private static final int SUB_DIST = 15;

    private static final int FIGHT_PRESS_THRE = 7;
    private static final int NOT_FIGHT = 2;
    private static final int HILL_ATTACK_DIST = 12;


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

    static void attackHills(List<Tile> hills, int antsNum, BiFunction<XYTileMv, Consumer<Move>, Boolean> op, Consumer<Move> move) {
        int hillNum = hills.size();

        if (hillNum > 0) {
            int pAttNum = (int) (antsNum * 0.1);
            int attPHill = pAttNum / hillNum;
            int attNum = attPHill < 5 ? attPHill : 5;

            int dist = HILL_RAID_DIST;
            int size = Utils.searchSize(dist);

            hills.parallelStream().map(XYTile::getTile)
                    .forEach(h -> {
                        Map<XYTile, Integer> searched = new ConcurrentHashMap<>(size);
                        searched.put(h, -1);
                        TreeSearch.breadthFirstMultiSearch(h.getNbDir(),
                                (t, l) -> {
                                    Integer lv = searched.get(t.getTile());
                                    if (lv == null || l < lv) {
                                        searched.put(t.getTile(), l);
                                        return !t.getTile().getStatus().isTaboo();
                                    }

                                    return false;
                                },
                                (l, c) -> l < dist && c.get() > 0,
                                (t, c) -> {
                                    if (t.getTile().getStatus().isMyAnt() && !t.getTile().getStatus().getMoved().getAndSet(true)) {
                                        if (op.apply(t, move)) {
                                            c.getAndDecrement();
                                        }
                                    }
                                },
                                new AtomicInteger(attNum),
                                0
                        );
                    });
        }
    }

    static void explore(List<Tile> myAnts, BiFunction<XYTileMv, Consumer<Move>, Boolean> op, Consumer<Move> move) {
        XYTile.updateVisitInfluence();

        int dist = EXPLORE_DIST;
        int size = Utils.searchSize(dist);

        {
            exploreTaskLink.entrySet().removeIf(entry -> {
                XYTile tile = entry.getKey();
                return !tile.getStatus().isMyAnt() || tile.getStatus().isMoved();
            });

            Set<XYTile> et = new HashSet<>(exploreTaskLink.keySet());

            et.parallelStream().forEach(tile -> {
                XYExploreTask tg = exploreTaskLink.remove(tile);
                exploreMove(tile, tg, op, move);
            });
        }

        Set<XYTile> updated = Collections.newSetFromMap(new ConcurrentHashMap<>());
        myAnts.parallelStream().map(XYTile::getTile).forEach(tile -> {
            Map<XYTile, Integer> searched = new ConcurrentHashMap<>(size);
            TreeSearch.depthFirstFill(tile,
                    (t, l) -> {
                        Integer lv = searched.get(t);
                        if (lv == null || l < lv) {
                            searched.put(t, l);
                            return true;
                        }

                        return false;
                    },
                    l -> l < dist,
                    (t, l) -> {
                        if (!updated.contains(t)) {
                            updated.add(t);
                            t.getVisit().setInfluence(0);
                        }
                    },
                    0);
        });

        Set<XYTile> border = Collections.newSetFromMap(new ConcurrentHashMap<>(myAnts.size()));

        LOGGER.info("my explore ants: {}", myAnts);

        myAnts.parallelStream().map(XYTile::getTile).forEach(tile -> {
            LOGGER.info("explore ant: {}", tile);

            boolean toExplore = tile.getStatus().isMyAnt() && !tile.getStatus().isMoved();

            List<XYTileMvAggWt> wList = tile.getNbDir().stream().map(XYTileMvAggWt::new).collect(Collectors.toList());

            Map<XYTile, Integer> searched = new ConcurrentHashMap<>(size);
            wList.parallelStream().filter(nbt -> !nbt.getMove().getTile().getStatus().isTaboo() && !nbt.getMove().getTile().getStatus().isMyAnt())
                    .forEach(nbt -> TreeSearch.depthFirstFill(nbt.getMove().getTile(),
                            (t, l) -> {
                                Integer lv = searched.get(t);
                                if (lv == null || l < lv) {
                                    searched.put(t, l);
                                    return true;
                                }

                                return false;
                            },
                            l -> l < dist,
                            (t, l) -> {
                                int w = t.getVisit().getInfluence().get();
                                if (w > 0) {
                                    border.add(t);
                                    if (toExplore) {
                                        nbt.getWeight().set(nbt.getWeight().get() + w);
                                    }
                                }
                            },
                            0));

            if (toExplore) {
                Optional<XYTileMvAggWt> rw = wList.stream().filter(t -> t.getWeight().get() > 0).max((t1, t2) -> {
                    int weight = t1.getWeight().get() - t2.getWeight().get();
                    return weight != 0 ? weight : ThreadLocalRandom.current().nextInt(2) - 1;
                });

                rw.ifPresent(t -> {
                    LOGGER.info("imm explore: {}", tile);
                    op.apply(new XYTileMv(tile, Direction.getOppoDir(t.getMove().getDir())), move);
                });
            }
        });

        LOGGER.info("border: {}", border);

        if (!border.isEmpty()) {
            myAnts.parallelStream().map(XYTile::getTile).filter(t -> t.getStatus().isMyAnt() && !t.getStatus().isMoved()).forEach(tile -> {
                Optional<XYTileWt> tw = border.parallelStream().map(b -> new XYTileWt(b, Utils.distManh(b.getX(), b.getY(), tile.getX(), tile.getY(), XYTile.getXt(), XYTile.getYt()))).max(
                        (t1, t2) -> {
                            int di = t1.getTile().getVisit().getInfluence().get() - t2.getTile().getVisit().getInfluence().get();
                            if (di == 0) {
                                int dw = t2.getWeight() - t1.getWeight();
                                return dw != 0 ? dw : ThreadLocalRandom.current().nextInt(2) - 1;
                            } else {
                                return di;
                            }
                        });

                LOGGER.info("border explore: {}", tile);
                LOGGER.info("border nb of {}: {}", tile, tile.getNbDir());

                tw.ifPresent(tg -> {
                    border.remove(tg.getTile());

                    XYExploreTask task = new XYExploreTask(tile, tg.getTile());
                    exploreMove(tile, task, op, move);
                });
            });
        }
    }

    private static void exploreMove(XYTile tile, XYExploreTask task, BiFunction<XYTileMv, Consumer<Move>, Boolean> op, Consumer<Move> move) {
        XYTileMv mv = task.getMove();
        XYTile ct = XYTile.getTile(Direction.getOppoDir(mv.getDir()).getNeighbour(tile.getX(), tile.getY(), XYTile.getXt(), XYTile.getYt()));
        if (!ct.getStatus().isTaboo() && !ct.getStatus().isMyAnt()) {
            op.apply(mv, move);
            task.move();
            if (!task.end()) {
                exploreTaskLink.put(ct, task);
            }
        } else {
            exploreTaskLink.put(tile, task);
        }
    }

    public static void attackEnemy(List<Tile> myAnts, List<Tile> myHills, BiFunction<XYTileMv,
            Consumer<Move>, Boolean> op, Consumer<Move> move, AtomicBoolean terminator) {
        int lDist = ALLY_DIST;
        int lSize = Utils.searchSize(lDist);

        Map<XYTile, Set<XYTile>> allyAnts = new ConcurrentHashMap<>();
        Set<XYTile> myFight = Collections.newSetFromMap(new ConcurrentHashMap<>());

        myAnts.parallelStream().map(XYTile::getTile).forEach(tile -> {
            Map<XYTile, Integer> searched = new ConcurrentHashMap<>(lSize);
            Set<XYTile> ally = Collections.<XYTile>newSetFromMap(new ConcurrentHashMap<>());
            allyAnts.put(tile, ally);

            TreeSearch.depthFirstFill(tile,
                    (t, l) -> {
                        Integer lv = searched.get(t);
                        if (lv == null || l < lv) {
                            searched.put(t, l);
                            return true;
                        }

                        return false;
                    },
                    l -> l < lDist,
                    (t, l) -> {
//                        LOGGER.info("combat ally: {} {} {} {}", tile, t, t.getStatus().isMyAnt(), t.getStatus().isMoved());
                        if (t.getStatus().isMyAnt() && !t.getStatus().isMoved()) {
                            ally.add(t);
                            myFight.add(t);
                        }
                    },
                    0);
        });

        int eDist = ENEMY_DIST;
        int eSize = Utils.searchSize(eDist);

        Map<XYTile, Set<XYTile>> enemyAnts = new ConcurrentHashMap<>();
        Set<XYTile> oppFight = Collections.newSetFromMap(new ConcurrentHashMap<>());

        myAnts.parallelStream().map(XYTile::getTile).forEach(tile -> {
            Map<XYTile, Integer> searched = new ConcurrentHashMap<>(eSize);
            Set<XYTile> enemy = Collections.<XYTile>newSetFromMap(new ConcurrentHashMap<>());
            enemyAnts.put(tile, enemy);

            TreeSearch.depthFirstFill(tile,
                    (t, l) -> {
                        Integer lv = searched.get(t);
                        if (lv == null || l < lv) {
                            searched.put(t, l);
                            return true;
                        }

                        return false;
                    },
                    l -> l < eDist,
                    (t, l) -> {
                        if (t.getStatus().isOppAnt()) {
                            tile.getStatus().incEnemyCnt();
                            enemy.add(t);
                            oppFight.add(t);
                        }
                    },
                    0);
        });


        myAnts.parallelStream().map(XYTile::getTile).forEach(tile -> {
            int enemyCnt = tile.getStatus().getEnemyCnt();
            if (enemyCnt > 0) {
                int hillDist = myHills.parallelStream().map(XYTile::getTile).mapToInt(h -> Utils.distManh(tile.getX(), tile.getY(), h.getX(), h.getY(), XYTile.getXt(), XYTile.getYt())).min().orElseGet(() -> 0);
                int inf = (hillDist <= HILL_ATTACK_DIST && hillDist > 0) ? enemyCnt + (HILL_ATTACK_DIST - hillDist) : enemyCnt;
                tile.getStatus().setAttackInf(inf);
            }
        });

        List<XYTile> toFight = myAnts.parallelStream().map(XYTile::getTile)
                .filter(t -> t.getStatus().getEnemyCnt() >= 1 && !allyAnts.get(t).isEmpty())
                .sorted((t1, t2) -> {
                    return t2.getStatus().getAttackInf() - t1.getStatus().getAttackInf();
                }).limit(MAX_FIGHT).collect(Collectors.toList());

        Set<XYTile> picked = Collections.<XYTile>newSetFromMap(new ConcurrentHashMap());
        Set<XYTile> dupAnts = Collections.<XYTile>newSetFromMap(new ConcurrentHashMap());

        toFight.stream().forEachOrdered(tile -> {
            if (picked.add(tile)) {
                Set<XYTile> enemy = enemyAnts.get(tile);
                Set<XYTile> ally = allyAnts.get(tile);

                Set<XYTile> newAlly = Collections.newSetFromMap(new ConcurrentHashMap());
                newAlly.addAll(allyAnts.get(tile));

                do {
                    List<XYTile> allyList = new ArrayList<>(newAlly);
                    newAlly.clear();
                    allyList.parallelStream().filter(t -> picked.add(t))
                            .forEach(t -> {
//                                LOGGER.info("merge hunt ants: {} {}", t, allyAnts.get(t));
                                enemy.addAll(enemyAnts.get(t));
                                ally.addAll(allyAnts.get(t));
                                newAlly.add(t);
                                dupAnts.add(t);
                            });
                } while (!newAlly.isEmpty() && ally.size() < MAX_ALLY);
            }
        });

        toFight.removeAll(dupAnts);

//        toFight.parallelStream().forEach(tile -> {
//            Set<XYTile> ma = allyAnts.get(tile);
//            if (ma.size() > MAX_ALLY) {
//                List<XYTile> nma = ma.parallelStream().filter(al -> Utils.distManh(tile.getX(), tile.getY(), al.getX(), al.getY(), XYTile.getXt(), XYTile.getYt()) > 4).collect(Collectors.toList());
//                ma.removeAll(nma);
//            }
//        });

        if (!toFight.isEmpty()) {
            for (int i = 0; i < toFight.size(); i++) {
                LOGGER.info("enemies to hunt 2: {}", i);
                LOGGER.info("enemies to hunt ant: {}", toFight.get(i));
                LOGGER.info("enemies to hunt team: {}", allyAnts.get(toFight.get(i)));
                LOGGER.info("enemies to hunt opp: {}", enemyAnts.get(toFight.get(i)));
            }
        }

        AtomicInteger sBat = new AtomicInteger(0);
        AtomicInteger lBat = new AtomicInteger(0);

        toFight.parallelStream().forEach(t -> {
            if ((allyAnts.get(t).size() + enemyAnts.get(t).size()) <= FIGHT_PRESS_THRE) {
                sBat.incrementAndGet();
            } else {
                lBat.incrementAndGet();
            }
        });

        boolean aggressive = myFight.size() - oppFight.size() > 5;
        boolean reduced = lBat.get() > (MAX_FIGHT / 2);

        int sgDist = ENEMY_DIST;
        int sgSize = Utils.searchSize(sgDist);

        toFight.parallelStream().forEachOrdered(tile -> {
            Set<XYTile> ma = allyAnts.get(tile);
            Set<XYTile> oa = enemyAnts.get(tile);

            if (ma.isEmpty()) {
                LOGGER.info("invalid combat: {}", tile);
            }

            if (oa.size() < NOT_FIGHT && (ma.size() >= NOT_FIGHT * 2)) {
                oa.parallelStream().forEach(ot -> {
                    Map<XYTile, Integer> searched = new ConcurrentHashMap<>(sgSize);
                    searched.put(ot, -1);
                    TreeSearch.breadthFirstMultiSearch(ot.getNbDir(),
                            (t, l) -> {
                                Integer lv = searched.get(t.getTile());
                                if (lv == null || l < lv) {
                                    searched.put(t.getTile(), l);
                                    return true;
                                }

                                return false;
                            },
                            (l, c) -> l < sgDist && c.get() > 0,
                            (t, c) -> {
                                if (t.getTile().getStatus().isMyAnt() && !t.getTile().getStatus().getMoved().getAndSet(true)) {
                                    if (op.apply(t, move)) {
                                        LOGGER.info("combat lost enemy: {} {}", ot, t);
                                        c.getAndDecrement();
                                    }
                                }
                            },
                            new AtomicInteger(oa.size()),
                            0
                    );
                });
            } else {
                if (!ma.isEmpty() && !oa.isEmpty() && !terminator.get()) {
                    Minimax.minimax(ma, oa, aggressive, reduced, terminator)
                            .parallelStream().forEach(mv -> op.apply(mv, move));
                }
            }
        });

        if (terminator.get()) {
            return;
        }

        int sDist = SUB_DIST;
        int sSize = Utils.searchSize(sDist);

        toFight.parallelStream().forEach(tile -> {
            Set<XYTile> ma = allyAnts.get(tile);
            Set<XYTile> oa = enemyAnts.get(tile);

            int diff = oa.size() - ma.size();

//            if (diff > -3 && (ma.size() <= MAX_ALLY + 3)) {
                diff = diff < 0 ? 2 : (diff + 3);

                Map<XYTile, Integer> searched = new ConcurrentHashMap<>(sSize);
                searched.put(tile, -1);
                TreeSearch.breadthFirstMultiSearch(tile.getNbDir(),
                        (t, l) -> {
                            Integer lv = searched.get(t.getTile());
                            if (lv == null || l < lv) {
                                searched.put(t.getTile(), l);
                                return !t.getTile().getStatus().isTaboo();
                            }

                            return false;
                        },
                        (l, c) -> l < sDist && c.get() > 0,
                        (t, c) -> {
                            if (t.getTile().getStatus().isMyAnt() && !t.getTile().getStatus().getMoved().getAndSet(true)) {
                                if (op.apply(t, move)) {
                                    LOGGER.info("combat reinforcement: {} {}", tile, t);
                                    c.getAndDecrement();
                                }
                            }
                        },
                        new AtomicInteger(diff),
                        0
                );
//            }
        });
    }


}
