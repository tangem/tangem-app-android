package com.tangem.feature.wallet.presentation.wallet.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.feature.wallet.presentation.wallet.state.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import timber.log.Timber

@Composable
internal fun WalletEventEffect(
    walletsListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    event: StateEvent<WalletEvent>,
    onAutoScrollSet: () -> Unit,
    onAlertConfigSet: (WalletAlertState) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalContext.current.resources
    val clipboardManager = LocalClipboardManager.current
    EventEffect(
        event = event,
        onTrigger = { value ->
            when (value) {
                is WalletEvent.ChangeWallet -> {
                    onAutoScrollSet()
                    walletsListState.animateScrollToItem(index = value.index)
                }
                is WalletEvent.ShowError -> {
                    snackbarHostState.showSnackbar(message = value.text.resolveReference(resources))
                }
                is WalletEvent.ShowToast -> {
                    Toast.makeText(context, value.text.resolveReference(resources), Toast.LENGTH_SHORT).show()
                }
                is WalletEvent.CopyAddress -> {
                    clipboardManager.setText(AnnotatedString(value.address))
                }
                is WalletEvent.ShowWalletAlreadySignedHashesMessage -> {
                    onAlertConfigSet(
                        WalletAlertState.WalletAlreadySignedHashes(onUnderstandClick = value.onUnderstandClick),
                    )
                }
                is WalletEvent.RateApp -> {
                    val reviewManager = ReviewManagerFactory.create(context)
                    val requestTask = reviewManager.requestReviewFlow()

                    requestTask
                        .addOnCompleteListener {
                            handleOnCompleteRequestTask(
                                reviewManager = reviewManager,
                                activity = context as? Activity ?: return@addOnCompleteListener,
                                task = it,
                                onDismissClick = value.onDismissClick,
                            )
                        }
                        .addOnFailureListener(Timber::e)
                }
            }
        },
    )
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
