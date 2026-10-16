package com.tangem.features.yield.supply.impl.promobottomsheet.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.YieldSupplyTestTags
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.promobottomsheet.entity.YieldModePromoBottomSheetUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val ASSET_ICON_SIZE = 28.dp
private val ASSET_ICON_STEP = 21.dp
private val ASSET_ICON_RING = 2.dp

@Composable
internal fun YieldModePromoBottomSheet(state: YieldModePromoBottomSheetUM) {
    TangemModal<YieldModePromoBottomSheetUM>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismissRequest,
            content = state,
        ),
        onBack = state.onBack,
        scrollableContent = false,
        chromePlacement = TangemModal.ChromePlacement.Overlay,
        containerColor = TangemTheme.colors3.bg.primary,
        title = { content ->
            TangemModal.TopNavigation(
                endButton = {
                    TangemButton.Close(
                        modifier = Modifier.testTag(YieldSupplyTestTags.PROMO_BOTTOM_SHEET_CLOSE_BUTTON),
                        onClick = content.onCloseClick,
                    )
                },
            )
        },
        content = { content ->
            Content(state = content, modifier = Modifier.weight(weight = 1f, fill = false))
        },
    )
}

@Composable
private fun Content(state: YieldModePromoBottomSheetUM, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { state.onShown() }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            HeaderArt()
            Texts(title = state.title)
            AssetsCapsule(tokensCount = state.tokensCount, iconUrls = state.tokenIconUrls)
            SpacerH(12.dp)
        }

        TangemFooter(
            modifier = Modifier.fillMaxWidth(),
            windowInsets = TangemFooter.ModalWindowInsets,
            fadeEnabled = false,
        ) {
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(YieldSupplyTestTags.PROMO_BOTTOM_SHEET_TRY_BUTTON),
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.yield_module_main_promo_button),
                onClick = state.onTryClick,
            )
        }
    }
}

@Composable
private fun HeaderArt() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.img_yield_promo_graph_104),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(width = 104.dp, height = 93.dp),
        )
    }
}

@Composable
private fun Texts(title: TextReference) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier
                .testTag(YieldSupplyTestTags.PROMO_BOTTOM_SHEET_TITLE)
                .semantics { heading() },
            text = title.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = stringResourceSafe(R.string.yield_module_main_promo_subtitle),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun AssetsCapsule(tokensCount: TextReference, iconUrls: ImmutableList<String>) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.secondary)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OverlappingAssetIcons(iconUrls = iconUrls)
        Text(
            modifier = Modifier.weight(1f),
            text = tokensCount.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 12.sp),
        )
    }
}

@Composable
private fun OverlappingAssetIcons(iconUrls: ImmutableList<String>) {
    val ringColor = TangemTheme.colors3.bg.secondary
    val placeholder = ColorPainter(TangemTheme.colors3.bg.tertiary)
    val context = LocalContext.current

    Box(modifier = Modifier.width(ASSET_ICON_SIZE + ASSET_ICON_STEP * (iconUrls.size - 1).coerceAtLeast(0))) {
        iconUrls.forEachIndexed { index, url ->
            AsyncImage(
                model = ImageRequest.Builder(context).data(url).crossfade(enable = true).build(),
                contentDescription = null,
                placeholder = placeholder,
                error = placeholder,
                modifier = Modifier
                    .padding(start = ASSET_ICON_STEP * index)
                    .size(ASSET_ICON_SIZE)
                    .border(width = ASSET_ICON_RING, color = ringColor, shape = CircleShape)
                    .padding(ASSET_ICON_RING)
                    .clip(CircleShape),
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, fontScale = 2f)
@Composable
private fun YieldModePromoBottomSheetPreview() {
    TangemThemePreviewRedesign(alwaysShowBottomSheets = true) {
        YieldModePromoBottomSheet(
            state = YieldModePromoBottomSheetUM(
                title = stringReference("Earn with your crypto up to 9.20% APY"),
                tokensCount = stringReference("Choose from 999 tokens"),
                tokenIconUrls = persistentListOf("", "", ""),
                onShown = {},
                onCloseClick = {},
                onBack = {},
                onDismissRequest = {},
                onTryClick = {},
            ),
        )
    }
}
// endregion