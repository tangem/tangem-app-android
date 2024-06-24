package com.tangem.core.ui.components.appbar.models

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

private const val COLLAPSED_APP_BAR_THRESHOLD = 0.4f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarMedium(
    title: TextReference,
    scrollBehavior: TopAppBarScrollBehavior,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIconResId: Int = R.drawable.ic_back_24,
    colors: TopAppBarColors = TangemTopAppBarColors,
) {
    MediumTopAppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        colors = colors,
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
                text = title.resolveReference(),
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(
                modifier = Modifier.size(TangemTheme.dimens.size32),
                onClick = onBackClick,
            ) {
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size24),
                    painter = painterResource(id = navigationIconResId),
                    contentDescription = null,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
internal val TangemTopAppBarColors: TopAppBarColors
    @Composable
    @ReadOnlyComposable
    get() = TopAppBarColors(
        containerColor = TangemTheme.colors.background.secondary,
        scrolledContainerColor = TangemTheme.colors.background.secondary,
        navigationIconContentColor = TangemTheme.colors.icon.primary1,
        titleContentColor = TangemTheme.colors.text.primary1,
        actionIconContentColor = TangemTheme.colors.icon.primary1,
    )