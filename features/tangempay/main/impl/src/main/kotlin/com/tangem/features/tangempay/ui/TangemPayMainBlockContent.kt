package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.features.tangempay.main.impl.R
import com.tangem.utils.StringsSigns.DASH_SIGN

@Composable
internal fun TangemPayMainBlockContent(
    state: TangemPayMainUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TangemPayMainUM.Empty -> Unit
        is TangemPayMainUM.Loading -> TangemPayMainLoading(modifier)
        is TangemPayMainUM.SelectPlan -> TangemPayRow(
            subtitle = resourceReference(R.string.tangempay_main_select_plan),
            modifier = modifier,
            onClick = state.onClick,
        )
        is TangemPayMainUM.UnderReview -> TangemPayRow(
            subtitle = state.subtitle,
            modifier = modifier,
            onClick = state.onClick,
        )
        is TangemPayMainUM.ActionRequired -> TangemPayRow(
            subtitle = resourceReference(R.string.tangempay_action_required_title),
            subtitleColor = TangemTheme.colors3.text.status.warning,
            modifier = modifier,
            onClick = state.onClick,
        )
        is TangemPayMainUM.IssuingCard -> TangemPayRow(
            subtitle = resourceReference(R.string.tangempay_issuing_your_card),
            modifier = modifier,
            onClick = state.onClick,
        )
        is TangemPayMainUM.FailedToIssue -> TangemPayRow(
            subtitle = resourceReference(R.string.tangempay_failed_to_issue_card),
            modifier = modifier,
            showError = true,
            onClick = state.onClick,
        )
        is TangemPayMainUM.Content -> TangemPayMainContent(state, isBalanceHidden, modifier)
        is TangemPayMainUM.TemporaryUnavailable -> TangemPayUnavailable(
            subtitle = stringReference(DASH_SIGN),
            modifier = modifier,
        )
        is TangemPayMainUM.SyncNeeded -> TangemPayUnavailable(
            subtitle = resourceReference(R.string.tangempay_payment_account_sync_needed),
            modifier = modifier,
        )
        is TangemPayMainUM.ExposedDevice -> TangemPayRow(
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
    TangemRow(
        modifier = modifier.tangemPayCard(),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = payMainUM.onClick,
        startSlot = { TangemPayIssuedIcon() },
        titleSlot = { TangemPayTitle() },
        subtitleSlot = { TangemPaySubtitle(text = payMainUM.subtitle) },
        valueSlot = {
            TangemPayFiatAmount(
                text = payMainUM.balance.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
                isBalanceFlickering = payMainUM.isBalanceFlickering,
                isBalanceFromCache = payMainUM.shouldShowOnlyCacheWarning,
            )
        },
        subvalueSlot = {
            TangemRowText(
                text = payMainUM.balanceSubtitle.orMaskWithStars(isBalanceHidden),
                role = TangemRowTextRole.Subvalue,
            )
        },
    )
}

@Composable
private fun TangemPayRow(
    subtitle: TextReference,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    subtitleColor: Color = TangemTheme.colors3.text.secondary,
    showError: Boolean = false,
) {
    TangemRow(
        modifier = modifier.tangemPayCard(),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = onClick,
        startSlot = { TangemPayIcon() },
        titleSlot = { TangemPayTitle() },
        subtitleSlot = {
            TangemPaySubtitle(
                text = subtitle,
                color = subtitleColor
            )
        },
        endSlot = if (showError) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_alert_24),
                    tint = TangemTheme.colors3.icon.status.warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun TangemPayUnavailable(subtitle: TextReference, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier.tangemPayCard(),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { TangemPayIcon() },
        titleSlot = {
            Text(
                text = stringResourceSafe(R.string.tangempay_title),
                color = TangemTheme.colors3.text.tertiary,
                style = TangemTheme.typography3.body.medium,
            )
        },
        subtitleSlot = { TangemPaySubtitle(text = subtitle, color = TangemTheme.colors3.text.tertiary) },
    )
}

@Composable
private fun TangemPayIcon() {
    TangemAvatar(
        state = TangemAvatar.UiState.Account(iconRes = R.drawable.ic_visa_24, color = TangemAvatar.FillColor.PayVisa),
        size = TangemAvatar.Size.X40,
    )
}

@Composable
private fun TangemPayIssuedIcon() {
    TangemAvatar(
        state = TangemAvatar.UiState.Account(iconRes = R.drawable.ic_visa_24, color = TangemAvatar.FillColor.PayVisa),
        size = TangemAvatar.Size.X40,
    )
}

@Composable
private fun TangemPayTitle() {
    TangemRowText(
        text = resourceReference(R.string.tangempay_title),
        role = TangemRowTextRole.Title,
    )
}

@Composable
private fun TangemPaySubtitle(text: TextReference, color: Color = TangemTheme.colors3.text.secondary) {
    Text(
        text = text.resolveReference(),
        color = color,
        style = TangemTheme.typography3.caption.medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TangemPayFiatAmount(text: AnnotatedString, isBalanceFlickering: Boolean, isBalanceFromCache: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isBalanceFromCache) {
            Icon(
                modifier = Modifier
                    .size(12.dp)
                    .testTag(TangemPayTestTags.MAIN_SCREEN_TILE_CACHE_ICON),
                painter = painterResource(R.drawable.ic_error_sync_24),
                tint = TangemTheme.colors3.icon.secondary,
                contentDescription = null,
            )
        }
        Text(
            text = text,
            style = TangemTheme.typography3.body.medium.applyBladeBrush(
                isEnabled = isBalanceFlickering,
                textColor = TangemTheme.colors3.text.primary,
            ),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(TangemPayTestTags.MAIN_SCREEN_TILE_BALANCE),
        )
    }
}

@Composable
private fun TangemPayMainLoading(modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier.tangemPayCard(),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            TangemAvatar(
                state = TangemAvatar.UiState.Shimmer(shape = TangemAvatar.UiState.Shimmer.Shape.RoundedSquare),
                size = TangemAvatar.Size.X40,
            )
        },
        titleSlot = {
            TextShimmer(
                style = TangemTheme.typography3.body.medium,
                radius = 10.dp,
                modifier = Modifier.width(100.dp),
            )
        },
        subtitleSlot = {
            TextShimmer(
                style = TangemTheme.typography3.caption.medium,
                radius = 10.dp,
                modifier = Modifier.width(44.dp),
            )
        },
        valueSlot = {
            TextShimmer(
                style = TangemTheme.typography3.body.medium,
                radius = 10.dp,
                modifier = Modifier.width(80.dp),
            )
        },
    )
}

@Composable
private fun Modifier.tangemPayCard(): Modifier = this
    .clip(RoundedCornerShape(24.dp))
    .background(TangemTheme.colors3.bg.secondary)
    .testTag(TangemPayTestTags.MAIN_SCREEN_TILE)

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayMainBlockContent_Preview(
    @PreviewParameter(TangemPayMainBlockContentPreviewParameterProvider::class)
    state: TangemPayMainUM,
) {
    TangemThemePreviewRedesign {
        TangemPayMainBlockContent(
            state = state,
            isBalanceHidden = false,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
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
        TangemPayMainUM.ActionRequired(onClick = {}),
        TangemPayMainUM.SelectPlan(onClick = {}),
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