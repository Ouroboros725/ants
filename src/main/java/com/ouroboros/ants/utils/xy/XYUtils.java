package com.ouroboros.ants.utils.xy;

import com.ouroboros.ants.game.xy.XYTile;
import com.ouroboros.ants.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Created by zhanxies on 5/26/2018.
 *
 */
public class XYUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(XYUtils.class);

    public static XYTile findCenter(Collection<XYTile> tiles) {
        if (tiles.isEmpty()) {
            return null;
        } else if (tiles.size() == 1) {
            XYTile center = tiles.iterator().next();
            LOGGER.info("calculated center: {}", center);
            return center;
        } else {
            XYTile tile0 = tiles.iterator().next();
            int x0 = tile0.getX();
            int y0 = tile0.getY();
            int xc = tiles.parallelStream().mapToInt(t -> Utils.dnc(t.getX(), x0, XYTile.getXt())).sum() / tiles.size();
            int yc = tiles.parallelStream().mapToInt(t -> Utils.dnc(t.getY(), y0, XYTile.getYt())).sum() / tiles.size();
            XYTile center = XYTile.getTile(Utils.nc(xc, XYTile.getXt()), Utils.nc(yc, XYTile.getYt()));
            LOGGER.info("calculated center: {}", center);
            return center;
        }
    }
}
