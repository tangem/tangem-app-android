package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_filter_20
import com.tangem.core.ui.res.generated.icons.ic_lightning_20
import com.tangem.core.ui.utils.getGreyScaleColorFilter
import com.tangem.domain.express.models.ProviderFilterType
import com.tangem.feature.swap.models.states.ChooseProviderBottomSheetConfig
import com.tangem.feature.swap.models.states.PercentDifference
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun ChooseProviderTitle(content: ChooseProviderBottomSheetConfig, onCloseClick: () -> Unit) {
    TangemModal.TopNavigation(
        title = if (content.availableFilters.isNotEmpty()) {
            resourceReference(R.string.express_provider_for_swap)
        } else {
            resourceReference(R.string.express_choose_providers_title)
        },
        onClose = onCloseClick,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

@Composable
internal fun ChooseProviderContent(content: ChooseProviderBottomSheetConfig, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (content.availableFilters.isNotEmpty()) {
            ProviderFilter(content = content)
        } else {
            Text(
                text = stringResourceSafe(R.string.express_choose_providers_subtitle),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SpacerH(16.dp)
        content.notification?.let { notification -> ProviderNotification(notification) }
        SpacerH(16.dp)
        FilteredProviderList(content = content)
        SpacerH(22.dp)
        Icon(
            imageVector = Icons.ic_lightning_20,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.secondary,
        )
        Text(
            text = stringResourceSafe(R.string.express_more_providers_soon),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier
                .padding(top = 4.dp, bottom = 32.dp)
                .padding(horizontal = 56.dp),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Measures every provider but places only the filtered ones, so the list keeps the exact height of the unfiltered
 * list and switching the CEX/DEX filter never resizes the sheet. Unplaced rows are neither drawn nor clickable.
 */
@Composable
private fun FilteredProviderList(content: ChooseProviderBottomSheetConfig) {
    val visibleIds = content.providers.mapTo(HashSet()) { it.id }
    val providers = content.allProviders.filterIsInstance<ProviderState.Content>()
    Layout(
        content = {
            providers.forEach { provider ->
                ProviderSheetItem(state = provider, isSelected = provider.id == content.selectedProviderId)
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val gap = 8.dp.roundToPx()
        val rowHeights = placeables.map { it.height }.filter { it > 0 }
        val height = rowHeights.sum() + gap * (rowHeights.size - 1).coerceAtLeast(0)
        layout(width = constraints.maxWidth, height = height) {
            var y = 0
            providers.forEachIndexed { index, provider ->
                val placeable = placeables[index]
                if (provider.id in visibleIds && placeable.height > 0) {
                    placeable.place(x = 0, y = y)
                    y += placeable.height + gap
                }
            }
        }
    }
}

@Composable
private fun ProviderNotification(notification: NotificationUM) {
    val title = notification.config.title
    TangemMessageBanner(
        title = title ?: notification.config.subtitle,
        description = notification.config.subtitle.takeIf { title != null },
        variant = TangemMessageBanner.Variant.Warning,
        showGlowRing = false,
    )
}

@Composable
private fun ProviderFilter(content: ChooseProviderBottomSheetConfig) {
    val items = content.availableFilters.map { filter ->
        TangemSegmentedControl.Item(
            id = filter.name,
            label = when (filter) {
                ProviderFilterType.ALL -> resourceReference(R.string.common_all)
                ProviderFilterType.CEX -> stringReference("CEX")
                ProviderFilterType.DEX -> stringReference("DEX")
            },
            isSelected = filter == content.selectedFilter,
            onClick = { content.onFilterSelect(filter) },
        )
    }.toImmutableList()
    TangemSegmentedControl(items = items, modifier = Modifier.fillMaxWidth())
}

/** One provider card of the sheet; the main swap screen keeps its own [ProviderItem]. */
@Composable
private fun ProviderSheetItem(state: ProviderState, isSelected: Boolean, modifier: Modifier = Modifier) {
    val content = state as? ProviderState.Content ?: return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.primary)
            .conditionalCompose(isSelected) {
                border(BorderStroke(1.dp, TangemTheme.colors3.border.accent.blue), RoundedCornerShape(16.dp))
            }
            .clickable { content.onProviderClick(content.id) }
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProviderIcon(
            iconUrl = content.iconUrl,
            isGrayscale = content.additionalBadge == ProviderState.AdditionalBadge.Unavailable,
            isBestRate = content.additionalBadge == ProviderState.AdditionalBadge.BestTrade,
        )
        ProviderInfo(content = content, modifier = Modifier.weight(1f))
        if (content.approvalSettings is ProviderState.ApprovalSettings.Content) {
            Icon(
                imageVector = Icons.ic_filter_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(onClick = content.approvalSettings.onApprovalSelectClick),
            )
        }
    }
}

@Composable
private fun ProviderInfo(content: ProviderState.Content, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = content.name,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.primary,
                maxLines = 1,
            )
            Text(
                text = content.type,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.tertiary,
                maxLines = 1,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = content.subtitle.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            PercentDifferenceText(percentDifference = content.percentLowerThenBest)
            val labelPair = content.additionalBadge.label()
            if (labelPair != null) {
                val (text, status) = labelPair
                TangemBadge(text = text, status = status, size = TangemBadge.Size.X4)
            }
        }
    }
}

@Composable
private fun ProviderIcon(iconUrl: String, isGrayscale: Boolean, isBestRate: Boolean) {
    val (alpha, colorFilter) = getGreyScaleColorFilter(isGrayscale = isGrayscale)
    Box {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(iconUrl).crossfade(true).build(),
            contentDescription = null,
            alpha = alpha,
            colorFilter = colorFilter,
            error = painterResource(R.drawable.ic_custom_token_44),
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        if (isBestRate) {
            Icon(
                painter = painterResource(R.drawable.ic_rounded_star_24),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.staticLight,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(12.dp)
                    .background(TangemTheme.colors3.bg.accent.blue, CircleShape)
                    .padding(2.dp),
            )
        }
    }
}

@Composable
private fun PercentDifferenceText(percentDifference: PercentDifference) {
    val difference = percentDifference as? PercentDifference.Value ?: return
    if (difference.value == 0f) return
    val isBetter = difference.value > 0f
    Text(
        text = if (isBetter) "+${difference.value}%" else "${difference.value}%",
        style = TangemTheme.typography3.subheading.medium,
        color = if (isBetter) TangemTheme.colors3.text.accent.blue else TangemTheme.colors3.text.status.error,
        maxLines = 1,
    )
}

private fun ProviderState.AdditionalBadge.label(): Pair<TextReference, TangemBadge.Status>? = when (this) {
    ProviderState.AdditionalBadge.BestTrade ->
        resourceReference(R.string.express_provider_best_rate) to TangemBadge.Status.Info
    ProviderState.AdditionalBadge.BestDexRate ->
        resourceReference(R.string.express_provider_best_dex_rate) to TangemBadge.Status.Info
    ProviderState.AdditionalBadge.Recommended ->
        resourceReference(R.string.express_provider_recommended) to TangemBadge.Status.Info
    ProviderState.AdditionalBadge.PermissionRequired ->
        resourceReference(R.string.express_provider_permission_needed) to TangemBadge.Status.Neutral
    ProviderState.AdditionalBadge.FCAWarningList ->
        resourceReference(R.string.express_provider_fca_warning_list) to TangemBadge.Status.Neutral
    ProviderState.AdditionalBadge.Unavailable ->
        resourceReference(R.string.common_not_available) to TangemBadge.Status.Neutral
    ProviderState.AdditionalBadge.Empty -> null
}

// region Preview
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_ChooseProvider() {
    val providers = persistentListOf<ProviderState>(
        previewProvider(
            id = "1inch",
            badge = ProviderState.AdditionalBadge.Empty,
            percent = -1.1f,
            withSettings = true,
        ),
        previewProvider(
            id = "ChangeHero",
            badge = ProviderState.AdditionalBadge.BestTrade,
            percent = 0.2f,
            withSettings = false,
        ),
        previewProvider(id = "SimpleSwap", badge = ProviderState.AdditionalBadge.PermissionRequired, percent = 0f),
    )
    val content = ChooseProviderBottomSheetConfig(
        selectedProviderId = "1inch",
        notification = NotificationUM.Error(
            title = resourceReference(R.string.warning_express_providers_fca_warning_title),
            subtitle = resourceReference(R.string.warning_express_providers_fca_warning_description),
        ),
        providers = providers,
        allProviders = providers,
        selectedFilter = ProviderFilterType.ALL,
        availableFilters = persistentListOf(ProviderFilterType.ALL, ProviderFilterType.CEX, ProviderFilterType.DEX),
        onFilterSelect = {},
    )
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.secondary)) {
            ChooseProviderTitle(content = content, onCloseClick = {})
            ChooseProviderContent(content = content)
        }
    }
}

private fun previewProvider(
    id: String,
    badge: ProviderState.AdditionalBadge,
    percent: Float,
    withSettings: Boolean = false,
) = ProviderState.Content(
    id = id,
    name = id,
    type = "DEX",
    iconUrl = "",
    subtitle = stringReference("1 780,29 POL"),
    additionalBadge = badge,
    percentLowerThenBest = PercentDifference.Value(percent),
    selectionType = ProviderState.SelectionType.SELECT,
    namePrefix = ProviderState.PrefixType.NONE,
    approvalSettings = if (withSettings) {
        ProviderState.ApprovalSettings.Content(onApprovalSelectClick = {})
    } else {
        ProviderState.ApprovalSettings.Empty
    },
    onProviderClick = {},
)
// endregion Preview