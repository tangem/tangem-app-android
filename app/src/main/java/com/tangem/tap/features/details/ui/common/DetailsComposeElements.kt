package com.tangem.tap.features.details.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.res.TangemTheme
import com.tangem.wallet.R

@Composable
fun SettingsScreensScaffold(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    background: @Composable (() -> Unit)? = null,
    fab: @Composable (() -> Unit)? = null,
    backgroundColor: Color = TangemTheme.colors.background.secondary,
    titleRes: Int? = null,
    onBackClick: () -> Unit,
) {
    BackHandler(true, onBackClick)

    Scaffold(
        topBar = {
            EmptyTopBarWithNavigation(
                onBackClick = onBackClick,
                backgroundColor = backgroundColor,
            )
        },
        modifier = modifier.systemBarsPadding(),
        backgroundColor = backgroundColor,
        floatingActionButton = { fab?.invoke() },
    ) {
        if (titleRes != null) {
            Box(modifier = modifier.fillMaxSize()) {
                background?.invoke()

                Column(modifier = modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(id = titleRes),
                        modifier = modifier.padding(
                            start = TangemTheme.dimens.spacing20,
                            end = TangemTheme.dimens.spacing20,
                            bottom = TangemTheme.dimens.spacing54,
                        ),
                        style = TangemTheme.typography.h1,
                        color = TangemTheme.colors.text.primary1,
                    )
                    content()
                }
            }
        } else {
            content()
        }
    }
}

@Composable
fun ScreenTitle(titleRes: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = titleRes),
        modifier = modifier.padding(start = 20.dp, end = 20.dp),
        style = TangemTheme.typography.h1,
        color = TangemTheme.colors.text.primary1,
    )
}

@Composable
fun EmptyTopBarWithNavigation(
    onBackClick: () -> Unit,
    backgroundColor: Color = TangemTheme.colors.background.primary,
) {
    TopAppBar(
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
fun DetailsMainButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
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
fun DetailsRadioButtonElement(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
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
                unselectedColor = colorResource(id = R.color.icon_secondary),
                selectedColor = colorResource(id = R.color.icon_accent),
            ),
        )

        Column {
            Text(
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = colorResource(id = R.color.text_primary_1),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = subtitle,
                style = TangemTheme.typography.body2,
                color = colorResource(id = R.color.text_secondary),
            )
        }
    }
}