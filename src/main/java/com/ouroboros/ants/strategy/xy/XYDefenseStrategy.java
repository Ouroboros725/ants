package com.ouroboros.ants.strategy.xy;

import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.xy.*;
import com.ouroboros.ants.utils.Move;
import com.ouroboros.ants.utils.Utils;
import com.ouroboros.ants.utils.xy.TreeSearch;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Sand on 5/2/2018.
 *
 */
public class XYDefenseStrategy {

    private static List<Tile> lastFood = new ArrayList<>();

    private static final int FOOD_DIST = 3;
    private static final int FOOD_HAV_DIST = 10;
    private static final int EXPLORE_DIST = 10;

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


    static void havFood(List<Tile> food, BiFunction<XYTileMove, Consumer<Move>, Boolean> op, Consumer<Move> move) {
        int dist = FOOD_DIST - 1;

        Set<Tile> foodSet = new HashSet<>(food);

        lastFood.parallelStream().filter(f -> !foodSet.contains(f)).map(XYTile::getTile).forEach(tile -> {
            int c = tile.getFood().getCount();
            final int cnt = c < 25 ? c : 25;
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

        List<XYTile> foodTarget = new ArrayList<>();

        food.parallelStream().map(XYTile::getTile).forEach(tile -> {
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
                        int inf = t.getFood().getInfluence();
                        t.getFood().setInfluence(inf + inf > 0 ? foodAccInf[l] : foodInf[l]);
                    },
                    0);
            tile.getFood().setCount(tile.getFood().getCount() + 1);
        });

        lastFood.clear();
        lastFood.addAll(food);

        Set<XYTile> exclude = new HashSet<>(foodTarget.size());
        List<XYTile> toFood = foodTarget.stream().filter(t -> t.getFood().getInfluence() >= foodInf[0]).sorted((t1, t2) -> t2.getFood().getInfluence() - t1.getFood().getInfluence())
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

        toFood.parallelStream().forEach(h -> {
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
                    (l, c) -> l < fDist && c.get() > 0,
                    (t, c) -> {
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

    static void explore(List<Tile> myAnts, BiFunction<XYTileMove, Consumer<Move>, Boolean> op, Consumer<Move> move) {
        XYTile.updateVisitInfluence();

        int dist = EXPLORE_DIST;

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

        myAnts.parallelStream().map(XYTile::getTile).filter(t -> !t.getStatus().isMoved()).forEach(tile -> {
            List<XYTileMoveAggWt> wList = tile.getNbDir().stream().map(XYTileMoveAggWt::new).collect(Collectors.toList());

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
                            int w = nbt.getMove().getTile().getVisit().getInfluence().get();
                            if (w > 0) {
                                border.add(nbt.getMove().getTile());
                                nbt.getWeight().set(nbt.getWeight().get() + w);
                            }
                        },
                        0));

            Optional<XYTileMoveAggWt> rw = wList.stream().filter(t -> t.getWeight().get() > 0).max((t1, t2) -> {
                int weight = t1.getWeight().get() - t2.getWeight().get();
                return weight != 0 ? weight : ThreadLocalRandom.current().nextInt(2) - 1;
            });

            rw.ifPresent(t -> op.apply(new XYTileMove(tile, t.getMove().getDir()), move));
        });

        if (!border.isEmpty()) {
            myAnts.parallelStream().map(XYTile::getTile).filter(t -> !t.getStatus().isMoved()).forEach(tile -> {
                Optional<XYTileWeighted> tw = border.parallelStream().map(b -> new XYTileWeighted(b, Utils.distManh(b.getX(), b.getY(), tile.getX(), tile.getY(), XYTile.getXt(), XYTile.getYt()))).min(Comparator.comparingInt(XYTileWeighted::getWeight));

                tw.ifPresent(tg -> {
                    Optional<XYTileMoveWeighted> tmw = tile.getNbDir().parallelStream().filter(nbt -> !nbt.getTile().getStatus().isTaboo() && !nbt.getTile().getStatus().isMyAnt())
                            .map(nbt -> new XYTileMoveWeighted(nbt, Utils.distManh(tg.getTile().getX(), tg.getTile().getY(), nbt.getTile().getX(), nbt.getTile().getY(), XYTile.getXt(), XYTile.getYt()))).min(Comparator.comparingInt(XYTileMoveWeighted::getWeight));
                    tmw.ifPresent(t -> op.apply(new XYTileMove(tile, t.getTile().getDir()), move));
                });
            });
        }
    }
}
