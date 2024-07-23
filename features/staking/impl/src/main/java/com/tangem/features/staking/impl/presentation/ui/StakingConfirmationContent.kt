package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.preview.AmountStatePreviewData
import com.tangem.common.ui.amountScreen.ui.AmountBlock
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.RouteType
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.TransactionDoneState
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmationStatePreviewData
import com.tangem.features.staking.impl.presentation.state.stub.StakingClickIntentsStub
import com.tangem.features.staking.impl.presentation.ui.block.NotificationsBlock
import com.tangem.features.staking.impl.presentation.ui.block.StakingFeeBlock
import com.tangem.features.staking.impl.presentation.ui.block.ValidatorBlock
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents

@Composable
internal fun StakingConfirmationContent(
    amountState: AmountState,
    state: StakingStates.ConfirmationState,
    clickIntents: StakingClickIntents,
    type: RouteType,
) {
    if (state !is StakingStates.ConfirmationState.Data) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.tertiary)
            .padding(TangemTheme.dimens.spacing16)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        AnimatedVisibility(
            visible = state.transactionDoneState is TransactionDoneState.Content,
            modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing12),
        ) {
            val transactionDoneStateContent = state.transactionDoneState as TransactionDoneState.Content
            TransactionDoneTitle(
                titleRes = R.string.sent_transaction_sent_title,
                date = transactionDoneStateContent.timestamp,
            )
        }
        AmountBlock(
            amountState = amountState,
            isClickDisabled = true,
            isEditingDisabled = true,
            onClick = {},
        )
        if (type == RouteType.STAKE) {
            ValidatorBlock(validatorState = state.validatorState, onClick = clickIntents::openValidators)
        }
        StakingFeeBlock(feeState = state.feeState)
        NotificationsBlock(notifications = state.notifications)
        SpacerHMax()
        FooterText(text = state.footerText)
    }
}

@Composable
private fun FooterText(text: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = text,
        color = TangemTheme.colors.text.tertiary,
        style = TangemTheme.typography.caption2,
        textAlign = TextAlign.Center,
    )
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
                clickIntents = StakingClickIntentsStub,
                type = RouteType.STAKE,
            )
        }
    }
}