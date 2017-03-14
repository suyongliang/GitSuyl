package com.huaxia.hpn.utils;

import android.content.Context;

/**
 * Created by hx-suyl on 2017/3/13.
 */

public class DisplayUtils {
    public static int pixelToDp(final Context pContext, final int pPixels)
    {
        final float density = pContext.getResources().getDisplayMetrics().density;

        return (int) ((pPixels / density) + 0.5);
    }

    public static int dpToPixel(final Context pContext, final int pDp)
    {
        final float density = pContext.getResources().getDisplayMetrics().density;

        return (int) ((pDp * density) + 0.5f);
    }
}
