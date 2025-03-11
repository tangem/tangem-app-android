package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsAppBarMenuConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarConfig
import com.tangem.features.tokendetails.impl.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TokenDetailsTopAppBar(config: TokenDetailsTopAppBarConfig) {
    var showDropdownMenu by rememberSaveable { mutableStateOf(false) }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = config.onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_24),
                    tint = TangemTheme.colors.icon.primary1,
                    contentDescription = "Back",
                )
            }
        },
        title = {},
        actions = {
            AnimatedVisibility(
                visible = config.tokenDetailsAppBarMenuConfig != null &&
                    config.tokenDetailsAppBarMenuConfig.items.isNotEmpty(),
            ) {
                IconButton(onClick = { showDropdownMenu = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vertical_24),
                        tint = TangemTheme.colors.icon.primary1,
                        contentDescription = "More",
                    )
                }
            }

            TangemDropdownMenu(
                expanded = showDropdownMenu,
                modifier = Modifier.background(TangemTheme.colors.background.primary),
                onDismissRequest = { showDropdownMenu = false },
                offset = DpOffset(x = TangemTheme.dimens.spacing20, y = TangemTheme.dimens.spacing10.times(-1)),
                content = {
                    config.tokenDetailsAppBarMenuConfig?.items?.fastForEach {
                        AppBarDropdownItem(
                            item = it,
                            dismissParent = { showDropdownMenu = false },
                        )
                    }
                },
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TangemTheme.colors.background.secondary,
            titleContentColor = TangemTheme.colors.icon.primary1,
            actionIconContentColor = TangemTheme.colors.icon.primary1,
        ),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    )
}

@Suppress("ComposableEventParameterNaming")
@Composable
private fun AppBarDropdownItem(
    item: TokenDetailsAppBarMenuConfig.MenuItem,
    dismissParent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .clickable {
                dismissParent()
                item.onClick()
            }
            .padding(vertical = TangemTheme.dimens.spacing8, horizontal = TangemTheme.dimens.spacing16),
        text = item.title.resolveReference(),
        style = TangemTheme.typography.body1.copy(color = item.textColorProvider()),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokenDetailsAppBarDropdownItem() {
    TangemThemePreview {
        AppBarDropdownItem(
            modifier = Modifier.background(TangemTheme.colors.background.primary),
            dismissParent = {},
            item = TokenDetailsAppBarMenuConfig.MenuItem(
                title = TextReference.Res(id = R.string.token_details_hide_token),
                textColorProvider = { TangemTheme.colors.text.warning },
                onClick = { },
            ),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokenDetailsTopAppBar() {
    TangemThemePreview {
        TokenDetailsTopAppBar(config = TokenDetailsPreviewData.tokenDetailsTopAppBarConfig)
    }
}