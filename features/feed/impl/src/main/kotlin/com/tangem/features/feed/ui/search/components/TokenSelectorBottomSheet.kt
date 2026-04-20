package com.tangem.features.feed.ui.search.components

import android.content.res.Configuration
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.Fade
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.ui.search.preview.TokenSelectorContentPreviewProvider
import com.tangem.features.feed.ui.search.state.TokenSelectorContentUM
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState

@Composable
internal fun TokenSelectorBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<TokenSelectorContentUM>(
        config = config,
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors2.surface.level2,
        content = { content ->
            TokenSelectorContent(content, config.onDismissRequest)
        },
    )
}

@Composable
private fun TokenSelectorContent(content: TokenSelectorContentUM, onDismiss: () -> Unit) {
    val hazeState = rememberHazeState()
    var topBarHeight by remember { mutableStateOf(0.dp) }

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.hazeSourceTangem(state = hazeState, 1f),
            contentPadding = PaddingValues(
                start = TangemTheme.dimens2.x4,
                end = TangemTheme.dimens2.x4,
                top = topBarHeight,
                bottom = TangemTheme.dimens2.x10,
            ),
        ) {
            tokenSelectorSectionItems(content.sections)
        }
        TokenSelectorSheetTopBar(
            modifier = Modifier.align(Alignment.TopEnd),
            onDismiss = onDismiss,
            hazeState = hazeState,
            onChangeHeight = { topBarHeight = it },
        )
        Fade(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            height = TangemTheme.dimens2.x10,
        )
    }
}

@Composable
private fun TokenSelectorSheetTopBar(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onChangeHeight: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = TangemTheme.colors2.surface.level2
    val density = LocalDensity.current
    TangemTopBar(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                if (coordinates.size.height > 0) {
                    with(density) {
                        onChangeHeight(coordinates.size.height.toDp())
                    }
                }
            }
            .hazeEffectTangem(state = hazeState) {
                backgroundColor = bgColor
                progressive = HazeProgressive.verticalGradient(
                    startIntensity = .55f,
                    endIntensity = 0f,
                    preferPerformance = true,
                    easing = EaseOut,
                )
            },
        type = TangemTopBarType.BottomSheet,
        title = resourceReference(R.string.markets_search_portfolio_header),
        endContent = {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_close_24),
                contentDescription = null,
                tint = TangemTheme.colors2.graphic.neutral.primary,
                modifier = Modifier
                    .size(TangemTheme.dimens2.x11)
                    .background(
                        color = TangemTheme.colors2.button.backgroundSecondary,
                        shape = CircleShape,
                    )
                    .clickableSingle(onClick = onDismiss)
                    .padding(TangemTheme.dimens2.x2_5),
            )
        },
    )
}

@Preview
@Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TokenSelectorBottomSheetPreview(
    @PreviewParameter(TokenSelectorContentPreviewProvider::class) content: TokenSelectorContentUM,
) {
    TangemThemePreviewRedesign {
        TokenSelectorBottomSheet(
            config = TangemBottomSheetConfig(
                onDismissRequest = {},
                content = content,
                isShown = true,
            ),
        )
    }
}