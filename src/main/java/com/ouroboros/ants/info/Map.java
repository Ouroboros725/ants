package com.ouroboros.ants.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public class Map implements Game {

    private static final Logger LOGGER = LoggerFactory.getLogger(Map.class);

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

    public int turns;
    public int rows;
    public int cols;
    public int loadtime;
    public int turntime;
    public int viewradius2;
    public int attackradius2;
    public int spawnradius2;
    public int player_seed;
    public int players;

    public Map(List<String> info) {
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
