package com.ouroboros.ants.strategy.xy;

import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.game.xy.XYTileMv;
import com.ouroboros.ants.utils.xy.TreeSearch;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhanxies on 5/15/2018.
 *
 */
public class XYExploreTask {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(XYExploreTask.class);

    private XYTile ant;
    private XYTile goal;
    private int stepCnt;
    private List<XYTileMv> steps;

    XYExploreTask(XYTile ant, XYTile goal) {
        this.ant = ant;
        this.goal = goal;
        this.stepCnt = 17;
        this.steps = new ArrayList<>();

        plan();
    }

    void plan() {
        LOGGER.info("explore ant: {}", ant);
        LOGGER.info("explore goal: {}", goal);
        Set<XYTile> searched = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Map<XYTileMv, XYTileMv> start = new HashMap<>();
        goal.getNbDir().parallelStream().forEach(t -> start.put(t, null));
        TreeSearch.breadthFirstLink(start, ant, t -> searched.add(t), steps);
        LOGGER.info("explore links: {}", steps);
    }

    XYTileMv getMove() {
        return steps.get(0);
    }

    void move() {
        stepCnt--;
        steps.remove(0);
    }

    boolean end() {
        return steps.isEmpty() || stepCnt == 0;
    }
}
