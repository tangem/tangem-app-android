package com.tangem.tap.features.details.ui.resetcard

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.details.ui.cardsettings.resolveReference
import com.tangem.tap.features.details.ui.common.DetailsMainButton
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R
import com.tangem.tap.features.details.ui.resetcard.ResetCardScreenState.Dialog as ResetCardDialog

@Composable
internal fun ResetCardScreen(state: ResetCardScreenState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    SettingsScreensScaffold(
        modifier = modifier,
        content = {
            ResetCardView(state = state)
        },
        onBackClick = onBackClick,
    )

    when (val dialog = state.dialog) {
        is ResetCardDialog.StartReset,
        is ResetCardDialog.ContinueReset,
        is ResetCardDialog.InterruptedReset,
        -> CommonResetDialog(dialog = dialog)
        is ResetCardDialog.CompletedReset -> CompletedResetDialog(dialog = dialog)
        null -> Unit
    }
}

@Composable
private fun ResetCardView(state: ResetCardScreenState) {
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
private fun Conditions(state: ResetCardScreenState) {
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
private fun CommonResetDialog(dialog: ResetCardScreenState.Dialog) {
    BasicDialog(
        title = stringResource(dialog.titleResId),
        message = stringResource(dialog.messageResId),
        dismissButton = DialogButton(
            title = stringResource(id = R.string.common_cancel),
            onClick = dialog.onDismiss,
        ),
        confirmButton = DialogButton(
            title = stringResource(id = R.string.card_settings_action_sheet_reset),
            warning = true,
            onClick = dialog.onConfirmClick,
        ),
        onDismissDialog = dialog.onDismiss,
    )
}

@Composable
private fun CompletedResetDialog(dialog: ResetCardDialog) {
    BasicDialog(
        title = stringResource(id = dialog.titleResId),
        message = stringResource(id = dialog.messageResId),
        confirmButton = DialogButton(
            title = stringResource(id = R.string.common_ok),
            onClick = dialog.onConfirmClick,
        ),
        onDismissDialog = {},
        isDismissable = false,
    )
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
                resetButtonEnabled = true,
                showResetPasswordButton = false,
                warningsToShow = listOf(ResetCardScreenState.WarningsToReset.LOST_WALLET_ACCESS),
                descriptionText = TextReference.Res(R.string.reset_card_with_backup_to_factory_message),
                acceptCondition1Checked = false,
                acceptCondition2Checked = false,
                onAcceptCondition1ToggleClick = {},
                onAcceptCondition2ToggleClick = {},
                onResetButtonClick = {},
                dialog = null,
            ),
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ResetCardScreenPreview() {
    TangemThemePreview {
        ResetCardScreenSample()
    }
}
// endregion Preview