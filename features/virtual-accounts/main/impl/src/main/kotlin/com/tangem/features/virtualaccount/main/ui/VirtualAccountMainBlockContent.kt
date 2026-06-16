package com.tangem.features.virtualaccount.main.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.virtualaccount.main.entity.VirtualAccountMainUM
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun VirtualAccountMainBlockContent(
    state: VirtualAccountMainUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is VirtualAccountMainUM.Empty -> Unit
        is VirtualAccountMainUM.Loading -> VirtualAccountMainLoading(modifier)
        is VirtualAccountMainUM.UnderReview -> VirtualAccountStateRow(
            subtitle = state.subtitle,
            modifier = modifier,
            onClick = state.onClick,
        )
        is VirtualAccountMainUM.Provisioning -> VirtualAccountStateRow(
            subtitle = stringReference("Setting up your account"),
            modifier = modifier,
            onClick = state.onClick,
        )
        is VirtualAccountMainUM.CountryNotSupported -> VirtualAccountStateRow(
            subtitle = stringReference("Not available in your region"),
            modifier = modifier,
            onClick = state.onClick,
        )
        is VirtualAccountMainUM.Content -> VirtualAccountMainContent(state, isBalanceHidden, modifier)
        is VirtualAccountMainUM.TemporaryUnavailable -> VirtualAccountStateRow(
            subtitle = stringReference(DASH_SIGN),
            modifier = modifier,
            isEnabled = false,
        )
        is VirtualAccountMainUM.SyncNeeded -> VirtualAccountStateRow(
            subtitle = stringReference("Virtual account session expired"),
            modifier = modifier,
            isEnabled = false,
        )
        is VirtualAccountMainUM.ExposedDevice -> VirtualAccountStateRow(
            subtitle = stringReference("Unable to use on rooted devices"),
            modifier = modifier,
        )
    }
}

@Composable
private fun VirtualAccountMainContent(
    state: VirtualAccountMainUM.Content,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    TangemRowContainer(
        modifier = modifier
            .clip(RoundedCornerShape(size = 18.dp))
            .background(TangemTheme.colors2.surface.level3)
            .clickableSingle(onClick = state.onClick),
    ) {
        AccountIcon(
            name = TextReference.EMPTY,
            icon = AccountIconUM.Virtual,
            size = AccountIconSize.RedesignedDefault,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.HEAD)
                .padding(end = TangemTheme.dimens2.x2),
        )
        Text(
            text = "Virtual account",
            color = TangemTheme.colors2.text.neutral.primary,
            style = TangemTheme.typography2.bodySemibold16,
            modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
        )
        Text(
            text = state.subtitle.resolveReference(),
            color = TangemTheme.colors2.text.neutral.secondary,
            style = TangemTheme.typography2.captionSemibold12,
            modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
        )
        VirtualAccountFiatAmount(
            text = state.balance.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
            isBalanceFlickering = state.isBalanceFlickering,
            isBalanceFromCache = state.shouldShowOnlyCacheWarning,
            modifier = Modifier.layoutId(TangemRowLayoutId.END_TOP),
        )
        Text(
            text = state.balanceSubtitle.resolveReference(),
            color = TangemTheme.colors2.text.neutral.secondary,
            style = TangemTheme.typography2.captionSemibold12,
            modifier = Modifier.layoutId(TangemRowLayoutId.END_BOTTOM),
        )
    }
}

@Composable
private fun VirtualAccountStateRow(
    subtitle: TextReference,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
) {
    TangemRowContainer(
        modifier = modifier
            .clip(RoundedCornerShape(size = 18.dp))
            .background(TangemTheme.colors2.surface.level3)
            .conditional(onClick != null && isEnabled) { clickableSingle(onClick = requireNotNull(onClick)) },
    ) {
        AccountIcon(
            name = TextReference.EMPTY,
            icon = AccountIconUM.Virtual,
            size = AccountIconSize.RedesignedDefault,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.HEAD)
                .padding(end = TangemTheme.dimens2.x2),
        )
        Text(
            text = "Virtual account",
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
    }
}

@Composable
private fun VirtualAccountFiatAmount(
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
                    painter = painterResource(CoreUiR.drawable.ic_error_sync_24),
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
private fun VirtualAccountMainLoading(modifier: Modifier = Modifier) {
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
private fun VirtualAccountMainBlockContent_Preview(
    @PreviewParameter(VirtualAccountMainBlockContentPreviewParameterProvider::class)
    state: VirtualAccountMainUM,
) {
    TangemThemePreviewRedesign {
        VirtualAccountMainBlockContent(state = state, isBalanceHidden = false)
    }
}

private class VirtualAccountMainBlockContentPreviewParameterProvider :
    CollectionPreviewParameterProvider<VirtualAccountMainUM>(
        collection = listOf(
            VirtualAccountMainUM.Loading,
            VirtualAccountMainUM.SyncNeeded,
            VirtualAccountMainUM.TemporaryUnavailable,
            VirtualAccountMainUM.ExposedDevice,
            VirtualAccountMainUM.Provisioning(onClick = {}),
            VirtualAccountMainUM.CountryNotSupported(onClick = {}),
            VirtualAccountMainUM.UnderReview(subtitle = TextReference.Str("KYC in progress"), onClick = {}),
            VirtualAccountMainUM.Content(
                subtitle = TextReference.Str("USDC"),
                isBalanceFlickering = true,
                balance = TextReference.Str("$ 101.56"),
                balanceSubtitle = TextReference.Str("USDC"),
                onClick = {},
                shouldShowOnlyCacheWarning = true,
            ),
        ),
    )
// endregion