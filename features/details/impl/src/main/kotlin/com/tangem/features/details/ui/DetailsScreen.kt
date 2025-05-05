package com.tangem.features.details.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.block.BlockItem
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TestTags
import com.tangem.features.details.component.preview.PreviewDetailsComponent
import com.tangem.features.details.entity.DetailsFooterUM
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.entity.DetailsUM
import com.tangem.features.details.impl.R

@Composable
internal fun DetailsScreen(
    state: DetailsUM,
    userWalletListBlockContent: ComposableContentComponent,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = TangemTheme.colors.background.secondary

    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        topBar = {
            TangemTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                startButton = TopAppBarButtonUM.Back(state.popBack),
            )
        },
    ) { paddingValues ->
        Content(
            modifier = Modifier.padding(paddingValues),
            state = state,
            userWalletListBlockContent = userWalletListBlockContent,
        )
    }

    SelectFeedbackEmailTypeBottomSheet(state.selectFeedbackEmailTypeBSConfig)
}

@Composable
private fun Content(
    state: DetailsUM,
    userWalletListBlockContent: ComposableContentComponent,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.testTag(TestTags.DETAILS_SCREEN),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        contentPadding = PaddingValues(
            top = TangemTheme.dimens.spacing12,
            bottom = TangemTheme.dimens.spacing16,
        ),
    ) {
        item {
            Text(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
                text = stringResourceSafe(R.string.details_title),
                style = TangemTheme.typography.h1,
                color = TangemTheme.colors.text.primary1,
            )
        }
        items(
            items = state.items,
            key = DetailsItemUM::id,
        ) { block ->
            Block(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
                model = block,
                userWalletListBlockContent = userWalletListBlockContent,
            )
        }

        item(key = "footer") {
            Footer(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing12),
                model = state.footer,
            )
        }
    }
}

@Composable
private fun Block(
    model: DetailsItemUM,
    userWalletListBlockContent: ComposableContentComponent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                shape = TangemTheme.shapes.roundedCornersXMedium,
                color = TangemTheme.colors.background.primary,
            ),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        val itemModifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.DETAILS_SCREEN_ITEM)

        when (model) {
            is DetailsItemUM.Basic -> {
                model.items.forEach { item ->
                    key(item.id) {
                        BlockItem(
                            modifier = itemModifier,
                            model = item.block,
                        )
                    }
                }
            }
            is DetailsItemUM.WalletConnect -> {
                WalletConnectBlock(
                    modifier = itemModifier,
                    onClick = model.onClick,
                )
            }
            is DetailsItemUM.UserWalletList -> {
                userWalletListBlockContent.Content(modifier = itemModifier)
            }
        }
    }
}

@Composable
private fun Footer(model: DetailsFooterUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(
                top = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing16,
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        val socialsScrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scrollable(socialsScrollState, orientation = Orientation.Horizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        ) {
            model.socials.forEach { social ->
                key(social.id) {
                    IconButton(
                        modifier = Modifier.size(TangemTheme.dimens.size32),
                        onClick = social.onClick,
                    ) {
                        Icon(
                            modifier = Modifier.size(TangemTheme.dimens.size24),
                            painter = painterResource(id = social.iconResId),
                            tint = TangemTheme.colors.icon.informative,
                            contentDescription = null,
                        )
                    }
                }
            }
        }

        Text(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing6),
            text = model.appVersion,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_DetailsScreen() {
    TangemThemePreview {
        PreviewDetailsComponent().Content(modifier = Modifier.fillMaxSize())
    }
}
// endregion Preview