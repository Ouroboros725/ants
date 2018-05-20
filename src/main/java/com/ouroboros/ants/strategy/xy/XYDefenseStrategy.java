package com.ouroboros.ants.strategy.xy;

import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.game.xy.XYTileMv;
import com.ouroboros.ants.utils.Move;
import com.ouroboros.ants.utils.xy.TreeSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Sand on 5/2/2018.
 *
 */
public class XYDefenseStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(XYDefenseStrategy.class);

    private static List<Tile> lastFood = new ArrayList<>();

    private static final int FOOD_DIST = 3;
    private static final int FOOD_HAV_DIST = 10;

    private static final int DEFENCE_DIST = 12;

    private static int[] foodInf;
    private static int[] foodAccInf;
    private static int[][] foodDecInf;

    static {
        foodInf = new int[FOOD_DIST];
        foodAccInf = new int[FOOD_DIST];

        for (int i = 0; i < FOOD_DIST; i++) {
            int w = FOOD_DIST - i - 1;
            foodInf[i] = (int) Math.pow(2, w);
            foodAccInf[i] = w;
        }

        foodDecInf = new int[25][];
        for (int i = 0; i < 25; i++) {
            foodDecInf[i] = spawnDecCalc(i);
        }
    }

    private static int[] spawnDecCalc(int cnt) {
        int[] dec = new int[FOOD_DIST];

        for (int i = 0; i < FOOD_DIST; i++) {
            dec[i] = -(foodInf[i] + cnt * foodAccInf[i]);
        }

        return dec;
    }


    static void havFood(List<Tile> food, BiFunction<XYTileMv, Consumer<Move>, Boolean> op, Consumer<Move> move) {
        int dist = FOOD_DIST - 1;

        Set<Tile> foodSet = new HashSet<>(food);

        lastFood.parallelStream().filter(f -> !foodSet.contains(f)).map(XYTile::getTile).forEach(tile -> {
//            LOGGER.info("miss food: {}", tile );
            int c = tile.getFood().getCount();
            final int cnt = c < 25 ? c : 24;
            Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>(13));
            TreeSearch.depthFirstFill(tile,
                    (t, l) -> {
                        if (!searched.contains(t)) {
                            searched.add(t);
                            return true;
                        }
                        return false;
                    },
                    l -> l < dist,
                    (t, l) -> t.getFood().setInfluence(t.getFood().getInfluence() + foodDecInf[cnt][l]),
                    0);
            tile.getFood().setCount(0);
        });

        Set<XYTile> foodTarget = Collections.newSetFromMap(new ConcurrentHashMap<>());

        food.parallelStream().map(XYTile::getTile).forEach(tile -> {
            int cnt = tile.getFood().getCount();
            Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>(13));
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
                        foodTarget.add(t);
                        if (cnt < 25) {
                            int inf = t.getFood().getInfluence();
                            t.getFood().setInfluence(inf + (cnt > 0 ? foodAccInf[l] : foodInf[l]));
                        }
                    },
                    0);
            tile.getFood().incCount();
        });

        lastFood.clear();
        lastFood.addAll(food);

        LOGGER.info("food: {}", foodTarget.size());

        Set<XYTile> exclude = new HashSet<>(foodTarget.size());
        List<XYTile> toFood = foodTarget.stream()
                .filter(t -> {
//                    LOGGER.info("food inf filter: {} {}", t.getFood().getInfluence(), t.getFood().getInfluence() >= foodInf[0]);
                    return t.getFood().getInfluence() >= foodInf[0];
                })
                .sorted((t1, t2) -> t2.getFood().getInfluence() - t1.getFood().getInfluence())
                .filter(tile -> {
                    if (exclude.contains(tile)) {
                        return false;
                    } else {
                        exclude.add(tile);
                        Set<XYTile> searched = new HashSet<>(13);
                        TreeSearch.depthFirstFill(tile,
                                (t, l) -> {
                                    if (!searched.contains(t)) {
                                        searched.add(t);
                                        return true;
                                    }
                                    return false;
                                },
                                l -> l < dist,
                                (t, l) -> exclude.add(t),
                                0);

                        return true;
                    }
                }).collect(Collectors.toList());

        int fDist = FOOD_HAV_DIST;

        LOGGER.info("food: {}", toFood.size());

        toFood.parallelStream().forEach(h -> {
            Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>(361));
            searched.add(h);
            TreeSearch.breadthFirstMultiSearch(h.getNbDir(),
                    (t, l) -> {
                        if (!searched.contains(t.getTile())) {
                            searched.add(t.getTile());
//                            LOGGER.info("searched: " + !t.getTile().getStatus().isTaboo());
                            return !t.getTile().getStatus().isTaboo();
                        }
                        return false;
                    },
                    (l, c) -> l < fDist && c.get() > 0,
                    (t, c) -> {
//                        LOGGER.info("my ant: " + (t.getTile().getStatus().isMyAnt()));
//                        LOGGER.info("moved: " + (!t.getTile().getStatus().getMoved().get()));
                        if (t.getTile().getStatus().isMyAnt() && !t.getTile().getStatus().getMoved().getAndSet(true)) {
                            if (op.apply(t, move)) {
                                c.getAndDecrement();
                            }
                        }
                    },
                    new AtomicInteger(1),
                    0
            );
        });
    }

    static void defendHill(List<Tile> hills, int antsNum, BiFunction<XYTileMv, Consumer<Move>, Boolean> op, Consumer<Move> move) {
        int hillNum = hills.size();

        if (hillNum > 0) {
            int pAttNum = (int) (antsNum * 0.025);
            int defPHill = pAttNum / hillNum;
            int attNum = defPHill > 1 ? defPHill : 1;

            int dist = DEFENCE_DIST;

            Map<XYTile, XYTile> oppAnts = new ConcurrentHashMap<>();
            hills.parallelStream().map(XYTile::getTile).forEach(tile -> {
                Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>(361));

                TreeSearch.depthFirstFill(tile,
                        (t, l) -> {
                            return searched.add(t);
                        },
                        l -> l < dist,
                        (t, l) -> {
                            if (t.getStatus().isOppAnt()) {
                                oppAnts.put(t, tile);
                            }
                        },
                        0);
            });

            if (!oppAnts.isEmpty()) {
                Map<XYTile, Integer> kSpot = new ConcurrentHashMap<>(oppAnts.size());

                oppAnts.entrySet().parallelStream().forEach(entry -> {
                    List<XYTileMv> steps = new ArrayList<>();
                    Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>());
                    searched.add(entry.getKey());
                    Map<XYTileMv, XYTileMv> start = new HashMap<>();
                    entry.getKey().getNbDir().parallelStream().forEach(t -> start.put(t, null));
                    TreeSearch.breadthFirstLink(start, entry.getValue(), searched::add, steps);
                    if (!steps.isEmpty()) {
                        kSpot.put(steps.get(steps.size() / 2).getTile(), steps.size());
                    }
                });

                kSpot.entrySet().parallelStream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).forEachOrdered(tile -> {
                    Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>());
                    searched.add(tile);

                    TreeSearch.breadthFirstMultiSearch(tile.getNbDir(),
                            (t, l) -> {
                                return searched.add(t.getTile());
                            },
                            (l, c) -> l < dist && c.get() > 0,
                            (t, c) -> {
                                if (t.getTile().getStatus().isMyAnt() && !t.getTile().getStatus().getMoved().getAndSet(true)) {
                                    if (op.apply(t, move)) {
                                        LOGGER.info("defend hill: {}", t.getTile());
                                        c.getAndDecrement();
                                    }
                                }
                            },
                            new AtomicInteger(1),
                            0
                    );
                });
            }
        }
    }
}
