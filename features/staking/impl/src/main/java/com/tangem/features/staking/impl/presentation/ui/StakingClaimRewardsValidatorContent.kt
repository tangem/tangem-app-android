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
import com.tangem.core.ui.components.inputrow.InputRowImageInfo
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents
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
            key(item.title.resolveReference() + index) {
                InputRowImageInfo(
                    subtitle = item.title,
                    caption = combinedReference(
                        resourceReference(R.string.staking_details_apr),
                        annotatedReference {
                            appendSpace()
                            appendColored(
                                text = item.validator?.apr.orZero().format { percent() },
                                color = TangemTheme.colors.text.accent,
                            )
                        },
                    ),
                    infoTitle = item.formattedFiatAmount,
                    infoSubtitle = item.formattedCryptoAmount,
                    imageUrl = item.validator?.image.orEmpty(),
                    onImageError = { ValidatorImagePlaceholder() },
                    modifier = modifier
                        .roundedShapeItemDecoration(index, state.rewards.lastIndex, false)
                        .background(TangemTheme.colors.background.action)
                        .clickable(
                            enabled = item.pendingActions.isNotEmpty(),
                            onClick = {
                                clickIntents.onActiveStake(item)
                            },
                        ),
                )
            }
        }
    }
}