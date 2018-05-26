package com.ouroboros.ants.strategy.agg;

import com.ouroboros.ants.game.Global;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TileDir;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.strategy.AbstractStrategy;
import com.ouroboros.ants.utils.Move;
import com.ouroboros.ants.utils.Search.DistCalc;
import com.ouroboros.ants.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.ouroboros.ants.Ants.executor;
import static com.ouroboros.ants.Ants.scheduler;
import static com.ouroboros.ants.strategy.agg.AggStrategySyncData.*;
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

    @Autowired
    AggStrategyCommon common;

    @Autowired
    AggStrategyCalc calc;

    @Autowired
    AggStrategyHill hill;

    @Autowired
    AggStrategyFood food;

    @Autowired
    AggStrategyExplore explore;

    int xt;
    int yt;

    int[][][][] euclDist;
    int[][][][] manhDist;

    int viewRadius2;
    int attackRadius2;
    int spawnRadius2;

    int spawnRadius;
    int defenceRadius;
    int borderRadius;
    int attackHillRadius;
    int getFoodRadius;

    int spawnInfRad;
    int[] spawnInf;
    int[] spawnAccInf;
    int[][] spawnDecInf;

    DistCalc euclDistCalc = (x1, y1, x2, y2) -> euclDist[x1][y1][x2][y2];
    DistCalc manhDistCalc = (x1, y1, x2, y2) -> manhDist[x1][y1][x2][y2];

    @Override
    protected void setupStrategy(Global gameStates, AtomicBoolean terminator) {
        xt = gameStates.xt;
        yt = gameStates.yt;

        euclDist = new int[xt][yt][xt][yt];
        manhDist = new int[xt][yt][xt][yt];

        LOGGER.debug("prepare 1");

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

        LOGGER.debug("prepare 2");

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
            spawnInf[i] = (int) Math.pow(2, w);
            spawnAccInf[i] = w;
        }

        spawnDecInf = new int[100][];
        for (int i = 0; i < 100; i++) {
            spawnDecInf[i] = spawnDecCalc(i);
        }

        LOGGER.debug("prepare 3");

        setVisitInfMap(new int[xt][yt]);
        setWater(new boolean[xt][yt]);
        setLand(new boolean[xt][yt]);

        LOGGER.debug("prepare 4");

        LOGGER.debug("prepare 5");

        setFoodInfCnt(new int[xt][yt]);
        setFoodInfMap(new int[xt][yt]);

        scheduler.schedule(calc::prepareCalc, gameStates.turnTime, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(calc::preCalcEucl, gameStates.turnTime * 50, gameStates.turnTime * 10, TimeUnit.MILLISECONDS);

        LOGGER.debug("prepare 6");
    }

    @Override
    protected void executeStrategy(Turn turnInfo, Global gameStates, Consumer<Move> output, AtomicBoolean terminator) {
        if (!turnInfo.myAnts.isEmpty()) {
            LOGGER.debug("init turn");
            initMovedAnts(xt, yt);
            initAnts(xt, yt);

            LOGGER.debug("exec turn");

            try {
                CompletableFuture<Void> waterUpdate = runAsync(() -> common.updateWater(turnInfo.water), executor);

                CompletableFuture<Void> myAnts = runAsync(() -> common.updateAntsMap(turnInfo.myAnts), executor);

                CompletableFuture<boolean[][]> oppAttArea = supplyAsync(() -> common.getAntsAttackArea(turnInfo.oppAnts), executor);
                CompletableFuture<boolean[][]> noGoArea = waterUpdate.thenCombineAsync(oppAttArea, Utils::getSecondArg, executor)
                        .thenApplyAsync(common::getTabooArea, executor);

                CompletableFuture<Void> attackHills = noGoArea.thenCombineAsync(myAnts, Utils::getFirstArg, executor)
                        .thenAcceptBothAsync(myAnts, (ng, ants) -> hill.attackHills(turnInfo.oppHills, turnInfo.myAnts.size(), ng, output), executor);

                LOGGER.debug("attack hill");
                attackHills.get();

                CompletableFuture<Void> foodInfUpdate = runAsync(() -> food.updateFoodInfMap(turnInfo.food), executor);
                CompletableFuture<List<Tile>> tgtFood = foodInfUpdate.thenApplyAsync(v -> food.calcTargetFood(), executor);
                CompletableFuture<Void> havFood = noGoArea.thenCombineAsync(myAnts, Utils::getFirstArg, executor)
                        .thenAcceptBothAsync(tgtFood, (ng, fd) -> food.havFood(fd, ng, output), executor);

                LOGGER.debug("hav food");
                havFood.get();

                CompletableFuture<int[][]> borderCalc = supplyAsync(() -> explore.calcBorder(turnInfo.myAnts), executor);
                CompletableFuture<Void> visitInfUpdate = borderCalc.thenAcceptAsync(explore::updateVisitInfMap, executor);
                CompletableFuture<Void> exploreUpdate = visitInfUpdate.thenCombineAsync(noGoArea, Utils::getSecondArg, executor)
                        .thenAcceptBothAsync(borderCalc, (ng, bd) -> explore.explore(bd, ng, output), executor);

                LOGGER.debug("explore");
                exploreUpdate.get();

                CompletableFuture<int[][]> oppBorderCalc = supplyAsync(() -> explore.calcBorder(turnInfo.oppAnts), executor);
                CompletableFuture<boolean[][]> oppAnts = supplyAsync(() -> common.getAntsMap(turnInfo.oppAnts), executor);

                CompletableFuture<Void> landUpdate = borderCalc.thenComposeAsync(infMap -> runAsync(() -> explore.updateLand(infMap), executor), executor);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("agg strategy crashed.", e);
            } catch (ExecutionException e) {
                LOGGER.error("agg strategy crashed.", e);
            }
        }

    }

    int[] spawnDecCalc(int cnt) {
        int[] dec = new int[spawnInfRad];

        for (int i = 0; i < spawnInfRad; i++) {
            dec[i] = -(spawnInf[i] + cnt * spawnAccInf[i]);
        }

        return dec;
    }

    void moveAnt(TileDir td, boolean[][] ants, boolean[][] movedAnts, Consumer<Move> output) {
        ants[td.tile.x][td.tile.y] = false;
        Tile newT = td.direction.getNeighbour(td.tile.x, td.tile.y, xt, yt);
        movedAnts[newT.x][newT.y] = true;
        ants[newT.x][newT.y] = true;
        output.accept(new Move(td.tile.x, td.tile.y, td.direction.getChar()));
    }

    void moveAnts(BiConsumer<boolean[][], boolean[][]> moveConsumer) {
        boolean[][] movedAnts = getMovedAnts();
        boolean[][] ants = getAnts();

        moveConsumer.accept(ants, movedAnts);

        setAnts(ants);
        setMovedAnts(movedAnts);
    }
}
