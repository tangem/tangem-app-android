package com.tangem.tap.features.details.ui.resetcard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.details.ui.cardsettings.resolveReference
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
internal fun ResetCardScreen(state: ResetCardScreenState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    SettingsScreensScaffold(
        modifier = modifier,
        content = {
            when (state) {
                is ResetCardScreenState.ResetCardScreenContent -> ResetCardView(state = state)
                ResetCardScreenState.InitialState -> {
                    // do nothing for now, just white screen
                }
            }
        },
        onBackClick = onBackClick,
    )

    LastWarningDialog(state = state)
}

@Composable
private fun ResetCardView(state: ResetCardScreenState.ResetCardScreenContent) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // set scrollState after fillMaxSize
            .padding(horizontal = TangemTheme.dimens.spacing20),
    ) {
        Title()
        SpacerH24()
        AlertImage()
        SpacerH24()
        Subtitle()
        SpacerH16()
        Description(text = state.descriptionText)
        SpacerH12()
        Conditions(state)
        DynamicSpacer(scrollState = scrollState)
        SpacerH16()
        ResetButton(enabled = state.resetButtonEnabled, onResetButtonClick = state.onResetButtonClick)
        SpacerH16()
    }
}

@Composable
private fun Title() {
    Text(
        text = stringResource(id = R.string.card_settings_reset_card_to_factory),
        style = TangemTheme.typography.h1,
        color = TangemTheme.colors.text.primary1,
    )
}

@Composable
private fun AlertImage() {
    Image(
        painter = painterResource(id = R.drawable.img_alert_80),
        contentDescription = null,
        modifier = Modifier.size(TangemTheme.dimens.size80),
    )
}

@Composable
private fun Subtitle() {
    Text(
        text = stringResource(id = R.string.common_attention),
        style = TangemTheme.typography.h3,
        color = TangemTheme.colors.text.primary1,
    )
}

@Composable
private fun Description(text: TextReference) {
    Text(
        text = text.resolveReference(),
        style = TangemTheme.typography.body1,
        color = TangemTheme.colors.text.secondary,
    )
}

@Composable
private fun Conditions(state: ResetCardScreenState.ResetCardScreenContent) {
    state.warningsToShow.forEach {
        when (it) {
            ResetCardScreenState.WarningsToReset.LOST_WALLET_ACCESS -> {
                ConditionCheckBox(
                    checkedState = state.acceptCondition1Checked,
                    onCheckedChange = state.onAcceptCondition1ToggleClick,
                    description = TextReference.Res(R.string.reset_card_to_factory_condition_1),
                )
            }

            ResetCardScreenState.WarningsToReset.LOST_PASSWORD_RESTORE -> {
                ConditionCheckBox(
                    checkedState = state.acceptCondition2Checked,
                    onCheckedChange = state.onAcceptCondition2ToggleClick,
                    description = TextReference.Res(R.string.reset_card_to_factory_condition_2),
                )
            }
        }
    }
}

@Composable
private fun ConditionCheckBox(checkedState: Boolean, onCheckedChange: (Boolean) -> Unit, description: TextReference) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange.invoke(!checkedState) })
            .padding(vertical = TangemTheme.dimens.size16),
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing16),
    ) {
        IconToggleButton(checked = checkedState, onCheckedChange = onCheckedChange) {
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
                        TangemTheme.colors.control.checked
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
        )
    }
}

/**
 * It's helps to create an adaptive layout.
 * ResetButton will be attach to the bottom of large screen or will be inside scroll layout of small screen.
 *
 * @param scrollState flag determines if screen is small (has scroll) or large (hasn't scroll)
 */
@Composable
private fun ColumnScope.DynamicSpacer(scrollState: ScrollState) {
    if (!scrollState.canScrollBackward && !scrollState.canScrollForward) {
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ResetButton(enabled: Boolean, onResetButtonClick: () -> Unit) {
    DetailsMainButton(
        title = stringResource(id = R.string.reset_card_to_factory_button_title),
        onClick = onResetButtonClick,
        enabled = enabled,
    )
}

@Composable
private fun LastWarningDialog(state: ResetCardScreenState) {
    if (state is ResetCardScreenState.ResetCardScreenContent && state.lastWarningDialog.isShown) {
        BasicDialog(
            title = stringResource(id = R.string.common_attention),
            message = stringResource(id = R.string.card_settings_action_sheet_title),
            dismissButton = DialogButton(
                title = stringResource(id = R.string.card_settings_action_sheet_reset),
                warning = true,
                onClick = state.lastWarningDialog.onResetButtonClick,
            ),
            confirmButton = DialogButton(
                title = stringResource(id = R.string.common_cancel),
                onClick = state.lastWarningDialog.onDismiss,
            ),
            onDismissDialog = state.lastWarningDialog.onDismiss,
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
            state = ResetCardScreenState.ResetCardScreenContent(
                accepted = true,
                warningsToShow = listOf(ResetCardScreenState.WarningsToReset.LOST_WALLET_ACCESS),
                descriptionText = TextReference.Res(R.string.reset_card_with_backup_to_factory_message),
                onAcceptCondition1ToggleClick = {},
                onAcceptCondition2ToggleClick = {},
                onResetButtonClick = {},
                lastWarningDialog = ResetCardScreenState.ResetCardScreenContent.LastWarningDialog(
                    isShown = false,
                    onResetButtonClick = {},
                    onDismiss = {},
                ),
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