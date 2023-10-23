package com.tangem.tap.features.details.ui.resetcard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
internal fun ResetCardScreen(state: ResetCardScreenState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    SettingsScreensScaffold(
        modifier = modifier,
        content = { ResetCardView(state = state) },
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Suppress("LongMethod", "MagicNumber")
@Composable
private fun ResetCardView(state: ResetCardScreenState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ScreenTitle(titleRes = R.string.card_settings_reset_card_to_factory)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 21.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.img_alert),
                contentDescription = "",
                tint = Color.Unspecified,
            )
        }
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

            ConditionCheckBox(
                checkedState = state.acceptCondition1Checked,
                onCheckedChange = state.onAcceptCondition1ToggleClick,
                description = TextReference.Res(R.string.reset_card_to_factory_condition_1),
            )

            ConditionCheckBox(
                checkedState = state.acceptCondition2Checked,
                onCheckedChange = state.onAcceptCondition2ToggleClick,
                description = TextReference.Res(R.string.reset_card_to_factory_condition_2),
            )

            Spacer(modifier = Modifier.size(16.dp))
            Box(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            ) {
                AnimatedContent(
                    targetState = state.resetButtonEnabled,
                    label = "Update checked state",
                ) { buttonEnabled ->
                    DetailsMainButton(
                        title = stringResource(id = R.string.reset_card_to_factory_button_title),
                        onClick = state.onResetButtonClick,
                        enabled = buttonEnabled,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ConditionCheckBox(checkedState: Boolean, onCheckedChange: (Boolean) -> Unit, description: TextReference) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = { onCheckedChange.invoke(!checkedState) },
            )
            .padding(top = TangemTheme.dimens.size16, bottom = TangemTheme.dimens.size16),
    ) {
        IconToggleButton(
            checked = checkedState,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = TangemTheme.dimens.size20, end = TangemTheme.dimens.size20),
        ) {
            AnimatedContent(targetState = checkedState, label = "Update checked state") { checked ->
                Icon(
                    painter = painterResource(
                        if (checked) {
                            R.drawable.ic_accepted
                        } else {
                            R.drawable.ic_unticked
                        },
                    ),
                    contentDescription = null,
                    tint = if (checked) {
                        TangemTheme.colors.icon.accent
                    } else {
                        TangemTheme.colors.icon.secondary
                    },
                )
            }
        }
        Text(
            text = description.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            modifier = Modifier.padding(end = TangemTheme.dimens.size20),
        )
    }
}

// region Preview
@Composable
private fun ResetCardScreenSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.secondary),
    ) {
        ResetCardScreen(
            state = ResetCardScreenState(
                accepted = true,
                descriptionText = TextReference.Res(R.string.reset_card_with_backup_to_factory_message),
                onAcceptCondition1ToggleClick = {},
                onAcceptCondition2ToggleClick = {},
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
