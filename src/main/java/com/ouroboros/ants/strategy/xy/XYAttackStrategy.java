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
import com.ouroboros.ants.utils.xy.XYUtils;
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
    private static final int ENEMY_DIST = 5;
    private static final int COMBAT_DIST = 3;
    private static final int ANTI_DIST = 5;
    private static final int ALLY_DIST = 15;


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

    public static void attackEnemy(List<Tile> oppAnts, BiFunction<XYTileMv, Consumer<Move>, Boolean> op, Consumer<Move> move, AtomicBoolean terminator) {
        int dist = ENEMY_DIST;
        int size = Utils.searchSize(dist);

        Map<XYTile, Set<XYTile>> myAnts = new ConcurrentHashMap<>();
        Map<XYTile, Set<XYTile>> enemyAnts = new ConcurrentHashMap<>();

        oppAnts.parallelStream().map(XYTile::getTile).forEach(tile -> {
            Map<XYTile, Integer> searched = new ConcurrentHashMap<>(size);
            myAnts.put(tile, Collections.<XYTile>newSetFromMap(new ConcurrentHashMap<>()));
            enemyAnts.put(tile, Collections.<XYTile>newSetFromMap(new ConcurrentHashMap<>()));

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
                        if (t.getStatus().isMyAnt() && !t.getStatus().isMoved()) {
                            myAnts.get(tile).add(t);
                        } else if (t.getStatus().isOppAnt()) {
                            tile.getStatus().incEnemyCnt();
                            enemyAnts.get(tile).add(t);
                        }
                    },
                    0);
        });

        List<XYTile> toFight = oppAnts.parallelStream().map(XYTile::getTile)
                .filter(t -> t.getStatus().getEnemyCnt() > 1)
                .sorted((t1, t2) -> {
                    return t2.getStatus().getEnemyCnt() - t1.getStatus().getEnemyCnt();
                }).collect(Collectors.toList());

        Set<XYTile> dupAnts = Collections.<XYTile>newSetFromMap(new ConcurrentHashMap());
        toFight.stream().forEachOrdered(tile -> {
            if (myAnts.get(tile).isEmpty()) {
                dupAnts.add(tile);
                return;
            }

            if (!dupAnts.contains(tile)) {
                enemyAnts.get(tile).parallelStream().filter(t -> !t.equals(tile) && toFight.contains(t))
                        .forEach(t -> {
                            LOGGER.info("merge hunt ants: {} {}", t, myAnts.get(t));
                            myAnts.get(tile).addAll(myAnts.get(t));
                            dupAnts.add(t);
                        });
            }
        });

        toFight.removeAll(dupAnts);

        if (!toFight.isEmpty()) {
            LOGGER.info("enemies to hunt 2: {}", toFight.size());
            LOGGER.info("enemies to hunt team: {}", myAnts.get(toFight.get(0)));
            LOGGER.info("enemies to hunt opp: {}", enemyAnts.get(toFight.get(0)));
        }

        Set<XYTile> cTargets = Collections.newSetFromMap(new ConcurrentHashMap<>(toFight.size()));
        toFight.parallelStream().forEachOrdered(t -> {
            XYTile ct = XYUtils.findCenter(myAnts.get(t));
            if (ct != null) {
                cTargets.add(ct);
            }
        });

        int cDist = COMBAT_DIST;
        int cSize = Utils.searchSize(cDist);

        Set<XYTile> myIncluded = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Map<XYTile, Set<XYTile>> combMyAnts = new ConcurrentHashMap<>();

        cTargets.parallelStream().forEachOrdered(tile -> {
            Map<XYTile, Integer> searched = new ConcurrentHashMap<>(cSize);

            Set<XYTile> allies = Collections.<XYTile>newSetFromMap(new ConcurrentHashMap<>());
            combMyAnts.put(tile, allies);

            TreeSearch.depthFirstFill(tile,
                    (t, l) -> {
                        Integer lv = searched.get(t);
                        if (lv == null || l < lv) {
                            searched.put(t, l);
                            return true;
                        }

                        return false;
                    },
                    l -> l < cDist,
                    (t, l) -> {
                        if (t.getStatus().isMyAnt() && !t.getStatus().isMoved() && myIncluded.add(t)) {
                            allies.add(t);
                        }
                    },
                    0);
        });

        Set<XYTile> oppIncluded = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Map<XYTile, Set<XYTile>> combOppAnts = new ConcurrentHashMap<>();

        int eDist = ANTI_DIST;
        int eSize = Utils.searchSize(eDist);

        cTargets.parallelStream().forEach(tile -> {
            Set<XYTile> mcb = combMyAnts.get(tile);
            Set<XYTile> myEnemies = Collections.<XYTile>newSetFromMap(new ConcurrentHashMap<>());
            combOppAnts.put(tile, myEnemies);

            Map<XYTile, Integer> searched = new ConcurrentHashMap<>(eSize);

            mcb.parallelStream().forEach(at -> {
                TreeSearch.depthFirstFill(at,
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
                                myEnemies.add(t);
                                oppIncluded.add(t);
                            }
                        },
                        0);
            });
        });

        if (!cTargets.isEmpty()) {
            LOGGER.info("combat: {}", cTargets.size());
            XYTile cf = cTargets.iterator().next();
            LOGGER.info("combat center: {}", cf);
            LOGGER.info("combat team: {}", combMyAnts.get(cf));
            LOGGER.info("combat opp: {}", combOppAnts.get(cf));
        }

        List<XYTile> fTargets = cTargets.stream().filter(t -> {
            return !combMyAnts.get(t).isEmpty() && !combOppAnts.get(t).isEmpty();
        }).collect(Collectors.toList());

        boolean aggressive = myIncluded.size() - oppIncluded.size() > 5;

        fTargets.parallelStream().forEachOrdered(t -> {
            Set<XYTile> ma = combMyAnts.get(t);
            Set<XYTile> oa = combOppAnts.get(t);

            if (ma.isEmpty()) {
                LOGGER.info("invalid combat: {}", t);
            }

            if (!ma.isEmpty() && !oa.isEmpty() && !terminator.get()) {
                Minimax.minimax(ma, oa, aggressive, terminator)
                        .parallelStream().forEach(mv -> op.apply(mv, move));
            }
        });

        if (terminator.get()) {
            return;
        }

        int aDist = ALLY_DIST;
        int aSize = Utils.searchSize(aDist);

        fTargets.parallelStream().forEach(tile -> {
            int diff = combOppAnts.get(tile).size() - combMyAnts.get(tile).size();
            diff = diff < 0 ? 1 : (diff + 1);

            Map<XYTile, Integer> searched = new ConcurrentHashMap<>(aSize);
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
                (l, c) -> l < aDist && c.get() > 0,
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
        });
    }


}
