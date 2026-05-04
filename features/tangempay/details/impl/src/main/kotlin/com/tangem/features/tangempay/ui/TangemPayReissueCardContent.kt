package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayReissueCardError
import com.tangem.features.tangempay.entity.TangemPayReissueCardUM

@Composable
internal fun TangemPayReissueCardContent(state: TangemPayReissueCardUM) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismissRequest,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        onBack = state.onDismissRequest,
        title = {
            TangemModalBottomSheetTitle(
                title = TextReference.EMPTY,
                endIconRes = R.drawable.ic_close_24,
                onEndClick = state.onDismissRequest,
            )
        },
    ) {
        Content(state)
    }
}

@Composable
private fun Content(state: TangemPayReissueCardUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(TangemTheme.dimens.spacing56)
                .clip(CircleShape)
                .background(TangemTheme.colors.icon.accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_update_32),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
                modifier = Modifier.size(32.dp),
            )
        }

        SpacerH24()

        Text(
            text = stringResourceSafe(R.string.tangempay_reissue_card_title),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        SpacerH8()

        Text(
            text = stringResourceSafe(R.string.tangempay_reissue_card_description),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        SpacerH24()

        FeeBlock(state)

        if (state.error != null) {
            SpacerH16()
            ErrorBlock(
                error = state.error,
                onRetryFee = state.onRetryFee,
                onAddFundsClick = state.onAddFundsClick,
            )
        }

        SpacerH24()

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(R.string.tangempay_reissue_card_confirm),
            enabled = state.error == null && !state.isFeeLoading,
            showProgress = state.isReissuingInProgress,
            onClick = state.onConfirmClick,
        )

        SpacerH16()
    }
}

@Composable
private fun FeeBlock(state: TangemPayReissueCardUM) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors.background.action,
                shape = TangemTheme.shapes.roundedCornersMedium,
            )
            .padding(TangemTheme.dimens.spacing12),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResourceSafe(R.string.tangempay_reissue_card_fee_label),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        AnimatedContent(
            targetState = when {
                state.isFeeLoading -> null
                state.error == TangemPayReissueCardError.InitialDataLoading -> "—"
                else -> state.feeAmount
            },
        ) { fee ->
            if (fee != null) {
                Text(
                    text = fee,
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.tertiary,
                )
            } else {
                TextShimmer(style = TangemTheme.typography.body1, text = "$0.00")
            }
        }
    }
}

@Composable
private fun ErrorBlock(error: TangemPayReissueCardError, onAddFundsClick: () -> Unit, onRetryFee: () -> Unit) {
    when (error) {
        TangemPayReissueCardError.InsufficientFunds -> Notification(
            config = NotificationConfig(
                title = resourceReference(R.string.tangempay_reissue_card_insufficient_funds_title),
                subtitle = resourceReference(R.string.tangempay_reissue_card_insufficient_funds_subtitle),
                iconResId = R.drawable.img_usdc_16,
                buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = resourceReference(R.string.tangempay_card_details_add_funds),
                    iconResId = R.drawable.ic_plus_24,
                    onClick = onAddFundsClick,
                ),
            ),
            containerColor = TangemTheme.colors.background.action,
            modifier = Modifier.fillMaxWidth(),
        )
        TangemPayReissueCardError.InitialDataLoading -> Notification(
            config = NotificationConfig(
                title = resourceReference(R.string.tangempay_reissue_card_fee_unreachable_error_title),
                subtitle = resourceReference(R.string.send_fee_unreachable_error_text),
                iconResId = R.drawable.img_attention_20,
                buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = resourceReference(R.string.warning_button_refresh),
                    onClick = onRetryFee,
                ),
            ),
            containerColor = TangemTheme.colors.background.action,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun preview() = TangemThemePreview {
    TangemPayReissueCardContent(
        state = TangemPayReissueCardUM.stub(),
    )
}