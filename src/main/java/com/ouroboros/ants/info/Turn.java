package com.ouroboros.ants.info;

import com.ouroboros.ants.game.Tile;
import com.ouroboros.ants.game.TilePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanxies on 3/30/2018.
 *
 */
public class Turn implements Game {

    private static final Logger LOGGER = LoggerFactory.getLogger(Turn.class);

    public List<Tile> water = new ArrayList<>();
    public List<Tile> food = new ArrayList<>();
    public List<Tile> myHills = new ArrayList<>();
    public List<Tile> oppHills = new ArrayList<>();
//    public List<TilePlayer> hill = new LinkedList<>();
    public List<Tile> myAnts = new ArrayList<>();
    public List<Tile> oppAnts = new ArrayList<>();
//    public List<TilePlayer> liveAnts = new LinkedList<>();
    public List<TilePlayer> deadAnts = new ArrayList<>();

    public boolean gameEnd;

    public Turn(List<String> info) {
        try {
            for (String line : info) {
                LOGGER.debug("received map info {}", line);

                String tokens[] = line.split(" ");
                if (tokens.length > 2) {
                    int y = Integer.parseInt(tokens[1]);
                    int x = Integer.parseInt(tokens[2]);

                    switch (tokens[0]) {
                        case "w":
                            water.add(Tile.getTile(x, y));
                            break;
                        case "f":
                            food.add(Tile.getTile(x, y));
                            break;
                        case "h":
                            int ph = Integer.parseInt(tokens[3]);
                            if (ph == 0) {
                                myHills.add(Tile.getTile(x, y));
                            } else {
                                oppHills.add(Tile.getTile(x, y));
                            }
//                            hill.add(new TilePlayer(Tile.getTile(x, y), p));
                            break;
                        case "a":
                            int pa = Integer.parseInt(tokens[3]);
                            if (pa == 0) {
                                myAnts.add(Tile.getTile(x, y));
                            } else {
                                oppAnts.add(Tile.getTile(x, y));
                            }
//                            liveAnts.add(new TilePlayer(Tile.getTile(x, y), p));
                            break;
                        case "d":
                            deadAnts.add(new TilePlayer(Tile.getTile(x, y), Integer.parseInt(tokens[3])));
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
