package com.ouroboros.ants.strategy.agg;

import com.ouroboros.ants.game.Direction;
import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TileDir;
import com.ouroboros.ants.game.TilePriority;
import com.ouroboros.ants.utils.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static com.ouroboros.ants.strategy.agg.AggStrategySyncData.*;
import static com.ouroboros.ants.strategy.agg.AggStrategySyncData.getFoodInfMap;
import static com.ouroboros.ants.utils.Influence.infUpdate;
import static com.ouroboros.ants.utils.Search.shallowDFSBack;

/**
 * Created by zhanxies on 4/25/2018.
 *
 */
@Profile("agg")
@Component
public class AggStrategyFood {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggStrategyFood.class);

    @Autowired
    AggStrategy str;

    void updateFoodInfMap(List<Tile> foodT) {
        int[][] foodInfCnt = getFoodInfCnt();
        int[][] foodInfMap = getFoodInfMap();

        boolean[][] food = new boolean[str.xt][str.yt];
        for (Tile t : foodT) {
            food[t.x][t.y] = true;
        }

        for (int x = 0; x < str.xt; x++) {
            for (int y = 0; y < str.yt; y++) {
                if (food[x][y]) {
                    int[] inc = foodInfCnt[x][y]++ == 0 ? str.spawnInf : str.spawnAccInf;
                    infUpdate(Tile.getTile(x, y), inc.length, str.xt, str.yt, (ox, oy, i) -> foodInfMap[ox][oy] += inc[i], new boolean[str.xt][str.yt]);
                } else if (foodInfCnt[x][y] > 0) {
                    int cnt = foodInfCnt[x][y] - 1;
                    int[] inc = cnt <= 100 ? str.spawnDecInf[cnt] : str.spawnDecCalc(cnt);
                    infUpdate(Tile.getTile(x, y), inc.length, str.xt, str.yt, (ox, oy, i) -> foodInfMap[ox][oy] += inc[i], new boolean[str.xt][str.yt]);
                    foodInfCnt[x][y] = 0;
                }
            }
        }

        setFoodInfCnt(foodInfCnt);
        setFoodInfMap(foodInfMap);
    }

    List<Tile> calcTargetFood() {
        int[][] foodInfMap = getFoodInfMap();

//        LOGGER.debug("food inf map: \n{}", Arrays.deepToString(foodInfMap));

//        LOGGER.debug("spawn inf {}", Arrays.toString(spawnInf));
//        LOGGER.debug("spawn acc inf {}", Arrays.toString(spawnAccInf));
//        LOGGER.debug("spawn dec inf {}", Arrays.toString(spawnDecInf[99]));

        List<TilePriority> foodList = new ArrayList<>();
        for (int x = 0; x < str.xt; x++) {
            for (int y = 0; y < str.yt; y++) {
                if (foodInfMap[x][y] >= str.spawnInf[0]) {
                    foodList.add(new TilePriority(Tile.getTile(x, y), foodInfMap[x][y]));
                }
            }
        }

        Collections.sort(foodList, Collections.reverseOrder());

        List<Tile> foodResult = new LinkedList<>();
        for (TilePriority tp : foodList) {
            if (!foodTileCovered(tp.tile, tp.priority, foodInfMap, str.spawnRadius)) {
                foodResult.add(tp.tile);
                LOGGER.debug("food inf map selected: {}, {}", tp.tile, tp.priority);
            } else {
                LOGGER.debug("food inf map dropped: {}, {}", tp.tile, tp.priority);
            }
        }

        return foodResult;
    }

    boolean foodTileCovered(Tile tile, int priority, int[][] foodInfMap, int depth) {
        if (depth == 0) {
            return false;
        }

        for (Direction d : Direction.values()) {
            Tile dt = d.getNeighbour(tile.x, tile.y, str.xt, str.yt);
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

    void havFood(List<Tile> foodList, boolean[][] blocks, Consumer<Move> output) {
        if (!foodList.isEmpty()) {
            LOGGER.debug("hav food");

            str.moveAnts((ants, movedAnts) -> {
                for (Tile t : foodList) {
                    List<TileDir> tds = shallowDFSBack(t, ants, movedAnts, blocks, str.xt, str.yt, str.getFoodRadius, 1);
                    if (!tds.isEmpty()) {
                        for (TileDir td : tds) {
                            LOGGER.debug("hav food food: {}", t);
                            LOGGER.debug("hav food move: {}", td.tile);
                            str.moveAnt(td, ants, movedAnts, output);
                        }
                    }
                }
            });

//            LOGGER.debug("hav food finishes");
        }
    }


}
