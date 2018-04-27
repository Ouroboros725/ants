package com.ouroboros.ants.strategy.agg;

import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TileDir;
import com.ouroboros.ants.utils.Move;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

import static com.ouroboros.ants.strategy.agg.AggStrategySyncData.*;
import static com.ouroboros.ants.utils.Search.shallowDFSBack;

/**
 * Created by zhanxies on 4/25/2018.
 *
 */
@Profile("agg")
@Component
public class AggStrategyHill {

    @Autowired
    AggStrategy str;

    void attackHills(List<Tile> hills, int antsNum, boolean[][] blocks, Consumer<Move> output) {
        int hillNum = hills.size();

        if (hillNum > 0) {
            str.moveAnts((ants, movedAnts) -> {
                int pAttNum = (int) (antsNum * 0.1);
                int attPHill = pAttNum / hillNum;
                int attNum = attPHill < 5 ? attPHill : 5;

                for (Tile hill : hills) {
                    List<TileDir> moves = shallowDFSBack(hill, ants, movedAnts, blocks, str.xt, str.yt, str.attackHillRadius, attNum);
                    for (TileDir td : moves) {
                        str.moveAnt(td, ants, movedAnts, output);
                    }
                }
            });
        }
    }

    void defendHills(List<Tile> hills, List<Tile> oppAnts) {

    }
}
