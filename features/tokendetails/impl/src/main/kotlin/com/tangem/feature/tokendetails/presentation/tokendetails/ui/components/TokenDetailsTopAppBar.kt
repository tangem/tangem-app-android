package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownItem
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenu
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TokenDetailsTopBarTestTags
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
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
                    modifier = Modifier.testTag(TokenDetailsTopBarTestTags.BACK_BUTTON)
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
                        modifier = Modifier.testTag(TokenDetailsTopBarTestTags.MORE_BUTTON)
                    )
                }
            }

            TangemDropdownMenu(
                expanded = showDropdownMenu,
                modifier = Modifier.background(TangemTheme.colors.background.primary),
                onDismissRequest = { showDropdownMenu = false },
                content = {
                    config.tokenDetailsAppBarMenuConfig?.items?.fastForEach {
                        TangemDropdownItem(
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokenDetailsTopAppBar() {
    TangemThemePreview {
        TokenDetailsTopAppBar(config = TokenDetailsPreviewData.tokenDetailsTopAppBarConfig)
    }
}