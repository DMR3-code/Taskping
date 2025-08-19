package com.s23010301.taskping.utils;

import android.content.Context;

import androidx.annotation.ArrayRes;

public class ColorUtils {

    public static int getStableColorAvoidRepeat(Context context, String taskId, int position, @ArrayRes int colorArrayRes, int lastColorIndex) {
        int[] colors = context.getResources().getIntArray(colorArrayRes);

        // Base stable index from ID
        int colorIndex = Math.abs(taskId.hashCode()) % colors.length;

        // If this task’s color matches previous card’s color, shift to next
        if (position > 0 && colorIndex == lastColorIndex) {
            colorIndex = (colorIndex + 1) % colors.length;
        }

        return colors[colorIndex];
    }
}
