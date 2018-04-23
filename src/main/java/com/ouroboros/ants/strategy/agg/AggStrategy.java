package com.ouroboros.ants.strategy.agg;

import com.google.common.collect.Lists;
import com.ouroboros.ants.game.*;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.strategy.AbstractStrategy;
import com.ouroboros.ants.utils.Move;
import com.ouroboros.ants.utils.Search.DistCalc;
import com.ouroboros.ants.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.ouroboros.ants.Ants.executor;
import static com.ouroboros.ants.Ants.scheduler;
import static com.ouroboros.ants.strategy.agg.AggStrategySyncData.*;
import static com.ouroboros.ants.utils.Influence.infUpdate;
import static com.ouroboros.ants.utils.Search.findPath;
import static com.ouroboros.ants.utils.Search.shallowDFSBack;
import static com.ouroboros.ants.utils.Utils.dist1D;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Created by zhanxies on 4/10/2018.
 *
 */
@Profile("agg")
@Component
public class AggStrategy extends AbstractStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggStrategy.class);

    private int xt;
    private int yt;

    private int[][][][] euclDist;
    private int[][][][] manhDist;

    DistCalc euclDistCalc = (x1, y1, x2, y2) -> euclDist[x1][y1][x2][y2];
    DistCalc manhDistCalc = (x1, y1, x2, y2) -> manhDist[x1][y1][x2][y2];

    private int viewRadius2;
    private int attackRadius2;
    private int spawnRadius2;

    private int spawnRadius;
    private int defenceRadius;
    private int borderRadius;
    private int attackHillRadius;
    private int getFoodRadius;

    private int  spawnInfRad;
    private int[] spawnInf;
    private int[] spawnAccInf;
    private int[][] spawnDecInf;

    @Override
    protected void setupStrategy(Global gameStates) {
        xt = gameStates.xt;
        yt = gameStates.yt;

        euclDist = new int[xt][yt][xt][yt];
        manhDist = new int[xt][yt][xt][yt];

        for (int x1 = 0; x1 < xt; x1++) {
            for (int y1 = 0; y1 < yt; y1++) {
                for (int x2 = 0; x2 < xt; x2++) {
                    for (int y2 = 0; y2 < xt; y2++) {
                        int dx = dist1D(x1, x2, xt);
                        int dy = dist1D(y1, y2, yt);
                        euclDist[x1][y1][x2][y2] = dx * dx + dy * dy;
                        manhDist[x1][y1][x2][y2] = dx + dy;
                    }
                }
            }
        }

        viewRadius2 = gameStates.viewRadius2;
        attackRadius2 = gameStates.attackRadius2;
        spawnRadius2 = gameStates.spawnRadius2;

        spawnRadius = (int) Math.sqrt(spawnRadius2);
        defenceRadius = (int) Math.ceil(Math.sqrt(attackRadius2 / 2.0d) * 2.0d);
        borderRadius = (int) Math.ceil(Math.sqrt(viewRadius2 / 2.0d) * 2.0d);
        attackHillRadius = (int) Math.ceil(Math.sqrt(viewRadius2) * 2.0d);
        getFoodRadius = (int) Math.ceil(Math.sqrt(viewRadius2 / 2.0d) * 2.0d);

        spawnInfRad = spawnRadius + 2;
        spawnInf = new int[spawnInfRad];
        spawnAccInf = new int[spawnInfRad];

        for (int i = 0; i < spawnInfRad; i++) {
            int w = spawnInfRad - i - 1;
            spawnInf[i] = 2 ^ w;
            spawnAccInf[i] = w;
        }

        spawnDecInf = new int[100][];
        for (int i = 0; i < 100; i++) {
            spawnDecInf[i] = spawnDecCalc(i);
        }

        setVisitInfMap(new int[xt][yt]);
        setWater(new boolean[xt][yt]);
        setLand(new boolean[xt][yt]);

        setPathsDict(new TileLink[xt][yt][xt][yt]);
        setPathsDist(new int[xt][yt][xt][yt]);

        setFoodInfCnt(new int[xt][yt]);
        setFoodInfMap(new int[xt][yt]);

        scheduler.scheduleAtFixedRate(this::preCalcEucl, gameStates.turnTime * 50, gameStates.turnTime * 10, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void executeStrategy(Turn turnInfo, Global gameStates, Consumer<Move> output) {
        if (!turnInfo.myAnts.isEmpty()) {
            initMovedAnts(xt, yt);

            CompletableFuture<Void> waterUpdate = runAsync(() -> updateWater(turnInfo.water), executor);

            CompletableFuture<boolean[][]> myAnts = supplyAsync(() -> getAntsMap(turnInfo.myAnts), executor);

            CompletableFuture<boolean[][]> oppAttArea = supplyAsync(() -> getAntsAttackArea(turnInfo.oppAnts), executor);
            CompletableFuture<boolean[][]> noGoArea = waterUpdate.thenCombineAsync(oppAttArea, Utils::getSecondArg, executor)
                    .thenComposeAsync(oaa -> supplyAsync(() -> getTabooArea(oaa)), executor);

            CompletableFuture<Void> attackHills = noGoArea.thenCombineAsync(myAnts, (ng, ants) -> Lists.newArrayList(ants, ng), executor)
                    .thenComposeAsync(list -> runAsync(() -> attackHills(turnInfo.oppHills, list.get(0), turnInfo.myAnts.size(), list.get(1), output)), executor);


            CompletableFuture<Void> updateFoodInf = runAsync(() -> updateFoodInfMap(turnInfo.food), executor);
            CompletableFuture<List<Tile>> tgtFood = updateFoodInf.thenComposeAsync(v -> supplyAsync(this::calcTargetFood), executor);
            CompletableFuture<Void> spawnFood = noGoArea.thenCombineAsync(myAnts, (ng, ants) -> Lists.newArrayList(ants, ng), executor)
                    .thenCombineAsync(tgtFood, (p1, p2) -> new SpawnFoodArgs(p1.get(0), p1.get(1), p2), executor)
                    .thenComposeAsync(arg -> runAsync(() -> spawnFood(arg, output)), executor);

            CompletableFuture<boolean[][]> oppAnts = supplyAsync(() -> getAntsMap(turnInfo.oppAnts), executor);

            CompletableFuture<int[][]> borderCalc = supplyAsync(() -> calcBorder(turnInfo.myAnts), executor);
            CompletableFuture<int[][]> oppBorderCalc = supplyAsync(() -> calcBorder(turnInfo.oppAnts), executor);

            try {
                CompletableFuture.allOf(attackHills, spawnFood)
                        .thenRunAsync(() -> LOGGER.debug("agg strategy finishes"))
                        .exceptionally(ex -> {
                            LOGGER.error("Agg strategy failed.", ex);
                            return null;
                        }).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("agg strategy crashed.", e);
            } catch (ExecutionException e) {
                LOGGER.error("agg strategy crashed.", e);
            }
        }

    }

    private void moveAnt(Move move, boolean[][] movedAnts, Consumer<Move> output) {
        movedAnts[move.x][move.y] = true;
        output.accept(move);
    }

    private void updateWater(List<Tile> waterT) {
        if (isMapKnown()) {
            return;
        }

        if (waterT.isEmpty()) {
            return;
        }

        boolean[][] water = getWater();
        for (Tile w : waterT) {
            water[w.x][w.y] = true;
        }
        setWater(water);
    }

    private boolean[][] getAntsAttackArea(List<Tile> antsT) {
        boolean[][] inf = new boolean[xt][yt];
        boolean[][] searched = new boolean[xt][yt];

        for (Tile at : antsT) {
            infUpdate(Tile.getTile(at.x, at.y), defenceRadius + 1, xt, yt, (ox, oy, i) -> {
                if (euclDist[at.x][at.y][ox][oy] <= attackRadius2 + 1) {
                    inf[ox][oy] = true;
                }
            }, searched);
        }

        return inf;
    }

    private boolean[][] getTabooArea(boolean[][] antsInf) {
        boolean[][] water = getWater();

        boolean[][] blocks = new boolean[xt][yt];

        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                if (water[x][y] || antsInf[x][y]) {
                    blocks[x][y] = true;
                }
            }
        }

        return blocks;
    }

    private boolean[][] getAntsMap(List<Tile> ants) {
        boolean[][] antsMap = new boolean[xt][yt];
        for (Tile t : ants) {
            antsMap[t.x][t.y] = true;
        }
        return antsMap;
    }



    private void attackHills(List<Tile> hills, boolean[][] ants, int antsNum, boolean[][] blocks, Consumer<Move> output) {
        int hillNum = hills.size();

        if (hillNum > 0) {
            boolean[][] movedAnts = getMovedAnts();

            int pAttNum = (int) (antsNum * 0.1);
            int attPHill = pAttNum / hillNum;
            int attNum = attPHill < 5 ? attPHill : 5;

            for (Tile hill : hills) {
                List<TileDir> moves = shallowDFSBack(hill, ants, movedAnts, blocks, xt, yt, attackHillRadius, attNum);
                for (TileDir td : moves) {
                    moveAnt(new Move(td.tile.x, td.tile.y, td.direction.getChar()), movedAnts, output);
                }
            }

            setMovedAnts(movedAnts);
        }
    }

    private void defendHills(List<Tile> hills, List<Tile> oppAnts) {

    }

    private void updateFoodInfMap(List<Tile> foodT) {
        int[][] foodInfCnt = getFoodInfCnt();
        int[][] foodInfMap = getFoodInfMap();

        boolean[][] food = new boolean[xt][yt];
        for (Tile t : foodT) {
            food[t.x][t.y] = true;
        }

        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                if (food[x][y]) {
                    int[] inc = foodInfCnt[x][y]++ == 0 ? spawnInf : spawnAccInf;
                    infUpdate(Tile.getTile(x, y), inc.length, xt, yt, (ox, oy, i) -> foodInfMap[ox][oy] += inc[i], new boolean[xt][yt]);
                } else if (foodInfCnt[x][y] > 0) {
                    int cnt = foodInfCnt[x][y];
                    int[] inc = cnt < 100 ? spawnDecInf[cnt] : spawnDecCalc(cnt);
                    infUpdate(Tile.getTile(x, y), inc.length, xt, yt, (ox, oy, i) -> foodInfMap[ox][oy] += inc[i], new boolean[xt][yt]);
                    foodInfCnt[x][y] = 0;
                }
            }
        }

        setFoodInfCnt(foodInfCnt);
        setFoodInfMap(foodInfMap);
    }

    private List<Tile> calcTargetFood() {
        int[][] foodInfMap = getFoodInfMap();

        List<TilePriority> foodList = new ArrayList<>();
        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                if (foodInfMap[x][y] >= spawnInf[0]) {
                    foodList.add(new TilePriority(Tile.getTile(x, y), foodInfMap[x][y]));
                }
            }
        }

        Collections.sort(foodList, Collections.reverseOrder());

        List<Tile> foodResult = new LinkedList<>();
        for (TilePriority tp : foodList) {
            if (!foodTileCovered(tp.tile, tp.priority, foodInfMap, spawnRadius)) {
                foodResult.add(tp.tile);
            }
        }

        return foodResult;
    }

    private boolean foodTileCovered(Tile tile, int priority, int[][] foodInfMap, int depth) {
        if (depth == 0) {
            return false;
        }

        for (Direction d : Direction.values()) {
            Tile dt = d.getNeighbour(tile.x, tile.y, xt, yt);
            if (foodInfMap[dt.x][dt.y] > priority) {
                return true;
            } else {
                boolean dr = foodTileCovered(dt, priority, foodInfMap, depth - 1);
                if (dr) {
                    return true;
                }
            }
        }

        return false;
    }

    private static class SpawnFoodArgs {
        List<Tile> foodList;
        boolean[][] ants;
        boolean[][] blocks;

        public SpawnFoodArgs(boolean[][] ants, boolean[][] blocks, List<Tile> foodList) {
            this.ants = ants;
            this.blocks = blocks;
            this.foodList = foodList;
        }
    }

    private void spawnFood(SpawnFoodArgs args, Consumer<Move> output) {
        if (!args.foodList.isEmpty()) {
            LOGGER.debug("spawn food");

            boolean[][] movedAnts = getMovedAnts();

            for (Tile t : args.foodList) {
                List<TileDir> tds = shallowDFSBack(t, args.ants, movedAnts, args.blocks, xt, yt, getFoodRadius, 1);
                if (!tds.isEmpty()) {
                    for (TileDir td : tds) {
                        LOGGER.debug("spawn food move: " + td.tile);
                        moveAnt(new Move(td.tile.x, td.tile.y, td.direction.getChar()), movedAnts, output);
                    }
                }
            }

            LOGGER.debug("spawn food finishes");
        }
    }

    private int[] spawnDecCalc(int cnt) {
        int[] dec = new int[spawnInfRad];

        for (int i = 0; i < spawnInfRad; i++) {
            int w = spawnInfRad - i - 1;
            dec[i] = -(2 ^ w) - (w * (cnt - 1));
        }

        return dec;
    }

    private int[][] calcBorder(List<Tile> antsT) {
        int[][] borderMap = new int[xt][yt];
        boolean[][] searched = new boolean[xt][yt];

        for (Tile at : antsT) {
            infUpdate(Tile.getTile(at.x, at.y), borderRadius, xt, yt, (ox, oy, i) -> {
                if (euclDist[at.x][at.y][ox][oy] <= viewRadius2) {
                    borderMap[ox][oy] = -1;
                } else {
                    borderMap[ox][oy] = 1;
                }
            }, searched);
        }

        return borderMap;
    }

    private void updateVisitInfMap(int[][] borderMap) {
        int[][] visitInfMap = getVisitInfMap();

        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                if (borderMap[x][y] < 0) {
                    visitInfMap[x][y] = 0;
                }

                visitInfMap[x][y]++;
            }
        }

        setVisitInfMap(visitInfMap);
    }

    private void updateLand(int[][] borders) {
        boolean[][] land = getLand();
        boolean[][] water = getWater();

        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                if (borders[x][y] < 0 && !water[x][y]) {
                    land[x][y] = true;
                }
            }
        }

        setLand(land);
    }

    private void preCalcEucl() {
        boolean[][] land = getLand();
        boolean[][] water = getWater();

        TileLink[][][][] pathsDict = getPathsDict();
        int[][][][] pathsDist = getPathsDist();


        boolean ready = true;
        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                if (!land[x][y] && !water[x][y]) {
                    ready = false;
                    break;
                }
            }
        }

        if (ready) {
            LOGGER.debug("paths calculation starts.");

            setMapKnown(true);

            for (int x1 = 0; x1 < xt; x1++) {
                for (int y1 = 0; y1 < yt; y1++) {
                    for (int x2 = 0; x2 < xt; x2++) {
                        for (int y2 = 0; y2 < yt; y2++) {
                            if (!water[x1][y1] && !water[x2][y2] && pathsDict[x1][y1][x2][y2] == null) {
                                findShortestPath(Tile.getTile(x1, y1), Tile.getTile(x2, y2), land, pathsDict, pathsDist);
                            }
                        }
                    }
                }
            }

            setPathsDict(pathsDict);
            setPathsDist(pathsDist);

            setPreCalcFinished(true);

            LOGGER.debug("paths calculation finishes.");
        }
    }

    private void findShortestPath(Tile t1, Tile t2, boolean[][] land, TileLink[][][][] pathsDict, int[][][][] pathsDist) {
        TileLink start = findPath(t1, t2, land, xt, yt);

        TileLink reverse = reversePath(start);
        populatePathsDict(start, pathsDict, pathsDist);
        populatePathsDict(reverse, pathsDict, pathsDist);
    }

    private TileLink reversePath(TileLink link) {
        TileLink last = null;
        TileLink next = link;
        do {
            TileLink current = new TileLink(link.current, null, last);
            if (last != null) {
                last.last = current;
            }

            last = current;
            next = next.next;
        } while (next != null);

        return last;
    }

    private void populatePathsDict(TileLink link, TileLink[][][][] pathsDict, int[][][][] pathsDist) {
        while (link.next != null) {
            TileLink next = link.next;
            int d = 1;
            do {
                pathsDict[link.current.x][link.current.y][next.current.x][next.current.y] = next;
                pathsDist[link.current.x][link.current.y][next.current.x][next.current.y] = d;
                next = next.next;
                d++;
            } while (next != null);
            link = link.next;
        }
    }
}
