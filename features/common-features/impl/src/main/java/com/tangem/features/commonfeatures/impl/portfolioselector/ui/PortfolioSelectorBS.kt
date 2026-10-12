package com.tangem.features.commonfeatures.impl.portfolioselector.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.LocalBottomSheetContentScrollable
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.commonfeatures.impl.portfolioselector.entity.PortfolioSelectorUM

@Composable
internal fun PortfolioSelectorBS(
    state: PortfolioSelectorUM,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBack,
        containerColor = TangemTheme.colors3.bg.secondary,
        type = TangemBottomSheetType.Modal,
        title = {
            TangemTopNavigation(
                title = state.title,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = onDismiss,
            )
        },
        content = {
            val bottomInset = LocalTangemBottomSheetContentBottomInset.current
            val listBottomPadding = PaddingValues(bottom = bottomInset.coerceAtLeast(16.dp))

            val contentScrollable = LocalBottomSheetContentScrollable.current
            val lazyListState = rememberLazyListState()

            if (contentScrollable != null) {
                val isScrollable by remember {
                    derivedStateOf { lazyListState.canScrollForward || lazyListState.canScrollBackward }
                }

                LaunchedEffect(isScrollable) {
                    contentScrollable.value = isScrollable
                }
            }

            PortfolioSelectorContentV3(
                state = state,
                modifier = modifier.padding(horizontal = 16.dp),
                lazyListState = lazyListState,
                contentPadding = listBottomPadding,
            )
        },
        footer = {
            if (state.button != null) {
                SecondaryButton(
                    text = state.button.text.resolveReference(),
                    onClick = state.button.onClick,
                    enabled = state.button.isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        },
    )
}

@Composable
internal fun PortfolioSelectorModal(
    state: PortfolioSelectorUM,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBack,
        title = {
            TangemModal.TopNavigation(title = state.title, onClose = onDismiss)
        },
        content = {
            PortfolioSelectorColumnContent(
                state = state,
                modifier = modifier
                    .padding(top = 16.dp, bottom = if (state.button == null) 24.dp else 0.dp)
                    .padding(horizontal = 16.dp),
            )

            if (state.button != null) {
                TangemFooter(
                    windowInsets = WindowInsets(0),
                    fadeEnabled = false,
                    blurBackground = false,
                    modifier = Modifier.padding(bottom = 24.dp),
                ) {
                    TangemButton(
                        modifier = Modifier.fillMaxWidth(),
                        variant = TangemButton.Variant.Secondary,
                        size = TangemButton.Size.X12,
                        text = state.button.text,
                        isEnabled = state.button.isEnabled,
                        onClick = state.button.onClick,
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(PortfolioSelectorPreviewStateProvider::class) params: PortfolioSelectorUM) {
    TangemThemePreview {
        PortfolioSelectorBS(
            state = params,
            onDismiss = {},
            modifier = Modifier,
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewV2(@PreviewParameter(PortfolioSelectorPreviewStateProvider::class) params: PortfolioSelectorUM) {
    TangemThemePreviewRedesign {
        PortfolioSelectorBS(
            state = params,
            onDismiss = {},
            modifier = Modifier,
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewModal(@PreviewParameter(PortfolioSelectorPreviewStateProvider::class) params: PortfolioSelectorUM) {
    TangemThemePreviewRedesign {
        PortfolioSelectorModal(
            state = params,
            onDismiss = {},
            onBack = {},
        )
    }
}