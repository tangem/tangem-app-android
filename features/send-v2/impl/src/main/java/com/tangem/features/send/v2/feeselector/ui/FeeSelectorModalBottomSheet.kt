package com.tangem.features.send.v2.feeselector.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.model.FeeSelectorIntents
import com.tangem.features.send.v2.feeselector.route.FeeSelectorRoute
import com.tangem.features.send.v2.impl.R

@Suppress("LongParameterList")
@Composable
internal fun FeeSelectorModalBottomSheet(
    childStack: ChildStack<FeeSelectorRoute, ComposableContentComponent>,
    state: FeeSelectorUM,
    feeSelectorIntents: FeeSelectorIntents,
    feeDisplaySource: FeeSelectorParams.FeeDisplaySource,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
) {
    if (state !is FeeSelectorUM.Content) return

    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        onBack = if (childStack.backStack.isNotEmpty()) onBack else null,
        title = {
            FeeTitle(
                childStack = childStack,
                feeDisplaySource = feeDisplaySource,
                onBack = onBack,
            )
        },
        content = {
            Content(childStack = childStack)
        },
        footer = when {
            childStack.active.configuration is FeeSelectorRoute.NetworkFee -> {
                {
                    PrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        enabled = state.isPrimaryButtonEnabled,
                        text = stringResourceSafe(R.string.common_confirm),
                        onClick = feeSelectorIntents::onDoneClick,
                    )
                }
            }
            childStack.active.configuration is FeeSelectorRoute.ChooseSpeed &&
                state.selectedFeeItem is FeeItem.Custom -> {
                {
                    PrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        enabled = state.isPrimaryButtonEnabled,
                        text = stringResourceSafe(R.string.common_done),
                        onClick = feeSelectorIntents::onDoneClick,
                    )
                }
            }
            else -> null
        },
    )
}

@Composable
private fun FeeTitle(
    childStack: ChildStack<FeeSelectorRoute, ComposableContentComponent>,
    feeDisplaySource: FeeSelectorParams.FeeDisplaySource,
    onBack: () -> Unit,
) {
    Children(
        stack = childStack,
        animation = stackAnimation(fade(animationSpec = tween(durationMillis = 100))),
    ) { current ->
        when (feeDisplaySource) {
            FeeSelectorParams.FeeDisplaySource.Screen -> {
                if (childStack.backStack.isNotEmpty()) {
                    TangemModalBottomSheetTitle(
                        title = current.configuration.title,
                        startIconRes = R.drawable.ic_back_24,
                        onStartClick = onBack,
                    )
                } else {
                    TangemModalBottomSheetTitle(
                        title = current.configuration.title,
                        endIconRes = R.drawable.ic_close_24,
                        onEndClick = onBack,
                    )
                }
            }
            FeeSelectorParams.FeeDisplaySource.BottomSheet -> {
                TangemModalBottomSheetTitle(
                    title = current.configuration.title,
                    startIconRes = R.drawable.ic_back_24,
                    onStartClick = onBack,
                )
            }
        }
    }
}

@Composable
private fun Content(
    childStack: ChildStack<FeeSelectorRoute, ComposableContentComponent>,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = childStack,
        animation = stackAnimation(fade(animationSpec = tween(durationMillis = 100))),
        modifier = modifier
            .fillMaxSize()
            .animateContentSize(),
    ) {
        it.instance.Content(Modifier.fillMaxSize())
    }
}