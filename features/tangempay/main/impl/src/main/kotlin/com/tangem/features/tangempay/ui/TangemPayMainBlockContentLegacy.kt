package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.inputrow.InputRowImageBase
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.features.tangempay.main.impl.R
import com.tangem.utils.StringsSigns.DASH_SIGN

private const val DISABLED_ALPHA = 0.6F

@Composable
internal fun TangemPayMainBlockItem(state: TangemPayMainUM, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayMainUM.Empty -> Unit
        is TangemPayMainUM.Loading -> TangemPayMainLoadingItem(modifier)
        is TangemPayMainUM.UnderReview -> TangemPayMainUnderReviewItem(state, modifier)
        is TangemPayMainUM.IssuingCard -> TangemPayMainIssuingCardItem(state, modifier)
        is TangemPayMainUM.FailedToIssue -> TangemPayMainFailedIssueItem(state, modifier)
        is TangemPayMainUM.Content -> TangemPayMainBlockContent(state, isBalanceHidden, modifier)
        is TangemPayMainUM.TemporaryUnavailable -> TangemPayMainTempUnavailableItem(modifier)
        is TangemPayMainUM.SyncNeeded -> TangemPayMainSyncNeededItem(modifier)
        is TangemPayMainUM.ExposedDevice -> TangemPayMainExposedDeviceItem(modifier)
    }
}

@Composable
private fun TangemPayMainBlockContent(
    state: TangemPayMainUM.Content,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = TangemTheme.shapes.roundedCornersXMedium,
        color = TangemTheme.colors.background.primary,
        onClick = state.onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.img_visa_36),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResourceSafe(R.string.tangempay_payment_account),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = state.subtitle.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.End,
            ) {
                TangemPayFiatAmount(
                    text = state.balance.resolveReference(),
                    isBalanceFlickering = state.isBalanceFlickering,
                    isBalanceFromCache = state.shouldShowOnlyCacheWarning,
                    isBalanceHidden = isBalanceHidden,
                )
                Text(
                    text = state.balanceSubtitle.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun TangemPayFiatAmount(
    text: String,
    isBalanceFlickering: Boolean,
    isBalanceFromCache: Boolean,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(isBalanceFromCache) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    modifier = Modifier.size(12.dp),
                    painter = painterResource(R.drawable.ic_error_sync_24),
                    tint = TangemTheme.colors.icon.inactive,
                    contentDescription = null,
                )
            }
        }

        Text(
            text = text.orMaskWithStars(isBalanceHidden),
            style = TangemTheme.typography.body2.applyBladeBrush(
                isEnabled = isBalanceFlickering,
                textColor = TangemTheme.colors.text.primary1,
            ),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun TangemPayMainUnderReviewItem(state: TangemPayMainUM.UnderReview, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier
            .clip(RoundedCornerShape(size = TangemTheme.dimens.radius14))
            .background(TangemTheme.colors.background.primary),
        onClick = state.onClick,
    ) {
        InputRowImageBase(
            modifier = Modifier
                .padding(
                    all = TangemTheme.dimens.spacing12,
                ),
            subtitle = resourceReference(R.string.tangempay_payment_account),
            caption = state.subtitle,
            subtitleColor = TangemTheme.colors.text.primary1,
            captionColor = TangemTheme.colors.text.tertiary,
            iconResWebp = R.drawable.img_visa_36,
        )
    }
}

@Composable
private fun TangemPayMainTempUnavailableItem(modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier
            .clip(RoundedCornerShape(size = TangemTheme.dimens.radius14))
            .background(TangemTheme.colors.background.primary),
        enabled = false,
    ) {
        InputRowImageBase(
            modifier = Modifier.padding(all = TangemTheme.dimens.spacing12),
            subtitle = resourceReference(R.string.tangempay_payment_account),
            caption = TextReference.Str(DASH_SIGN),
            subtitleColor = TangemTheme.colors.text.tertiary,
            captionColor = TangemTheme.colors.text.tertiary,
            iconResWebp = R.drawable.img_visa_36,
        )
    }
}

@Composable
private fun TangemPayMainIssuingCardItem(state: TangemPayMainUM.IssuingCard, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier
            .clip(RoundedCornerShape(size = TangemTheme.dimens.radius14))
            .background(TangemTheme.colors.background.primary),
        onClick = state.onClick,
    ) {
        InputRowImageBase(
            modifier = Modifier
                .padding(all = TangemTheme.dimens.spacing12),
            subtitle = resourceReference(R.string.tangempay_payment_account),
            caption = resourceReference(R.string.tangempay_issuing_your_card),
            subtitleColor = TangemTheme.colors.text.primary1,
            captionColor = TangemTheme.colors.text.tertiary,
            iconResWebp = R.drawable.img_visa_36,
        )
    }
}

@Composable
private fun TangemPayMainFailedIssueItem(state: TangemPayMainUM.FailedToIssue, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier
            .clip(RoundedCornerShape(size = TangemTheme.dimens.radius14))
            .background(TangemTheme.colors.background.primary),
        onClick = state.onClick,
    ) {
        InputRowImageBase(
            modifier = Modifier
                .padding(
                    all = TangemTheme.dimens.spacing12,
                ),
            subtitle = TextReference.Res(R.string.tangempay_payment_account),
            caption = TextReference.Res(R.string.tangempay_failed_to_issue_card),
            subtitleColor = TangemTheme.colors.text.primary1,
            captionColor = TangemTheme.colors.text.tertiary,
            iconResWebp = com.tangem.core.ui.R.drawable.img_visa_36,
            iconEndRes = R.drawable.ic_alert_24,
            endIconTint = TangemTheme.colors.icon.warning,
        )
    }
}

@Composable
private fun TangemPayMainSyncNeededItem(modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier
            .clip(RoundedCornerShape(size = TangemTheme.dimens.radius14))
            .background(TangemTheme.colors.background.primary),
        enabled = false,
    ) {
        InputRowImageBase(
            modifier = Modifier.padding(
                all = TangemTheme.dimens.spacing12,
            ),
            subtitle = resourceReference(R.string.tangempay_payment_account),
            caption = resourceReference(R.string.tangempay_payment_account_sync_needed),
            subtitleColor = TangemTheme.colors.text.tertiary,
            captionColor = TangemTheme.colors.text.tertiary,
            iconResWebp = R.drawable.img_visa_36,
        )
    }
}

@Composable
private fun TangemPayMainExposedDeviceItem(modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier
            .clip(RoundedCornerShape(size = TangemTheme.dimens.radius14))
            .background(TangemTheme.colors.background.primary)
            .alpha(DISABLED_ALPHA),
        enabled = false,
    ) {
        InputRowImageBase(
            modifier = Modifier
                .padding(
                    all = TangemTheme.dimens.spacing12,
                ),
            subtitle = resourceReference(R.string.tangempay_payment_account),
            caption = resourceReference(R.string.tangem_pay_rooted_device_subtitle),
            subtitleColor = TangemTheme.colors.text.primary1,
            captionColor = TangemTheme.colors.text.tertiary,
            iconResWebp = R.drawable.img_visa_36,
            endIconTint = TangemTheme.colors.icon.warning,
        )
    }
}

@Composable
private fun TangemPayMainLoadingItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = TangemTheme.colors.background.primary, shape = TangemTheme.shapes.roundedCornersXMedium)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleShimmer(modifier = Modifier.size(36.dp))
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            RectangleShimmer(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .sizeIn(minWidth = 70.dp, minHeight = 12.dp),
            )
            RectangleShimmer(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .sizeIn(minWidth = 52.dp, minHeight = 12.dp),
            )
        }
        SpacerWMax()
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            RectangleShimmer(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .sizeIn(minWidth = 40.dp, minHeight = 12.dp),
            )
            RectangleShimmer(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .sizeIn(minWidth = 40.dp, minHeight = 12.dp),
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
private fun TangemPayMainItemsPreview(
    @PreviewParameter(TangemPayMainUMPreviewParameterProvider::class)
    state: TangemPayMainUM,
) {
    TangemThemePreview {
        TangemPayMainBlockItem(state = state, isBalanceHidden = false)
    }
}

private class TangemPayMainUMPreviewParameterProvider : CollectionPreviewParameterProvider<TangemPayMainUM>(
    collection = listOf(
        TangemPayMainUM.Loading,
        TangemPayMainUM.SyncNeeded,
        TangemPayMainUM.TemporaryUnavailable,
        TangemPayMainUM.ExposedDevice,
        TangemPayMainUM.FailedToIssue(onClick = {}),
        TangemPayMainUM.UnderReview(subtitle = resourceReference(R.string.tangempay_kyc_in_progress), onClick = {}),
        TangemPayMainUM.IssuingCard(onClick = {}),
        TangemPayMainUM.Content(
            subtitle = TextReference.Str("*1234"),
            isBalanceFlickering = true,
            balance = TextReference.Str("$ 101.56"),
            balanceSubtitle = TextReference.Str("USDC"),
            onClick = {},
            shouldShowOnlyCacheWarning = true,
        ),
    ),
)