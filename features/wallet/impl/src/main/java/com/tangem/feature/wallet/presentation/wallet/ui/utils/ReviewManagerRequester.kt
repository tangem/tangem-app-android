package com.tangem.feature.wallet.presentation.wallet.ui.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import timber.log.Timber

internal object ReviewManagerRequester {

    fun request(context: Context, onDismissClick: () -> Unit) {
        val reviewManager = ReviewManagerFactory.create(context)
        val requestTask = reviewManager.requestReviewFlow()

        requestTask
            .addOnCompleteListener {
                handleOnCompleteRequestTask(
                    reviewManager = reviewManager,
                    activity = context.findActivity(),
                    task = it,
                    onDismissClick = onDismissClick,
                )
            }
            .addOnFailureListener(Timber::e)
    }

    private fun handleOnCompleteRequestTask(
        reviewManager: ReviewManager,
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

    private fun Context.findActivity(): Activity {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        error("Permissions should be called in the context of an Activity")
    }
}