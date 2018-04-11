package com.ouroboros.ants.utils;

/**
 * Created by zhanxies on 4/4/2018.
 *
 */
public class Utils {

    public static int dist(int x1, int y1, int x2, int y2, int xt, int yt) {
        int dx = Math.min(Math.abs(x1 - x2), xt - Math.abs(x1 - x2));
        int dy = Math.min(Math.abs(y1- y2), yt - Math.abs(y1 - y2));
        return dx * dx + dy * dy;
    }

    public static int nc(int x, int xt) {
        if (x < 0) {
            return xt + x;
        } else if (x >= xt) {
            return x - xt;
        } else {
            return x;
        }
    }


}
