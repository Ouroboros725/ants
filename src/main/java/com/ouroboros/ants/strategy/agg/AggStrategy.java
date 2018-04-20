package com.ouroboros.ants.strategy.agg;

import com.google.common.collect.Lists;
import com.ouroboros.ants.game.Global;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TileDir;
import com.ouroboros.ants.game.TileLink;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.strategy.AbstractStrategy;
import com.ouroboros.ants.utils.Move;
import com.ouroboros.ants.utils.Search.DistCalc;
import com.ouroboros.ants.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.ouroboros.ants.Ants.executor;
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

    private int  viewRadius2;
    private int  attackRadius2;
    private int  spawnRadius2;

    private int  spawnRadius;
    private int  defenceRadius;
    private int  borderRadius;
    private int  attackHillRadius;

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
    }

    @Override
    protected void executeStrategy(Turn turnInfo, Global gameStates, Consumer<Move> output) {
        if (!turnInfo.myAnts.isEmpty()) {
            initMovedAnts(xt, yt);

            CompletableFuture<Void> waterUpdate = runAsync(() -> updateWater(turnInfo.water), executor);
            CompletableFuture<Void> foodUpdate = runAsync(() -> updateFoodInfMap(turnInfo.food), executor);
            CompletableFuture<int[][]> borderCalc = supplyAsync(() -> calcBorder(turnInfo.myAnts), executor);
            CompletableFuture<int[][]> oppBorderCalc = supplyAsync(() -> calcBorder(turnInfo.oppAnts), executor);
            CompletableFuture<boolean[][]> myAnts = supplyAsync(() -> getAntsMap(turnInfo.myAnts), executor);
            CompletableFuture<boolean[][]> oppAnts = supplyAsync(() -> getAntsMap(turnInfo.oppAnts), executor);

            CompletableFuture<boolean[][]> oppAttArea = supplyAsync(() -> getAntsDefenseInf(turnInfo.oppAnts), executor);
            CompletableFuture<boolean[][]> noGoArea = waterUpdate.thenCombineAsync(oppAttArea, Utils::getSecondArg, executor)
                    .thenComposeAsync(oaa -> supplyAsync(() -> getNoGoArea(oaa)), executor);

            CompletableFuture<Void> attackHills = noGoArea.thenCombineAsync(myAnts, (ng, ants) -> Lists.newArrayList(ants, ng), executor)
                    .thenComposeAsync(list -> runAsync(() -> attackHills(turnInfo.oppHills, list.get(0), turnInfo.myAnts.size(), list.get(1), output)), executor);


            attackHills.thenRun(() -> {
            });
        }

    }

    private void updateWater(List<Tile> waterT) {
        boolean[][] water = getWater();
        for (Tile w : waterT) {
            water[w.x][w.y] = true;
        }
        setWater(water);
    }

    private boolean[][] getAntsDefenseInf(List<Tile> antsT) {
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

    private boolean[][] getNoGoArea(boolean[][] antsInf) {
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
        boolean[][] movedAnts = getMovedAnts();

        int hillNum = hills.size();
        int pAttNum = (int) (antsNum * 0.1);
        int attPHill = pAttNum / hillNum;
        int attNum = attPHill < 5 ? attPHill : 5;

        for (Tile hill : hills) {
            List<TileDir> moves = shallowDFSBack(hill, ants, movedAnts, blocks, xt, yt, attackHillRadius, attNum);
            for (TileDir td : moves) {
                movedAnts[td.tile.x][td.tile.y] = true;
                output.accept(new Move(td.tile.x, td.tile.y, td.direction.getChar()));
            }
        }

        setMovedAnts(movedAnts);
    }

    private void defendHills(List<Tile> hills, List<Tile> oppAnts) {

    }

    private void spawnFood() {
        // update influence map
        // find key locations
        // find candidate ants
        // ants find food
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

        boolean finished = false;

        while (!finished) {
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

                finished = true;
            } else {
                try {
                    Thread.sleep(5000l);
                } catch (InterruptedException e) {
                    LOGGER.error("Path calculation thread interrupted.", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

        setPathsDict(pathsDict);
        setPathsDist(pathsDist);
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
