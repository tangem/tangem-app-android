package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.preview.AmountStatePreviewData
import com.tangem.common.ui.amountScreen.ui.AmountBlock
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.TransactionDoneState
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmationStatePreviewData
import com.tangem.features.staking.impl.presentation.state.previewdata.ValidatorStatePreviewData
import com.tangem.features.staking.impl.presentation.state.stub.StakingClickIntentsStub
import com.tangem.features.staking.impl.presentation.ui.block.NotificationsBlock
import com.tangem.features.staking.impl.presentation.ui.block.StakingFeeBlock
import com.tangem.features.staking.impl.presentation.ui.block.ValidatorBlock
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents

@Suppress("LongParameterList")
@Composable
internal fun StakingConfirmationContent(
    amountState: AmountState,
    state: StakingStates.ConfirmationState,
    validatorState: StakingStates.ValidatorState,
    clickIntents: StakingClickIntents,
) {
    if (state !is StakingStates.ConfirmationState.Data) return
    val isTransactionSent = state.innerState == InnerConfirmationStakingState.COMPLETED
    val isTransactionInProgress = state.notifications.any { it is StakingNotification.Warning.TransactionInProgress }
    Column(
        modifier = Modifier
            .background(TangemTheme.colors.background.secondary)
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        val doneState = state.transactionDoneState
        AnimatedVisibility(
            visible = doneState is TransactionDoneState.Content,
            modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing12),
        ) {
            TransactionDoneTitle(
                title = resourceReference(R.string.common_in_progress),
                subtitle = resourceReference(R.string.staking_transaction_in_progress_text),
            )
        }
        AmountBlock(
            amountState = amountState,
            isClickDisabled = !state.isAmountEditable || isTransactionSent || isTransactionInProgress,
            isEditingDisabled = !state.isAmountEditable && state.innerState != InnerConfirmationStakingState.COMPLETED,
            onClick = clickIntents::onPrevClick,
        )
        ValidatorBlock(
            validatorState = validatorState,
            isClickable = !isTransactionInProgress,
            onClick = clickIntents::openValidators,
        )
        StakingFeeBlock(feeState = state.feeState, isTransactionSent = isTransactionSent)
        NotificationsBlock(notifications = state.notifications)
        Spacer(Modifier)
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_StakingConfirmationContent() {
    TangemThemePreview {
        Column(Modifier.background(TangemTheme.colors.background.primary)) {
            StakingConfirmationContent(
                amountState = AmountStatePreviewData.amountState,
                state = ConfirmationStatePreviewData.assentStakingState,
                validatorState = ValidatorStatePreviewData.validatorState,
                clickIntents = StakingClickIntentsStub,
            )
        }
    }
}