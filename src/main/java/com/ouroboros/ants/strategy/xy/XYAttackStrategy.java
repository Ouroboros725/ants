package com.ouroboros.ants.strategy.xy;

import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.xy.*;
import com.ouroboros.ants.utils.Move;
import com.ouroboros.ants.utils.Utils;
import com.ouroboros.ants.utils.xy.TreeSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
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
    private static final int ENEMY_DIST = 10;

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

            hills.parallelStream().map(XYTile::getTile)
                    .forEach(h -> {
                        Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>(361));
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
            Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>(361));
            TreeSearch.depthFirstFill(tile,
                    (t, l) -> {
                        if (!searched.contains(t)) {
                            searched.add(t);
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

            Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>(361));
            wList.parallelStream().filter(nbt -> !nbt.getMove().getTile().getStatus().isTaboo() && !nbt.getMove().getTile().getStatus().isMyAnt())
                    .forEach(nbt -> TreeSearch.depthFirstFill(nbt.getMove().getTile(),
                            (t, l) -> {
                                if (!searched.contains(t)) {
                                    searched.add(t);
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

    public static void attackEnemy(List<Tile> myAnts, BiFunction<XYTileMv, Consumer<Move>, Boolean> op, Consumer<Move> move) {
        int dist = ENEMY_DIST;

        myAnts.parallelStream().map(XYTile::getTile).forEach(tile -> {
            Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>(361));
            TreeSearch.depthFirstFill(tile,
                    (t, l) -> {
                        return searched.add(t);
                    },
                    l -> l < dist,
                    (t, l) -> {
                        if (t.getStatus().isOppAnt()) {
                            tile.getStatus().incEnemyCnt();
                        }
                    },
                    0);
        });
    }
}
