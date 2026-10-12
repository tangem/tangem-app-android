package com.tangem.features.introduction.impl.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_download_24
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
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
        // Flat card for the same reason as the Create wallet sheet: the glass material would sample the
        // video behind the screen, and a dialog window costs half a second before the sheet can animate.
        isMaterialEnabled = false,
        // The overlay composes in the host, outside this screen's forced palette, so the modal forces its own.
        forceDarkTheme = true,
        containerColor = TangemTheme.colors3.bg.secondary,
        showDragIndicator = false,
        // Back steps inside the sheet; on the first step it falls through to dismissing it.
        onBack = (step as? HaveWalletUM.Step.Import)?.onBack,
        title = { content ->
            TangemModal.TopNavigation(
                startButton = { StepBackButton(onBack = (content.step as? HaveWalletUM.Step.Import)?.onBack) },
                endButton = { TangemButton.Close(onClick = content.onCloseClick) },
                contentColumn = { StepTitle(step = content.step) },
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

/**
 * Always in the bar's start slot, so the bar never animates the slot itself: its slide drops the button's material
 * for as long as it runs and then brings the shadow in after the button has already arrived. Fading in place lets
 * the button arrive whole, together with the step it belongs to.
 */
@Composable
private fun StepBackButton(onBack: (() -> Unit)?) {
    val displayedOnBack = rememberLastNonNull(onBack)
    AnimatedVisibility(visible = onBack != null, enter = fadeIn(), exit = fadeOut()) {
        displayedOnBack?.let { TangemButton.Back(onClick = it) }
    }
}

/**
 * The Import and Error steps carry their title in the body, so the bar keeps only its buttons. The title fades out
 * in place instead of being swapped for an empty one: the bar's own title slot animates its width along with the
 * cross-fade, which clips the outgoing words away one by one as the slot shrinks to nothing.
 */
@Composable
private fun ColumnScope.StepTitle(step: HaveWalletUM.Step) {
    AnimatedVisibility(
        visible = step is HaveWalletUM.Step.Options,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        TangemNavigationText(
            text = resourceReference(R.string.introduction_button_i_have_a_wallet),
            role = TangemNavigationText.Role.Title,
        )
    }
}

@Composable
private fun OptionsStep(step: HaveWalletUM.Step.Options) {
    // The rows bring their own 16 dp of padding, which is the inset the design asks for; the 4 dp on top of it
    // is what the design leaves between them.
    Column(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                cardOrRing = previewOption(
                    titleRes = R.string.introduction_create_wallet_hardware_title,
                    descriptionRes = R.string.introduction_i_have_wallet_hardware_description,
                ),
                socialLogin = previewOption(
                    titleRes = R.string.introduction_i_have_wallet_sign_in_google_title,
                    descriptionRes = R.string.introduction_create_wallet_seed_phrase_description,
                ),
                importWallet = previewOption(
                    titleRes = R.string.hw_import_existing_wallet,
                    descriptionRes = R.string.hw_import_existing_wallet_description,
                ),
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

private fun previewOption(titleRes: Int, descriptionRes: Int) = WalletOptionUM(
    icon = Icons.ic_logo_tangem_20,
    title = resourceReference(titleRes),
    description = resourceReference(descriptionRes),
    onClick = {},
)
// endregion Preview