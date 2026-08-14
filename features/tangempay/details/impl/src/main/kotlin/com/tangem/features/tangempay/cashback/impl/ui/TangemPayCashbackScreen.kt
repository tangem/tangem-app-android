package com.tangem.features.tangempay.cashback.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeForegroundEffectTangem
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayAdditionalCashbackUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackHistogramUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackHistogramUM.Style
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackInfoTilesUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackScreenUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackUM
import dev.chrisbanes.haze.HazeStyle
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

private const val GLOW_RADIUS_FACTOR = 0.585f
private const val GLOW_BLUE_ALPHA = 0.20f
private const val GLOW_WARM_ALPHA = 0.15f

@Composable
internal fun TangemPayCashbackScreen(state: TangemPayCashbackScreenUM, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            TangemTopNavigation(
                title = resourceReference(CoreUiR.string.tangempay_cashback_title),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onClose = state.onCloseClick,
            )
        },
    ) { contentPadding ->
        if (state is TangemPayCashbackScreenUM.Content && state.cashback.isEmpty) {
            EmptyStateGlow(modifier = Modifier.fillMaxSize())
        }

        when (state) {
            is TangemPayCashbackScreenUM.Loading -> TangemPayCashbackShimmer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
            is TangemPayCashbackScreenUM.Error -> CashbackError(
                onReloadClick = state.onReloadClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
            is TangemPayCashbackScreenUM.Content -> CashbackContent(
                state = state,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CashbackContent(
    state: TangemPayCashbackScreenUM.Content,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(bottom = 16.dp),
    ) {
        HeroBlock(state = state.cashback)
        state.cashback.banner?.let { banner ->
            CashbackBanner(
                banner = banner,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        state.infoTiles?.let { infoTiles ->
            TangemPayCashbackInfoTiles(state = infoTiles, modifier = Modifier.padding(top = 24.dp))
        }
        state.histogram?.let { histogram ->
            TangemPayCashbackHistogram(state = histogram, modifier = Modifier.padding(top = 24.dp))
        }
        state.additionalCashback?.let { additionalCashback ->
            TangemPayAdditionalCashback(state = additionalCashback, modifier = Modifier.padding(top = 24.dp))
        }
    }
}

@Composable
private fun CashbackError(onReloadClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.inverse)
                .clickable(role = Role.Button, onClick = onReloadClick)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_refresh_24),
                contentDescription = resourceReference(CoreUiR.string.common_reload).resolveReference(),
                tint = TangemTheme.colors3.icon.inverse,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = resourceReference(CoreUiR.string.tangempay_cashback_error_title).resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun EmptyStateGlow(modifier: Modifier = Modifier) {
    val isDark = LocalIsInDarkTheme.current
    val warm = if (isDark) Color(0xFF7A4A25) else Color(0xFFEA8C44)
    val blue = if (isDark) Color(0xFF0090F9) else Color(0xFF0092FC)
    Box(
        modifier = modifier
            .hazeForegroundEffectTangem(style = HazeStyle(blurRadius = 56.dp, tint = null))
            .drawBehind {
                val radius = size.width * GLOW_RADIUS_FACTOR
                val center = Offset(x = size.width / 2f, y = 0f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            warm.copy(alpha = GLOW_WARM_ALPHA),
                            blue.copy(alpha = GLOW_BLUE_ALPHA),
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            },
    )
}

@Composable
private fun HeroBlock(state: TangemPayCashbackUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = state.subtitle.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CashbackBanner(banner: TangemPayCashbackUM.Banner, modifier: Modifier = Modifier) {
    val contentColor = when (banner.type) {
        TangemPayCashbackUM.Banner.Type.Info -> TangemTheme.colors3.text.status.info
        TangemPayCashbackUM.Banner.Type.Error -> TangemTheme.colors3.text.status.error
    }
    val backgroundColor = when (banner.type) {
        TangemPayCashbackUM.Banner.Type.Info -> TangemTheme.colors3.bg.status.infoSubtle
        TangemPayCashbackUM.Banner.Type.Error -> TangemTheme.colors3.bg.status.errorSubtle
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_information_24),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = banner.text.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = contentColor,
        )
    }
}

@Preview(device = Devices.PIXEL_7_PRO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun TangemPayCashbackScreenPreview(
    @PreviewParameter(TangemPayCashbackScreenUMProvider::class) state: TangemPayCashbackScreenUM,
) {
    TangemThemePreviewRedesign {
        TangemPayCashbackScreen(state = state)
    }
}

private class TangemPayCashbackScreenUMProvider : CollectionPreviewParameterProvider<TangemPayCashbackScreenUM>(
    collection = listOf(
        TangemPayCashbackScreenUM.Content(
            onCloseClick = {},
            cashback = TangemPayCashbackUM(
                title = stringReference("$22.54 earned in June"),
                subtitle = stringReference("Will be deposited on July 1 – 5"),
                isEmpty = false,
                banner = TangemPayCashbackUM.Banner(
                    text = stringReference("Cashback $22.54 for June will be deposited till July 5"),
                    type = TangemPayCashbackUM.Banner.Type.Info,
                ),
            ),
            infoTiles = previewInfoTiles(),
            histogram = previewHistogram(),
            additionalCashback = previewAdditionalCashback(),
        ),
        TangemPayCashbackScreenUM.Content(
            onCloseClick = {},
            cashback = TangemPayCashbackUM(
                title = stringReference("Start spending and earn cashback"),
                subtitle = stringReference("Collected amount will be shown here"),
                isEmpty = true,
                banner = null,
            ),
            infoTiles = null,
            histogram = null,
            additionalCashback = null,
        ),
        TangemPayCashbackScreenUM.Loading(onCloseClick = {}),
        TangemPayCashbackScreenUM.Error(onCloseClick = {}, onReloadClick = {}),
    ),
)

private fun previewInfoTiles() = TangemPayCashbackInfoTilesUM(
    rate = TangemPayCashbackInfoTilesUM.Tile(
        iconRes = CoreUiR.drawable.ic_percent_24,
        title = stringReference("Cashback 1%"),
        subtitle = stringReference("With your Basic plan"),
        onClick = {},
    ),
    accruals = TangemPayCashbackInfoTilesUM.Tile(
        iconRes = CoreUiR.drawable.ic_information_24,
        title = stringReference("Accruals"),
        subtitle = stringReference("Limits and exceptions"),
        onClick = {},
    ),
)

@Suppress("MagicNumber")
private fun previewHistogram(): TangemPayCashbackHistogramUM {
    fun bar(month: String, amount: String, value: Float, style: Style) = TangemPayCashbackHistogramUM.Bar(
        month = stringReference(month),
        amount = stringReference(amount),
        amountValue = value,
        style = style,
    )
    return TangemPayCashbackHistogramUM(
        title = stringReference("$132.15 earned in total"),
        bars = persistentListOf(
            bar(month = "Feb", amount = "$12.02", value = 12.02f, style = Style.Regular),
            bar(month = "Mar", amount = "$44.22", value = 44.22f, style = Style.Regular),
            bar(month = "Apr", amount = "$38.52", value = 38.52f, style = Style.Regular),
            bar(month = "May", amount = "$26.10", value = 26.10f, style = Style.Regular),
            bar(month = "Jun", amount = "$32.15", value = 32.15f, style = Style.Highlighted),
        ),
    )
}

private fun previewAdditionalCashback() = TangemPayAdditionalCashbackUM(
    items = persistentListOf(
        TangemPayAdditionalCashbackUM.Item(
            id = "1",
            name = stringReference("Groceries increase"),
            description = stringReference("+1% cashback for groceries stores"),
            badge = TangemPayAdditionalCashbackUM.Badge.Permanent,
        ),
        TangemPayAdditionalCashbackUM.Item(
            id = "2",
            name = stringReference("Cashback increase"),
            description = stringReference("+2% cashback for groceries stores. Max \$10/month"),
            badge = TangemPayAdditionalCashbackUM.Badge.Until(stringReference("Until 09.26.2026")),
        ),
    ),
)