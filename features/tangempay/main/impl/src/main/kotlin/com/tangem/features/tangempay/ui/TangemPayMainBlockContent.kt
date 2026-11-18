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
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_sign_usd_20
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.features.tangempay.main.impl.R
import com.tangem.utils.StringsSigns.DASH_SIGN

@Composable
internal fun TangemPayMainBlockContent(
    state: TangemPayMainUM,
    isBalanceHidden: Boolean,
    isGrouped: Boolean,
    modifier: Modifier = Modifier,
) {
    val rowModifier = modifier.tangemPayCard(
        isActionRequired = false,
        isGrouped = isGrouped,
    )
    when (state) {
        is TangemPayMainUM.Empty -> Unit
        is TangemPayMainUM.Loading -> TangemPayMainLoading(modifier = rowModifier)
        is TangemPayMainUM.UnderReview -> TangemPayRow(
            isGroupAppearance = !isGrouped,
            subtitle = state.subtitle,
            modifier = rowModifier,
            onClick = state.onClick,
        )
        is TangemPayMainUM.ActionRequired -> TangemPayRow(
            isGroupAppearance = !isGrouped,
            subtitle = resourceReference(R.string.tangempay_action_required_title),
            subtitleColor = TangemTheme.colors3.text.status.warning,
            modifier = rowModifier,
            onClick = state.onClick,
        )
        is TangemPayMainUM.IssuingCard -> TangemPayRow(
            isGroupAppearance = false,
            subtitle = resourceReference(R.string.money_account_main_status_activating),
            modifier = rowModifier,
            onClick = state.onClick,
        )
        is TangemPayMainUM.FailedToIssue -> TangemPayRow(
            isGroupAppearance = false,
            subtitle = resourceReference(R.string.tangempay_failed_to_issue_card),
            modifier = rowModifier,
            showError = true,
            onClick = state.onClick,
        )
        is TangemPayMainUM.Content -> TangemPayMainContent(
            payMainUM = state,
            isBalanceHidden = isBalanceHidden,
            isGrouped = isGrouped,
            modifier = modifier,
        )
        is TangemPayMainUM.TemporaryUnavailable -> TangemPayUnavailable(
            isPayGroupAppearance = false,
            subtitle = stringReference(DASH_SIGN),
            modifier = rowModifier,
        )
        is TangemPayMainUM.SyncNeeded -> TangemPayUnavailable(
            isPayGroupAppearance = !isGrouped,
            subtitle = resourceReference(R.string.tangempay_payment_account_sync_needed),
            modifier = rowModifier,
        )
        is TangemPayMainUM.ExposedDevice -> TangemPayRow(
            isGroupAppearance = false,
            subtitle = resourceReference(R.string.tangem_pay_rooted_device_subtitle),
            modifier = rowModifier,
        )
    }
}

@Composable
private fun TangemPayMainContent(
    payMainUM: TangemPayMainUM.Content,
    isBalanceHidden: Boolean,
    isGrouped: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        TangemRow(
            modifier = Modifier.tangemPayCard(isActionRequired = payMainUM.isActionRequired, isGrouped = isGrouped),
            contentLead = TangemRowContentLead.Start,
            verticalAlignment = TangemRowVerticalAlignment.Center,
            onClick = payMainUM.onClick,
            startSlot = { TangemPayIssuedIcon() },
            titleSlot = { TangemPayTitle(isPayGroupAppearance = false) },
            subtitleSlot = {
                TangemPaySubtitle(
                    text = payMainUM.subtitle,
                    color = if (payMainUM.isActionRequired) {
                        TangemTheme.colors3.text.status.warning
                    } else {
                        TangemTheme.colors3.text.secondary
                    },
                )
            },
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
        if (payMainUM.isActionRequired && !isGrouped) {
            TangemGlowRing(
                modifier = Modifier.matchParentSize(),
                variant = TangemGlowRing.Variant.Warning,
                cornerRadius = 24.dp,
            )
        }
    }
}

@Composable
private fun TangemPayRow(
    isGroupAppearance: Boolean,
    subtitle: TextReference,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    subtitleColor: Color = TangemTheme.colors3.text.secondary,
    showError: Boolean = false,
) {
    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = onClick,
        startSlot = { TangemPayIcon(isPayGroupAppearance = isGroupAppearance) },
        titleSlot = { TangemPayTitle(isPayGroupAppearance = isGroupAppearance) },
        subtitleSlot = {
            TangemPaySubtitle(
                text = subtitle,
                color = subtitleColor,
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
private fun TangemPayUnavailable(
    isPayGroupAppearance: Boolean,
    subtitle: TextReference,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { TangemPayIcon(isPayGroupAppearance = isPayGroupAppearance) },
        titleSlot = {
            Text(
                text = stringResourceSafe(tangemPayTitleRes(isPayGroupAppearance = isPayGroupAppearance)),
                color = TangemTheme.colors3.text.tertiary,
                style = TangemTheme.typography3.body.medium,
            )
        },
        subtitleSlot = { TangemPaySubtitle(text = subtitle, color = TangemTheme.colors3.text.tertiary) },
    )
}

@Composable
private fun TangemPayIcon(isPayGroupAppearance: Boolean) {
    if (isPayGroupAppearance) {
        TangemPayGroupIcon()
    } else {
        TangemAvatar(
            state = TangemAvatar.UiState.Account(kind = TangemAvatar.AccountKind.Visa),
            size = TangemAvatar.Size.X40,
        )
    }
}

@Composable
private fun TangemPayGroupIcon() {
    TangemAvatar(
        state = TangemAvatar.UiState.Icon(
            imageVector = Icons.ic_sign_usd_20,
            appearance = TangemAvatar.Appearance.Neutral,
            shape = TangemAvatar.Shape.RoundedSquare,
        ),
        size = TangemAvatar.Size.X40,
    )
}

@Composable
private fun TangemPayIssuedIcon() {
    TangemAvatar(
        state = TangemAvatar.UiState.Account(kind = TangemAvatar.AccountKind.Visa),
        size = TangemAvatar.Size.X40,
    )
}

@Composable
private fun TangemPayTitle(isPayGroupAppearance: Boolean) {
    TangemRowText(
        text = resourceReference(tangemPayTitleRes(isPayGroupAppearance = isPayGroupAppearance)),
        role = TangemRowTextRole.Title,
    )
}

private fun tangemPayTitleRes(isPayGroupAppearance: Boolean): Int = if (isPayGroupAppearance) {
    R.string.tangempay_main_group_title
} else {
    R.string.tangempay_main_name
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
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            TangemAvatar(
                state = TangemAvatar.UiState.Shimmer(shape = TangemAvatar.Shape.RoundedSquare),
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
private fun Modifier.tangemPayCard(isActionRequired: Boolean, isGrouped: Boolean): Modifier = if (isGrouped) {
    testTag(TangemPayTestTags.MAIN_SCREEN_TILE)
} else {
    clip(RoundedCornerShape(24.dp))
        .background(
            if (isActionRequired) TangemTheme.colors3.bg.status.warningSubtle else TangemTheme.colors3.bg.secondary,
        )
        .testTag(TangemPayTestTags.MAIN_SCREEN_TILE)
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
        TangemPayMainBlockContent(
            state = state,
            isBalanceHidden = false,
            isGrouped = false,
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
        TangemPayMainUM.IssuingCard(onClick = {}),
        TangemPayMainUM.Content(
            subtitle = TextReference.Str("*1234"),
            isBalanceFlickering = true,
            balance = TextReference.Str("$ 101.56"),
            balanceSubtitle = TextReference.Str("USDC"),
            onClick = {},
            shouldShowOnlyCacheWarning = true,
            isActionRequired = false,
        ),
        TangemPayMainUM.Content(
            subtitle = resourceReference(R.string.tangempay_action_required_title),
            isBalanceFlickering = false,
            balance = TextReference.Str("$ 101.56"),
            balanceSubtitle = TextReference.Str("USD"),
            onClick = {},
            shouldShowOnlyCacheWarning = false,
            isActionRequired = true,
        ),
    ),
)
// endregion