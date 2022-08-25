package com.tangem.tap.features.details.ui.resetcard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.ScreenTitle
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
fun ResetCardScreen(
    state: ResetCardScreenState,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(colorResource(id = R.color.background_primary))) {
        Image(
            painter = painterResource(id = R.drawable.ic_reset_background),
            contentDescription = "",
            modifier = modifier.offset(y = (-16).dp),
        )
        SettingsScreensScaffold(
            content = { ResetCardView(state = state, modifier = modifier) },
            onBackClick = onBackPressed,
            backgroundColor = Color.Transparent,
        )
    }
}

@Composable
fun ResetCardView(
    state: ResetCardScreenState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = modifier
                .padding(bottom = 20.dp),
        ) {
            ScreenTitle(titleRes = R.string.reset_card_to_factory_navigation_title)
        }

        Column(
            modifier = modifier
                .layout { measurable, constraints ->
                    val placeable =
                        measurable.measure(
                            constraints.copy(
                                // left 200.dp for min image height
                                maxHeight = constraints.maxHeight - 215.dp.roundToPx(),
                                // occupy all height except full image square in case of smaller text
                                minHeight = constraints.maxHeight - constraints.maxWidth,
                            ),
                        )
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .height(IntrinsicSize.Min),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = stringResource(id = R.string.common_attention),
                modifier = modifier.padding(start = 20.dp, end = 20.dp),
                style = TangemTypography.headline3,
                color = colorResource(id = R.color.text_primary_1),
            )

            Spacer(modifier = modifier.size(24.dp))

            Text(
                text = stringResource(id = R.string.reset_card_to_factory_message),
                modifier = modifier
                    .padding(start = 20.dp, end = 20.dp)
                    .weight(1f, false)
                    .verticalScroll(rememberScrollState()),
                style = TangemTypography.body1,
                color = colorResource(id = R.color.text_secondary),
            )

            Spacer(modifier = modifier.size(44.dp))
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(end = 20.dp),
            ) {
                IconToggleButton(
                    checked = state.accepted,
                    onCheckedChange = state.onAcceptWarningToggleClick,
                    modifier = modifier.padding(start = 20.dp, end = 20.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (state.accepted) {
                                R.drawable.ic_accepted
                            } else {
                                R.drawable.ic_unticked
                            },
                        ),
                        contentDescription = null,
                        tint = if (state.accepted) {
                            colorResource(id = R.color.icon_accent)
                        } else {
                            colorResource(id = R.color.icon_secondary)
                        },
                    )
                }
                Text(
                    text = stringResource(id = R.string.reset_card_to_factory_warning_message),
                    style = TangemTypography.body2,
                    color = colorResource(id = R.color.text_secondary),
                )
            }

            Spacer(modifier = modifier.size(32.dp))
            Box(
                modifier = modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            ) {
                DetailsMainButton(
                    title = stringResource(id = R.string.reset_card_to_factory_button_title),
                    onClick = state.onResetButtonClick,
                    enabled = state.resetButtonEnabled,
                )
            }
        }
    }
}

@Composable
@Preview
fun ResetCardScreenPreview(

) {
    ResetCardScreen(state = ResetCardScreenState(onAcceptWarningToggleClick = {}, accepted = true) {}, {})
}