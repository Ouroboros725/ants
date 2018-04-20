package com.ouroboros.ants.strategy.agg;

import com.ouroboros.ants.game.TileLink;

/**
 * Created by zhanxies on 4/20/2018.
 *
 */
public class AggStrategySyncData {

    private AggStrategySyncData() {
        throw new IllegalStateException("Utility class");
    }

    private static volatile int[][] foodInfMap;
    private static volatile int[][] foodInfCnt;

    private static volatile int[][] visitInfMap;
    private static volatile boolean[][] water;
    private static volatile boolean[][] land;

    private static volatile boolean[][] movedAnts;

    private static volatile TileLink[][][][] pathsDict;
    private static volatile int[][][][] pathsDist;

    private static volatile boolean preCalcFinished;
    private static volatile boolean mapKnown;

    public static int[][] getFoodInfCnt() {
        return foodInfCnt;
    }

    public static void setFoodInfCnt(int[][] foodInfCnt) {
        AggStrategySyncData.foodInfCnt = foodInfCnt;
    }

    public static int[][] getFoodInfMap() {
        return foodInfMap;
    }

    public static void setFoodInfMap(int[][] foodInfMap) {
        AggStrategySyncData.foodInfMap = foodInfMap;
    }

    public static boolean[][] getLand() {
        return land;
    }

    public static void setLand(boolean[][] land) {
        AggStrategySyncData.land = land;
    }

    public static boolean[][] getMovedAnts() {
        return movedAnts;
    }

    public static void setMovedAnts(boolean[][] movedAnts) {
        AggStrategySyncData.movedAnts = movedAnts;
    }

    public static boolean[][] initMovedAnts(int xt, int yt) {
        movedAnts = new boolean[xt][yt];
        return movedAnts;
    }

    public static TileLink[][][][] getPathsDict() {
        return pathsDict;
    }

    public static void setPathsDict(TileLink[][][][] pathsDict) {
        AggStrategySyncData.pathsDict = pathsDict;
    }

    public static int[][][][] getPathsDist() {
        return pathsDist;
    }

    public static void setPathsDist(int[][][][] pathsDist) {
        AggStrategySyncData.pathsDist = pathsDist;
    }

    public static int[][] getVisitInfMap() {
        return visitInfMap;
    }

    public static void setVisitInfMap(int[][] visitInfMap) {
        AggStrategySyncData.visitInfMap = visitInfMap;
    }

    public static boolean[][] getWater() {
        return water;
    }

    public static void setWater(boolean[][] water) {
        AggStrategySyncData.water = water;
    }

    public static boolean isMapKnown() {
        return mapKnown;
    }

    public static void setMapKnown(boolean mapKnown) {
        AggStrategySyncData.mapKnown = mapKnown;
    }

    public static boolean isPreCalcFinished() {
        return preCalcFinished;
    }

    public static void setPreCalcFinished(boolean preCalcFinished) {
        AggStrategySyncData.preCalcFinished = preCalcFinished;
    }
}
