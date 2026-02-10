package com.tangem.tap

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.tangem.core.navigation.review.ReviewManager
import timber.log.Timber
import com.google.android.play.core.review.ReviewManager as GReviewManager

/**
 * Implementation of [ReviewManager] for Google Play Store.
 */
internal class GoogleReviewManager : ReviewManager {

    override fun request(onDismissClick: () -> Unit) {
        foregroundActivityObserver.withForegroundActivity { activity ->
            val reviewManager = ReviewManagerFactory.create(activity)
            val requestTask = reviewManager.requestReviewFlow()
            requestTask
                .addOnCompleteListener { task ->
                    handleOnCompleteRequestTask(
                        reviewManager = reviewManager,
                        activity = activity,
                        task = task,
                        onDismissClick = onDismissClick,
                    )
                }
                .addOnFailureListener(Timber::e)
        }
    }

    private fun handleOnCompleteRequestTask(
        reviewManager: GReviewManager,
        activity: Activity,
        task: Task<ReviewInfo>,
        onDismissClick: () -> Unit,
    ) {
        if (task.isSuccessful) {
            val reviewFlow = reviewManager.launchReviewFlow(activity, task.result)
            reviewFlow
                .addOnCompleteListener { resultReviewTask ->
                    if (!resultReviewTask.isSuccessful) onDismissClick()
                }
                .addOnFailureListener(Timber::e)
        } else {
            Timber.e(task.exception)
        }
    }
}