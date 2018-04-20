package com.ouroboros.ants.utils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by zhanxies on 4/4/2018.
 *
 */
public class Utils {

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static int distEucl2(int x1, int y1, int x2, int y2, int xt, int yt) {
        int dx = dist1D(x1, x2, xt);
        int dy = dist1D(y1, y2, yt);
        return dx * dx + dy * dy;
    }

    public static int distManh(int x1, int y1, int x2, int y2, int xt, int yt) {
        int dx = dist1D(x1, x2, xt);
        int dy = dist1D(y1, y2, yt);
        return dx + dy;
    }

    public static int dist1D(int c1, int c2, int xt) {
        int d = c1 - c2;
        return Math.min(Math.abs(d), xt - Math.abs(d));
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

    public static <T> T getRandomElement(List<T> list) {
        if (list.size() == 1) {
            return list.get(0);
        }

        int i = ThreadLocalRandom.current().nextInt(list.size());
        return list.get(i);
    }

    public static <T, U> U getSecondArg(T arg1, U arg2) {
        return arg2;
    }
}
