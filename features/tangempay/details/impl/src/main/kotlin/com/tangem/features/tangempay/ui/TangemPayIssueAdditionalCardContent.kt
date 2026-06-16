package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButtonIconEnd
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayIssueAdditionalCardUM

@Composable
internal fun TangemPayIssueAdditionalCardContent(state: TangemPayIssueAdditionalCardUM) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.onDismiss,
        containerColor = TangemTheme.colors.background.secondary,
        title = {
            TangemModalBottomSheetTitle(
                endIconRes = R.drawable.ic_close_24,
                onEndClick = state.onDismiss,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CardIcon()
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = stringResourceSafe(R.string.tangempay_issue_additional_card_title),
                style = TangemTheme.typography.h3,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResourceSafe(R.string.tangempay_issue_additional_card_description),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
            )
            FeeRow(modifier = Modifier.padding(top = 16.dp), state = state)
            if (state.isBalanceInsufficient) {
                InsufficientFundsBlock(modifier = Modifier.padding(top = 8.dp), onAddFundsClick = state.onAddFundsClick)
            }
            PrimaryButton(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.tangempay_issue_card),
                showProgress = state.isLoading,
                enabled = !state.isLoading && !state.isBalanceInsufficient,
                onClick = state.onIssueClick,
            )
        }
    }
}

@Composable
private fun CardIcon() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(
                color = TangemTheme.colors.icon.accent.copy(alpha = 0.1f),
                shape = CircleShape,
            )
            .size(48.dp),
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_credit_card_add_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
    }
}

@Composable
private fun FeeRow(state: TangemPayIssueAdditionalCardUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.action)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResourceSafe(R.string.tangempay_issue_additional_card_fee_label),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = state.feeText,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun InsufficientFundsBlock(onAddFundsClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.action)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.img_usdc_16),
                contentDescription = null,
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = stringResourceSafe(R.string.tangempay_reissue_card_insufficient_funds_title),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = stringResourceSafe(
                        R.string.tangempay_reissue_card_insufficient_funds_subtitle,
                    ),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
        SecondaryButtonIconEnd(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            text = stringResourceSafe(R.string.common_add_funds),
            iconResId = R.drawable.ic_plus_24,
            onClick = onAddFundsClick,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayIssueAdditionalCardContentPreview(
    @PreviewParameter(IssueAdditionalCardPreviewProvider::class) state: TangemPayIssueAdditionalCardUM,
) {
    TangemThemePreview {
        TangemPayIssueAdditionalCardContent(state = state)
    }
}

private class IssueAdditionalCardPreviewProvider : PreviewParameterProvider<TangemPayIssueAdditionalCardUM> {
    override val values: Sequence<TangemPayIssueAdditionalCardUM>
        get() = sequenceOf(
            TangemPayIssueAdditionalCardUM(
                isBalanceInsufficient = false,
                feeText = "10 $",
                isLoading = false,
                onIssueClick = {},
                onAddFundsClick = {},
                onDismiss = {},
            ),
            TangemPayIssueAdditionalCardUM(
                isBalanceInsufficient = true,
                feeText = "10 $",
                isLoading = false,
                onIssueClick = {},
                onAddFundsClick = {},
                onDismiss = {},
            ),
            TangemPayIssueAdditionalCardUM(
                isBalanceInsufficient = false,
                feeText = "10 $",
                isLoading = true,
                onIssueClick = {},
                onAddFundsClick = {},
                onDismiss = {},
            ),
        )
}