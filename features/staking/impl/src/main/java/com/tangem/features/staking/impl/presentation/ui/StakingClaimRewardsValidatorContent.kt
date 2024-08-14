package com.tangem.features.staking.impl.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.tangem.core.ui.components.inputrow.InputRowImageInfo
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import com.tangem.utils.extensions.orZero

@Composable
internal fun StakingClaimRewardsValidatorContent(
    state: StakingStates.RewardsValidatorsState,
    clickIntents: StakingClickIntents,
    modifier: Modifier = Modifier,
) {
    if (state !is StakingStates.RewardsValidatorsState.Data) return
    Column(
        modifier = Modifier // Do not put fillMaxSize() in here
            .background(TangemTheme.colors.background.secondary)
            .padding(horizontal = TangemTheme.dimens.spacing12)
            .verticalScroll(rememberScrollState()),
    ) {
        state.rewards.forEachIndexed { index, item ->
            key(item.validator.address) {
                InputRowImageInfo(
                    subtitle = stringReference(item.validator.name),
                    caption = combinedReference(
                        resourceReference(R.string.staking_details_apr),
                        annotatedReference(
                            buildAnnotatedString {
                                appendSpace()
                                withStyle(SpanStyle(color = TangemTheme.colors.text.accent)) {
                                    append(
                                        BigDecimalFormatter.formatPercent(
                                            percent = item.validator.apr.orZero(),
                                            useAbsoluteValue = true,
                                        ),
                                    )
                                }
                            },
                        ),
                    ),
                    infoTitle = item.fiatAmount,
                    infoSubtitle = item.cryptoAmount,
                    imageUrl = item.validator.image.orEmpty(),
                    modifier = modifier
                        .roundedShapeItemDecoration(index, state.rewards.lastIndex, false)
                        .background(TangemTheme.colors.background.action)
                        .clickable(
                            onClick = {
                                clickIntents.onActiveStake(item)
                            },
                        ),
                )
            }
        }
    }
}
