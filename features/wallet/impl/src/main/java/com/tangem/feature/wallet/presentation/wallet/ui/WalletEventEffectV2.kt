package com.tangem.feature.wallet.presentation.wallet.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent.DemonstrateWalletsScrollPreview.Direction
import com.tangem.feature.wallet.presentation.wallet.ui.utils.animateScrollByIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

private const val VISIBLE_PART_OF_WALLET_CARD = 0.2f

@Suppress("LongParameterList")
@Composable
internal fun WalletEventEffectV2(
    walletsListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    event: StateEvent<WalletEvent>,
    selectedWalletIndex: Int,
    onAutoScrollSet: () -> Unit,
    onAlertConfigSet: (WalletAlertState) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalContext.current.resources
    val clipboardManager = LocalClipboardManager.current
    EventEffect(
        event = event,
        onTrigger = { value ->
            when (value) {
                is WalletEvent.ChangeWallet -> {
                    onAutoScrollSet()
                    walletsListState.animateScrollByIndex(prevIndex = selectedWalletIndex, newIndex = value.index)
                }
                is WalletEvent.ShowError -> {
                    snackbarHostState.showSnackbar(message = value.text.resolveReference(resources))
                }
                is WalletEvent.ShowToast -> {
                    Toast.makeText(context, value.text.resolveReference(resources), Toast.LENGTH_SHORT).show()
                }
                is WalletEvent.CopyAddress -> {
                    clipboardManager.setText(AnnotatedString(value.address))
                    Toast.makeText(context, value.toast.resolveReference(resources), Toast.LENGTH_SHORT).show()
                }
                is WalletEvent.ShowAlert -> onAlertConfigSet(value.state)
                is WalletEvent.RateApp -> {
                    val reviewManager = ReviewManagerFactory.create(context)
                    val requestTask = reviewManager.requestReviewFlow()

                    requestTask
                        .addOnCompleteListener {
                            handleOnCompleteRequestTask(
                                reviewManager = reviewManager,
                                activity = context.findActivity(),
                                task = it,
                                onDismissClick = value.onDismissClick,
                            )
                        }
                        .addOnFailureListener(Timber::e)
                }
                is WalletEvent.DemonstrateWalletsScrollPreview -> {
                    walletsListState.demonstrateScrolling(
                        coroutineScope = coroutineScope,
                        direction = value.direction,
                    )
                }
            }
        },
    )
}

private fun LazyListState.demonstrateScrolling(coroutineScope: CoroutineScope, direction: Direction) {
    coroutineScope.launch {
        animateScrollBy(
            value = calculateOffset(direction = direction, isReverse = false),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        )
    }
        .invokeOnCompletion {
            coroutineScope.launch {
                animateScrollBy(
                    value = calculateOffset(direction = direction, isReverse = true),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
        }
}

private fun LazyListState.calculateOffset(direction: Direction, isReverse: Boolean): Float {
    val sign = when (direction) {
        Direction.LEFT -> 1
        Direction.RIGHT -> -1
    }.times(other = if (isReverse) -1 else 1)

    return layoutInfo.viewportSize.width.toFloat() * VISIBLE_PART_OF_WALLET_CARD * sign
}

private fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    error("Permissions should be called in the context of an Activity")
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