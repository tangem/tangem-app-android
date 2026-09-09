package com.tangem.features.introduction.impl.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_download_24
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.hotwallet.cloudrestore.CloudRestoreErrorMessage
import com.tangem.features.introduction.impl.ui.state.HaveWalletUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun HaveWalletBottomSheet(config: TangemBottomSheetConfig) {
    val step = (config.content as? HaveWalletUM)?.step

    TangemModal<HaveWalletUM>(
        config = config,
        presentation = TangemModal.Presentation.Dialog,
        // Back steps inside the sheet; on the first step it falls through to dismissing it.
        onBack = (step as? HaveWalletUM.Step.Import)?.onBack,
        title = { content ->
            TangemTopNavigation(
                title = when (content.step) {
                    is HaveWalletUM.Step.Options ->
                        resourceReference(R.string.introduction_button_i_have_a_wallet)
                    // The Import step carries its title in the body, so the bar keeps only its buttons.
                    is HaveWalletUM.Step.Import,
                    is HaveWalletUM.Step.Error,
                    -> stringReference("")
                },
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0.dp),
                fadeEnabled = false,
                onBack = (content.step as? HaveWalletUM.Step.Import)?.onBack,
                onClose = config.onDismissRequest,
            )
        },
    ) { content ->
        // No transitionSpec on purpose: the default one puts the fades and the size on the same
        // Transition, so the card's height and the content swap are a single event with a single
        // interruption model. Spelling either channel out by hand splits them again.
        Box(modifier = Modifier.fillMaxWidth().reportMeasuredHeight()) {
            AnimatedContent(
                targetState = content.step,
                // Steps are rebuilt on every state update and carry lambdas, so only their type is a stable key.
                contentKey = { it::class },
                contentAlignment = Alignment.TopCenter,
                label = "HaveWalletStep",
            ) { step ->
                when (step) {
                    is HaveWalletUM.Step.Options -> OptionsStep(step = step)
                    is HaveWalletUM.Step.Import -> ImportStep(step = step)
                    is HaveWalletUM.Step.Error -> ErrorStep(step = step)
                }
            }
        }
    }
}

@Composable
private fun OptionsStep(step: HaveWalletUM.Step.Options) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WalletOptionCard(option = step.cardOrRing)
        step.socialLogin?.let { WalletOptionCard(option = it) }
        WalletOptionCard(option = step.importWallet)
    }
}

@Composable
private fun ImportStep(step: HaveWalletUM.Step.Import) {
    StepBody(
        icon = StepIcon(
            image = Icons.ic_arrow_download_24,
            tint = TangemTheme.colors3.icon.status.info,
            background = TangemTheme.colors3.bg.status.infoSubtle,
        ),
        title = step.title.resolveReference(),
        description = step.description.resolveReference(),
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            text = step.recoveryPhraseButtonText,
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            isEnabled = !step.isCloudLoading,
            onClick = step.onRecoveryPhraseClick,
        )
        step.cloudBackupButtonText?.let { text ->
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                text = text,
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
                isLoading = step.isCloudLoading,
                onClick = step.onCloudBackupClick,
            )
        }
    }
}

@Composable
private fun ErrorStep(step: HaveWalletUM.Step.Error) {
    StepBody(
        icon = StepIcon(
            image = ImageVector.vectorResource(CoreUiR.drawable.ic_alert_triange_24),
            tint = if (step.message.isWarning) {
                TangemTheme.colors3.icon.status.warning
            } else {
                TangemTheme.colors3.icon.status.error
            },
            background = if (step.message.isWarning) {
                TangemTheme.colors3.bg.status.warningSubtle
            } else {
                TangemTheme.colors3.bg.status.errorSubtle
            },
        ),
        title = step.message.title.resolveReference(),
        description = step.message.body.resolveReference(),
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            text = resourceReference(R.string.common_got_it),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            onClick = step.onGotItClick,
        )
    }
}

private class StepIcon(val image: ImageVector, val tint: Color, val background: Color)

@Composable
private fun StepBody(icon: StepIcon, title: String, description: String, buttons: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(icon.background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = icon.image,
                contentDescription = null,
                tint = icon.tint,
            )
        }
        Text(
            modifier = Modifier.padding(top = 32.dp),
            text = title,
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = description,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = buttons,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OptionsStepPreview() {
    TangemThemePreviewRedesign(isDark = true) {
        OptionsStep(
            step = HaveWalletUM.Step.Options(
                cardOrRing = previewOption(R.string.introduction_create_wallet_hardware_title),
                socialLogin = previewOption(R.string.introduction_i_have_wallet_sign_in_google_title),
                importWallet = previewOption(R.string.hw_import_existing_wallet),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ImportStepPreview() {
    TangemThemePreviewRedesign(isDark = true) {
        ImportStep(
            step = HaveWalletUM.Step.Import(
                title = resourceReference(R.string.hw_import_existing_wallet),
                description = resourceReference(R.string.hw_import_existing_wallet_description),
                recoveryPhraseButtonText = resourceReference(R.string.hw_cloud_backup_restore_use_recovery_phrase),
                cloudBackupButtonText = resourceReference(R.string.hw_cloud_backup_service_name),
                isCloudLoading = false,
                onRecoveryPhraseClick = {},
                onCloudBackupClick = {},
                onBack = {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ErrorStepPreview() {
    TangemThemePreviewRedesign(isDark = true) {
        ErrorStep(
            step = HaveWalletUM.Step.Error(
                message = CloudRestoreErrorMessage(
                    title = resourceReference(R.string.hw_cloud_backup_error_title),
                    body = resourceReference(R.string.hw_cloud_backup_restore_error_with_recovery),
                    isWarning = false,
                ),
                onGotItClick = {},
            ),
        )
    }
}

private fun previewOption(titleRes: Int) = WalletOptionUM(
    iconRes = CoreUiR.drawable.ic_tangem_card_24,
    title = resourceReference(titleRes),
    description = resourceReference(R.string.introduction_create_wallet_seed_phrase_description),
    onClick = {},
)
// endregion Preview