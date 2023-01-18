package com.tangem.tap.features.details.ui.resetcard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.details.ui.cardsettings.resolveReference
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.ScreenTitle
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
fun ResetCardScreen(state: ResetCardScreenState, onBackClick: () -> Unit) {
    SettingsScreensScaffold(
        content = { ResetCardView(state = state) },
        onBackClick = onBackClick,
        backgroundColor = Color.Transparent,
    )
}

@Suppress("LongMethod", "MagicNumber")
@Composable
fun ResetCardView(state: ResetCardScreenState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box {
            Image(
                painter = painterResource(id = R.drawable.ill_reset_background),
                contentDescription = null,
                modifier = Modifier.offset(y = (-82).dp),
            )
            ScreenTitle(titleRes = R.string.card_settings_reset_card_to_factory)
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.offset(y = (-32).dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = stringResource(id = R.string.common_attention),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                style = TangemTheme.typography.h3,
                color = TangemTheme.colors.text.primary1,
            )

            Spacer(modifier = Modifier.size(24.dp))

            Text(
                text = state.descriptionText.resolveReference(),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.secondary,
            )

            Spacer(modifier = Modifier.size(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = { state.onAcceptWarningToggleClick(!state.accepted) },
                    )
                    .padding(top = 16.dp, bottom = 16.dp),
            ) {
                IconToggleButton(
                    checked = state.accepted,
                    onCheckedChange = state.onAcceptWarningToggleClick,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp),
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
                            TangemTheme.colors.icon.accent
                        } else {
                            TangemTheme.colors.icon.secondary
                        },
                    )
                }
                Text(
                    text = stringResource(id = R.string.reset_card_to_factory_warning_message),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                    modifier = Modifier.padding(end = 20.dp),
                )
            }

            Spacer(modifier = Modifier.size(16.dp))
            Box(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
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

// region Preview
@Composable
private fun ResetCardScreenSample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.secondary),
    ) {
        ResetCardScreen(
            state = ResetCardScreenState(
                accepted = true,
                descriptionText = TextReference.Res(R.string.reset_card_with_backup_to_factory_message),
                onAcceptWarningToggleClick = {},
                onResetButtonClick = {},
            ),
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ResetCardScreenPreview_Light() {
    TangemTheme {
        ResetCardScreenSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ResetCardScreenPreview_Dark() {
    TangemTheme(isDark = true) {
        ResetCardScreenSample()
    }
}
// endregion Preview
