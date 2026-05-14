package com.example.tictactoeapplication.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory

fun showRateUs(context: Context?) {
    if (context == null) return
    val activity = context as? Activity ?: return

    val manager = ReviewManagerFactory.create(activity)
    val request = manager.requestReviewFlow()

    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val reviewInfo = task.result
            val flow = manager.launchReviewFlow(activity, reviewInfo)
            flow.addOnCompleteListener {
                // User finished the Play Store rating dialog
            }
        } else {
            // If Play refuses → open Play Store app page instead
            openPlayStore(activity)
        }
    }
}

 fun openPlayStore(activity: Activity) {
    val appPackageName = activity.packageName
    try {
        activity.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
        )
    } catch (e: ActivityNotFoundException) {
        activity.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName"))
        )
    }
}



