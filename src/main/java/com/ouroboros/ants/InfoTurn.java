package com.ouroboros.ants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public class InfoTurn implements InfoGame {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoTurn.class);

    List<Tile> water = new ArrayList<>();
    List<Tile> food = new ArrayList<>();
    List<TilePlayer> hill = new ArrayList<>();
    List<TilePlayer> liveAnts = new ArrayList<>();
    List<TilePlayer> deadAnts = new ArrayList<>();

    boolean gameEnd;

    InfoTurn(List<String> info, GameStates gameStates) {
        try {
            for (String line : info) {
                LOGGER.debug("received map info {}", line);

                String tokens[] = line.split(" ");
                if (tokens.length > 2) {
                    int y = Integer.parseInt(tokens[1]);
                    int x = Integer.parseInt(tokens[2]);

                    switch (tokens[0]) {
                        case "w":
                            water.add(gameStates.tiles[x][y]);
                            break;
                        case "f":
                            food.add(gameStates.tiles[x][y]);
                            break;
                        case "h":
                            hill.add(new TilePlayer(gameStates.tiles[x][y], Integer.parseInt(tokens[3])));
                            break;
                        case "a":
                            liveAnts.add(new TilePlayer(gameStates.tiles[x][y], Integer.parseInt(tokens[3])));
                            break;
                        case "d":
                            deadAnts.add(new TilePlayer(gameStates.tiles[x][y], Integer.parseInt(tokens[3])));
                            break;
                        default:
                            LOGGER.debug("unrecognized turn info {}", line);
                            break;
                    }
                } else if (tokens.length == 1 && "end".equals(tokens[0])) {
                    gameEnd = true;
                    break;
                }
            }

            if (gameEnd) {
                info.forEach(LOGGER::info);
            }

        } catch (NumberFormatException ex) {
            LOGGER.error("failed to process turn info", ex);
        }
    }
}
