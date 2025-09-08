package com.tangem.features.yieldlending.impl.subcomponents.startearning.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.expressStatus.ExpressStatusStep
import com.tangem.common.ui.expressStatus.state.ExpressStatusItemState
import com.tangem.common.ui.expressStatus.state.ExpressStatusItemUM
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yieldlending.impl.R
import com.tangem.features.yieldlending.impl.subcomponents.startearning.entity.EnterStep
import com.tangem.features.yieldlending.impl.subcomponents.startearning.entity.EnterStepType
import com.tangem.features.yieldlending.impl.subcomponents.startearning.entity.YieldLendingStartEarningContentUM
import com.tangem.features.yieldlending.impl.subcomponents.startearning.entity.YieldLendingStartEarningUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun YieldLendingStartEarningBottomSheet(config: TangemBottomSheetConfig, onClick: () -> Unit) {
    TangemModalBottomSheetWithFooter<YieldLendingStartEarningContentUM>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            AnimatedContent(
                targetState = config.content,
            ) { content ->
                when (content) {
                    is YieldLendingStartEarningContentUM.Main -> TangemModalBottomSheetTitle(
                        endIconRes = R.drawable.ic_close_24,
                        onEndClick = config.onDismissRequest,
                    )
                    is YieldLendingStartEarningContentUM.FeePolicy -> TangemModalBottomSheetTitle(
                        startIconRes = R.drawable.ic_back_24,
                        onEndClick = config.onDismissRequest,
                    )
                }

            }
        },
        footer = {
            val text = when (val content = config.content) {
                is YieldLendingStartEarningContentUM.Main -> content.currentStepType.title
                else -> TextReference.EMPTY
            }
            PrimaryButton(
                text = text.resolveReference(),
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        },
        content = { contentConfig ->
            YieldLendingStartEarningContent(contentConfig)
        },
    )
}

@Composable
private fun YieldLendingStartEarningContent(contentConfig: YieldLendingStartEarningContentUM) {
    AnimatedContent(
        targetState = contentConfig,
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
    ) { targetState ->
        when (targetState) {
            is YieldLendingStartEarningContentUM.Main -> StartEarningMainContent(targetState)
            is YieldLendingStartEarningContentUM.FeePolicy -> TODO()
        }
    }
}

@Composable
private fun StartEarningMainContent(state: YieldLendingStartEarningContentUM.Main) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        CurrencyIcon(
            state = state.currencyIconState,
            shouldDisplayNetwork = false,
            iconSize = 48.dp,
            modifier = Modifier
        )
        SpacerH24()
        Text(
            text = "Start earning",
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerH8()
        Text(
            text = "Your USDT will be supplied to Aave and will stay instantly available",
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
        SpacerH24()
        YieldStatusStep(state.steps)
        SpacerH24()
        FooterContainer(
            footer = stringReference("Your next deposits will be automatically supplied to Aave. See fee policy")
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TangemTheme.colors.background.action)
                    .padding(12.dp),
            ) {
                Text(
                    text = stringResourceSafe(R.string.common_network_fee_title),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.primary1,
                )
                SpacerWMax()
                if (state.fee == null) {
                    TextShimmer(
                        style = TangemTheme.typography.body1,
                        text = "0.00034 ETH • \$1.45",
                    )
                } else {
                    Text(
                        text = state.fee.amount.value.toString(),
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
            }
        }
    }
}

// TODO DEV ONLY
@Composable
private fun YieldStatusStep(steps: ImmutableList<EnterStep>) {
    Column(
        modifier = Modifier
            .background(TangemTheme.colors.background.action)
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = "DEV ONLY! For testing",
            color = TangemTheme.colors.text.attention,
        )
        SpacerH8()
        steps.forEachIndexed { index, step ->
            ExpressStatusStep(
                status = ExpressStatusItemUM(
                    text = step.stepType.title,
                    state = when {
                        step.isComplete -> ExpressStatusItemState.Done
                        step.isActive -> ExpressStatusItemState.Active
                        else -> ExpressStatusItemState.Default
                    }
                ),
                isLast = index == steps.lastIndex,
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YieldLendingStartEarningBottomSheet_Preview(
    @PreviewParameter(PreviewProvider::class) params: YieldLendingStartEarningUM,
) {
    TangemThemePreview {
        YieldLendingStartEarningBottomSheet(params.bottomSheetConfig, onClick = {})
    }
}

private class PreviewProvider : PreviewParameterProvider<YieldLendingStartEarningUM> {
    override val values: Sequence<YieldLendingStartEarningUM>
        get() = sequenceOf(
            YieldLendingStartEarningUM(
                bottomSheetConfig = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {},
                    content = YieldLendingStartEarningContentUM.Main(
                        currencyIconState = CurrencyIconState.Loading,
                        fee = null,
                        currentStepType = EnterStepType.Deploy,
                        steps = persistentListOf(
                            EnterStep(
                                stepType = EnterStepType.Deploy,
                                isActive = true,
                                isComplete = false,
                            ),
                            EnterStep(
                                stepType = EnterStepType.Approve,
                                isActive = false,
                                isComplete = false,
                            ),
                            EnterStep(
                                stepType = EnterStepType.Enter,
                                isActive = false,
                                isComplete = false,
                            )
                        ),
                    )
                )
            )
        )
}
// endregion