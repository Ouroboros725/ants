package com.ouroboros.ants.strategy.agg;

import com.ouroboros.ants.game.Situation;
import com.ouroboros.ants.info.Turn;
import com.ouroboros.ants.strategy.AbstractStrategy;
import com.ouroboros.ants.utils.Move;
import com.ouroboros.ants.utils.Search.DistCalc;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static com.ouroboros.ants.utils.Utils.dist1D;

/**
 * Created by zhanxies on 4/10/2018.
 *
 */
@Profile("agg")
@Component
public class AggStrategy extends AbstractStrategy {

    int[][][][] euclDist;
    int[][][][] manhDist;

    DistCalc euclDistCalc = (x1, y1, x2, y2) -> euclDist[x1][y1][x2][y2];
    DistCalc manhDistCalc = (x1, y1, x2, y2) -> manhDist[x1][y1][x2][y2];

    int viewRadiusSteps;
    int attackRadiusSteps;
    int spawnRadiusSteps;

    @Override
    protected void setupStrategy(Situation gameStates) {
        int xt = gameStates.xt;
        int yt = gameStates.yt;

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

        viewRadiusSteps = (int) Math.floor(Math.sqrt(gameStates.viewRadius2 / 2.0d) * 2.0d);
        attackRadiusSteps = (int) Math.floor(Math.sqrt(gameStates.attackRadius2 / 2.0d) * 2.0d);
        spawnRadiusSteps = (int) Math.floor(Math.sqrt(gameStates.spawnRadius2 / 2.0d) * 2.0d);
    }



    @Override
    protected void executeStrategy(Turn turnInfo, Situation gameStates, Consumer<Move> output) {

    }

    private void food() {

    }
}
