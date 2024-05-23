package com.tangem.features.details.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.component.preview.PreviewDetailsComponent
import com.tangem.features.details.impl.R
import com.tangem.features.details.state.DetailsBlock
import com.tangem.features.details.state.DetailsFooter
import com.tangem.features.details.state.DetailsState

private const val COLLAPSED_APP_BAR_THRESHOLD = 0.4f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailsScreen(component: DetailsComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsStateWithLifecycle()
    val backgroundColor = TangemTheme.colors.background.secondary
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    SystemBarsEffect {
        setSystemBarsColor(backgroundColor)
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = backgroundColor,
        topBar = { TopBar(state, scrollBehavior) },
    ) { paddingValues ->
        Content(
            modifier = Modifier.padding(paddingValues),
            state = state,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(state: DetailsState, scrollBehavior: TopAppBarScrollBehavior, modifier: Modifier = Modifier) {
    MediumTopAppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarColors(
            containerColor = TangemTheme.colors.background.secondary,
            scrolledContainerColor = TangemTheme.colors.background.secondary,
            navigationIconContentColor = TangemTheme.colors.icon.primary1,
            titleContentColor = TangemTheme.colors.text.primary1,
            actionIconContentColor = TangemTheme.colors.icon.primary1,
        ),
        title = {
            val collapsedStyle = TangemTheme.typography.subtitle1
            val expandedStyle = TangemTheme.typography.h1
            val style by remember(scrollBehavior.state.collapsedFraction) {
                derivedStateOf {
                    if (scrollBehavior.state.collapsedFraction >= COLLAPSED_APP_BAR_THRESHOLD) {
                        collapsedStyle
                    } else {
                        expandedStyle
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.details_title),
                style = style,
            )
        },
        navigationIcon = {
            IconButton(
                modifier = Modifier.size(TangemTheme.dimens.size32),
                onClick = state.popBack,
            ) {
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size24),
                    painter = painterResource(id = R.drawable.ic_back_24),
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun Content(state: DetailsState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        contentPadding = PaddingValues(
            top = TangemTheme.dimens.spacing16,
            bottom = TangemTheme.dimens.spacing16,
        ),
    ) {
        items(
            items = state.blocks,
            key = { block -> block.id },
        ) { block ->
            Block(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
                model = block,
            )
        }

        item {
            Footer(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing12),
                model = state.footer,
            )
        }
    }
}

@Composable
private fun Block(model: DetailsBlock, modifier: Modifier = Modifier) {
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
        when (model) {
            is DetailsBlock.Basic -> {
                model.items.forEach { item ->
                    BlockItem(
                        modifier = Modifier.fillMaxWidth(),
                        model = item,
                    )
                }
            }
            is DetailsBlock.Component -> {
                model.content(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Footer(model: DetailsFooter, modifier: Modifier = Modifier) {
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

        Text(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing6),
            text = model.appVersion,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_DetailsScreen() {
    TangemThemePreview {
        DetailsScreen(PreviewDetailsComponent(), modifier = Modifier.fillMaxSize())
    }
}
// endregion Preview
