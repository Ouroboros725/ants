package com.ouroboros.ants.strategy.agg;

import com.google.common.collect.Lists;
import com.ouroboros.ants.game.Global;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TileDir;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.strategy.AbstractStrategy;
import com.ouroboros.ants.utils.Move;
import com.ouroboros.ants.utils.Search.DistCalc;
import com.ouroboros.ants.utils.Utils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.ouroboros.ants.Ants.executor;
import static com.ouroboros.ants.utils.Influence.infUpdate;
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

    int xt;
    int yt;

    int[][][][] euclDist;
    int[][][][] manhDist;

    DistCalc euclDistCalc = (x1, y1, x2, y2) -> euclDist[x1][y1][x2][y2];
    DistCalc manhDistCalc = (x1, y1, x2, y2) -> manhDist[x1][y1][x2][y2];

    int viewRadius2;
    int attackRadius2;
    int spawnRadius2;

    int spawnRadius;
    int defenceRadius;
    int borderRadius;
    int attackHillRadius;

    int spawnInfRad;
    int[] spawnInf;
    int[] spawnAccInf;
    int[][] spawnDecInf;
    int[][] foodInfMap;
    int[][] foodInfCnt;

    int[][] visitInfMap;
    boolean[][] water;

    boolean[][] movedAnts;

    boolean[][] land;

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

        visitInfMap = new int[xt][yt];
        water = new boolean[xt][yt];

        land = new boolean[xt][yt];

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
    }

    @Override
    protected void executeStrategy(Turn turnInfo, Global gameStates, Consumer<Move> output) {
        if (!turnInfo.myAnts.isEmpty()) {
            movedAnts = new boolean[xt][yt];

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
        for (Tile w : waterT) {
            water[w.x][w.y] = true;
        }
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
        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                if (borderMap[x][y] < 0) {
                    visitInfMap[x][y] = 0;
                }

                visitInfMap[x][y]++;
            }
        }
    }

    private void updateLand(int[][] borders) {
        for (int x = 0; x < xt; x++) {
            for (int y = 0; y < yt; y++) {
                if (borders[x][y] < 0 && !water[x][y]) {
                    land[x][y] = true;
                }
            }
        }
    }

    private void preCalcEucl() {

    }
}
