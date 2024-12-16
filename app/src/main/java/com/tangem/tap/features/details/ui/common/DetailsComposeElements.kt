package com.tangem.tap.features.details.ui.common

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.wallet.R

@Composable
internal fun SettingsScreensScaffold(
    onBackClick: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    addBottomInsets: Boolean = true,
    fab: @Composable () -> Unit = {},
) {
    val state = rememberScaffoldState(snackbarHostState = snackbarHostState)
    val backgroundColor = TangemTheme.colors.background.secondary

    BackHandler(onBack = onBackClick)

    Scaffold(
        scaffoldState = state,
        topBar = {
            EmptyTopBarWithNavigation(
                modifier = Modifier.statusBarsPadding(),
                onBackClick = onBackClick,
                backgroundColor = backgroundColor,
            )
        },
        modifier = modifier,
        contentWindowInsets = WindowInsetsZero,
        backgroundColor = backgroundColor,
        floatingActionButton = {
            Box(modifier = Modifier.navigationBarsPadding()) {
                fab()
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .run {
                        if (addBottomInsets) {
                            navigationBarsPadding()
                        } else {
                            this
                        }
                    }
                    .padding(paddingValues)
                    .fillMaxSize(),
            ) {
                if (titleRes != null) {
                    Text(
                        text = stringResourceSafe(id = titleRes),
                        modifier = Modifier
                            .padding(horizontal = TangemTheme.dimens.spacing20)
                            .padding(bottom = TangemTheme.dimens.spacing36),
                        style = TangemTheme.typography.h1,
                        color = TangemTheme.colors.text.primary1,
                    )
                }

                content()
            }
        },
    )
}

@Composable
internal fun ScreenTitle(titleRes: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResourceSafe(id = titleRes),
        modifier = modifier.padding(start = 20.dp, end = 20.dp),
        style = TangemTheme.typography.h1,
        color = TangemTheme.colors.text.primary1,
    )
}

@Composable
internal fun EmptyTopBarWithNavigation(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = TangemTheme.colors.background.primary,
) {
    TopAppBar(
        modifier = modifier,
        title = { },
        navigationIcon =
        {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.primary1,
                )
            }
        },
        backgroundColor = backgroundColor,
        elevation = 0.dp,
    )
}

@Composable
internal fun DetailsMainButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PrimaryButtonIconEnd(
        text = title,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        iconResId = R.drawable.ic_tangem_24,
    )
}

@Composable
internal fun DetailsRadioButtonElement(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = { onClick() },
            )
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.padding(end = 20.dp),
            colors = RadioButtonDefaults.colors(
                unselectedColor = TangemTheme.colors.icon.secondary,
                selectedColor = TangemTheme.colors.icon.accent,
            ),
        )

        Column {
            Text(
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = subtitle,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        }
    }
}