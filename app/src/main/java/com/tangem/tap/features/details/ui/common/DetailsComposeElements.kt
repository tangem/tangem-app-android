package com.tangem.tap.features.details.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
fun ScreenTitle(
    titleRes: Int,
    modifier: Modifier = Modifier,
) {
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
fun DetailsMainButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(48.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(R.color.button_primary),
            contentColor = colorResource(R.color.text_primary_2),
            disabledBackgroundColor = colorResource(R.color.button_disabled),
            disabledContentColor = colorResource(R.color.text_disabled),
        ),
    ) {
        Text(text = title)
        Spacer(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp)
                .size(8.dp),
        )
        Icon(painter = painterResource(id = R.drawable.ic_tangem_24), contentDescription = "")
    }
}
