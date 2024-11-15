package com.capztone.admin.utils



import android.content.res.Resources

// Extension function to convert DP to pixels
fun Int.dpToPx(): Int {
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}
