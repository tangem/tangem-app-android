package com.tangem.features.collectibles.impl.onboarding.ui

import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.onboarding.model.converter.CollectiblesOnboardingUMConverter
import com.tangem.features.collectibles.impl.onboarding.ui.state.CollectiblesOnboardingUM

private val FOOTER_CLEARANCE = 54.dp

/**
 * Onboarding screen of the Collectibles feature: scrolling body under an overlaid top navigation, with a
 * pinned footer carrying the legal line and the CTA.
 *
 * [Figma](https://www.figma.com/design/HfAxhwUUpSE0y1wSAC2Tua/%F0%9F%90%B1-Gacha?node-id=531-43088)
 *
 * The footer's measured height is fed back into the body as trailing scroll space, so the last FAQ
 * answer can clear it instead of resting underneath.
 *
 * @param state screen state — the CTA's loader and every callback.
 * @param modifier applied to the scaffold root.
 */
@Composable
internal fun CollectiblesOnboardingScreen(state: CollectiblesOnboardingUM, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    var footerHeight by remember { mutableIntStateOf(0) }

    // The bar sits over a clean hero area, but content scrolling under a bare bar is unreadable — so the
    // DS scrim is off only while the top of the page is at rest.
    val isTopBarScrimShown by remember(scrollState) { derivedStateOf { scrollState.value > 0 } }

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                fadeEnabled = isTopBarScrimShown,
                endButton = { TangemButton.Close(onClick = state.onCloseClick) },
            )
        },
        overlay = { contentPadding ->
            CollectiblesOnboardingFooter(
                state = state,
                contentPadding = contentPadding,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { footerHeight = it.height },
            )
        },
    ) { contentPadding ->
        CollectiblesOnboardingContent(
            state = state,
            scrollState = scrollState,
            contentPadding = contentPadding,
            trailingSpace = with(LocalDensity.current) { footerHeight.toDp() } + FOOTER_CLEARANCE,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesOnboardingScreenPreview() {
    TangemThemePreviewRedesign {
        CollectiblesOnboardingScreen(state = previewState(isCreatingAccount = false))
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesOnboardingScreenCreatingPreview() {
    TangemThemePreviewRedesign {
        CollectiblesOnboardingScreen(state = previewState(isCreatingAccount = true))
    }
}

private fun previewState(isCreatingAccount: Boolean) = CollectiblesOnboardingUMConverter()
    .convert(
        value = CollectiblesOnboardingUMConverter.Callbacks(
            onCloseClick = {},
            onCreateAccountClick = {},
            onCollectiblesTermsClick = {},
            onTangemTermsClick = {},
        ),
    )
    .copy(isCreatingAccount = isCreatingAccount)