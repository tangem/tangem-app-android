package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.features.tangempay.main.impl.R
import com.tangem.utils.StringsSigns.DASH_SIGN

private const val DISABLED_ALPHA = 0.5F

@Composable
internal fun TangemPayMainBlockContent(
    state: TangemPayMainUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TangemPayMainUM.Empty -> Unit
        is TangemPayMainUM.Loading -> TangemPayMainLoading(modifier)
        is TangemPayMainUM.UnderReview -> TangemPayStateRow(
            subtitle = state.subtitle,
            modifier = modifier,
            onClick = state.onClick,
        )
        is TangemPayMainUM.IssuingCard -> TangemPayStateRow(
            subtitle = resourceReference(R.string.tangempay_issuing_your_card),
            modifier = modifier,
            onClick = state.onClick,
        )
        is TangemPayMainUM.FailedToIssue -> TangemPayStateRow(
            subtitle = resourceReference(R.string.tangempay_failed_to_issue_card),
            modifier = modifier,
            showError = true,
            onClick = state.onClick,
        )
        is TangemPayMainUM.Content -> TangemPayMainContent(state, isBalanceHidden, modifier)
        is TangemPayMainUM.TemporaryUnavailable -> TangemPayStateRow(
            subtitle = stringReference(DASH_SIGN),
            modifier = modifier,
            isEnabled = false,
        )
        is TangemPayMainUM.SyncNeeded -> TangemPayStateRow(
            subtitle = resourceReference(R.string.tangempay_payment_account_sync_needed),
            modifier = modifier,
            isEnabled = false,
        )
        is TangemPayMainUM.ExposedDevice -> TangemPayStateRow(
            subtitle = resourceReference(R.string.tangem_pay_rooted_device_subtitle),
            modifier = modifier,
        )
    }
}

@Composable
private fun TangemPayMainContent(
    payMainUM: TangemPayMainUM.Content,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    TangemRowContainer(
        modifier = modifier
            .clip(RoundedCornerShape(size = 18.dp))
            .background(TangemTheme.colors2.surface.level3)
            .clickableSingle(onClick = payMainUM.onClick),
    ) {
        Image(
            painter = painterResource(R.drawable.img_visa_36),
            contentDescription = null,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.HEAD)
                .size(TangemTheme.dimens2.x10)
                .padding(end = TangemTheme.dimens2.x2),
        )
        Text(
            text = stringResourceSafe(R.string.tangempay_payment_account),
            color = TangemTheme.colors2.text.neutral.primary,
            style = TangemTheme.typography2.bodySemibold16,
            modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
        )
        Text(
            text = payMainUM.subtitle.resolveReference(),
            color = TangemTheme.colors2.text.neutral.secondary,
            style = TangemTheme.typography2.captionSemibold12,
            modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
        )
        TangemPayFiatAmount(
            text = payMainUM.balance.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
            isBalanceFlickering = payMainUM.isBalanceFlickering,
            isBalanceFromCache = payMainUM.shouldShowOnlyCacheWarning,
            modifier = Modifier.layoutId(TangemRowLayoutId.END_TOP),
        )
        Text(
            text = payMainUM.balanceSubtitle.resolveReference(),
            color = TangemTheme.colors2.text.neutral.secondary,
            style = TangemTheme.typography2.captionSemibold12,
            modifier = Modifier.layoutId(TangemRowLayoutId.END_BOTTOM),
        )
    }
}

@Composable
private fun TangemPayStateRow(
    subtitle: TextReference,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showError: Boolean = false,
    isEnabled: Boolean = true,
) {
    TangemRowContainer(
        modifier = modifier
            .clip(RoundedCornerShape(size = 18.dp))
            .background(TangemTheme.colors2.surface.level3)
            .conditional(onClick != null && isEnabled) { clickableSingle(onClick = requireNotNull(onClick)) },
    ) {
        Image(
            painter = painterResource(R.drawable.img_visa_36),
            contentDescription = null,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.HEAD)
                .size(TangemTheme.dimens2.x10)
                .padding(end = TangemTheme.dimens2.x2)
                .conditionalCompose(!isEnabled) {
                    alpha(DISABLED_ALPHA)
                },
        )
        Text(
            text = stringResourceSafe(R.string.tangempay_payment_account),
            color = if (isEnabled) {
                TangemTheme.colors2.text.neutral.primary
            } else {
                TangemTheme.colors2.text.status.disabled
            },
            style = TangemTheme.typography2.bodySemibold16,
            modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
        )
        Text(
            text = subtitle.resolveReference(),
            color = if (isEnabled) {
                TangemTheme.colors2.text.neutral.secondary
            } else {
                TangemTheme.colors2.text.status.disabled
            },
            style = TangemTheme.typography2.captionSemibold12,
            modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
        )
        AnimatedVisibility(
            visible = showError,
            enter = fadeIn(),
            exit = fadeOut(),
            label = "Error Icon Animation",
            modifier = Modifier
                .layoutId(TangemRowLayoutId.TAIL)
                .size(TangemTheme.dimens2.x4),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_alert_24),
                tint = TangemTheme.colors2.graphic.status.warning,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun TangemPayFiatAmount(
    text: AnnotatedString,
    isBalanceFlickering: Boolean,
    isBalanceFromCache: Boolean,
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
                    tint = TangemTheme.colors2.graphic.neutral.secondary,
                    contentDescription = null,
                )
            }
        }

        Text(
            text = text,
            style = TangemTheme.typography2.bodySemibold16.applyBladeBrush(
                isEnabled = isBalanceFlickering,
                textColor = TangemTheme.colors2.text.neutral.primary,
            ),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun TangemPayMainLoading(modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier
            .clip(RoundedCornerShape(size = 18.dp))
            .background(TangemTheme.colors2.surface.level3),
    ) {
        CircleShimmer(
            modifier = Modifier
                .layoutId(TangemRowLayoutId.HEAD)
                .padding(end = TangemTheme.dimens2.x2)
                .size(TangemTheme.dimens2.x10),
        )
        TextShimmer(
            style = TangemTheme.typography2.bodySemibold16,
            radius = TangemTheme.dimens2.x25,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.START_TOP)
                .width(TangemTheme.dimens2.x25),
        )
        TextShimmer(
            style = TangemTheme.typography2.captionSemibold12,
            radius = TangemTheme.dimens2.x25,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.START_BOTTOM)
                .width(TangemTheme.dimens2.x11),
        )
        TextShimmer(
            style = TangemTheme.typography2.bodyRegular16,
            radius = TangemTheme.dimens2.x25,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.END_TOP)
                .width(TangemTheme.dimens2.x20),
        )
        TextShimmer(
            style = TangemTheme.typography2.bodyRegular16,
            radius = TangemTheme.dimens2.x25,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.END_BOTTOM)
                .width(TangemTheme.dimens2.x11),
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayMainBlockContent_Preview(
    @PreviewParameter(TangemPayMainBlockContentPreviewParameterProvider::class)
    state: TangemPayMainUM,
) {
    TangemThemePreviewRedesign {
        TangemPayMainBlockContent(state = state, isBalanceHidden = false)
    }
}

private class TangemPayMainBlockContentPreviewParameterProvider : CollectionPreviewParameterProvider<TangemPayMainUM>(
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
// endregion