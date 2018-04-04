package com.ouroboros.ants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public class InfoMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoMap.class);

    private static final String colsStr = "cols";
    private static final String rowsStr = "rows";
    private static final String turnsStr = "turns";
    private static final String loadtimeStr = "loadtime";
    private static final String turntimeStr = "turntime";
    private static final String viewradius2Str = "viewradius2";
    private static final String attackradius2Str = "attackradius2";
    private static final String spawnradius2Str = "spawnradius2";
    private static final String player_seedStr = "player_seed";
    private static final String playersStr = "players";

    int turns;
    int rows;
    int cols;
    int loadtime;
    int turntime;
    int viewradius2;
    int attackradius2;
    int spawnradius2;
    int player_seed;
    int players;

    InfoMap(List<String> info) {
        try {
            for (String line : info) {
                LOGGER.debug("received map info {}", line);

                String tokens[] = line.split(" ");
                String field = tokens[0].toLowerCase();

                switch (field) {
                    case colsStr:
                        this.cols = Integer.parseInt(tokens[1]);
                        break;
                    case rowsStr:
                        this.rows = Integer.parseInt(tokens[1]);
                        break;
                    case turnsStr:
                        this.turns = Integer.parseInt(tokens[1]);
                        break;
                    case loadtimeStr:
                        this.loadtime = Integer.parseInt(tokens[1]);
                        break;
                    case turntimeStr:
                        this.turntime = Integer.parseInt(tokens[1]);
                        break;
                    case viewradius2Str:
                        this.viewradius2 = Integer.parseInt(tokens[1]);
                        break;
                    case attackradius2Str:
                        this.attackradius2 = Integer.parseInt(tokens[1]);
                        break;
                    case spawnradius2Str:
                        this.spawnradius2 = Integer.parseInt(tokens[1]);
                        break;
                    case player_seedStr:
                        this.player_seed = Integer.parseInt(tokens[1]);
                        break;
                    case playersStr:
                        this.players = Integer.parseInt(tokens[1]);
                        break;
                    default:
                        LOGGER.debug("unrecognized map info {}", line);
                        break;
                }
            }
        } catch (NumberFormatException ex) {
            LOGGER.error("failed to process map info", ex);
        }
    }


}
