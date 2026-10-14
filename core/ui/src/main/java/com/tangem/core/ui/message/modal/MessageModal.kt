package com.tangem.core.ui.message.modal

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TangemModalActionScope
import com.tangem.core.ui.ds2.modal.TangemModalContent
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_warning_28
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import kotlinx.collections.immutable.ImmutableList

/**
 * Modal for one-off messages, the [TangemModal] replacement for
 * [com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheet]: a close button with an optional
 * [MessageModalUM.title], one content template ([MessageModalUM.content], see [TangemModalContent]) and the
 * [MessageModalUM.buttons] in the footer.
 *
 * The modal is shown for as long as it is in composition. Drop it from composition in [onDismissRequest]
 * and it plays the collapse animation on the way out.
 *
 * @param state Content of the modal. See [messageModalUM].
 * @param onDismissRequest Called after [MessageModalUM.onDismiss] on every dismissal: the close button, back,
 *   the scrim, or an action calling [TangemModalActionScope.dismiss].
 */
@Composable
fun MessageModal(state: MessageModalUM, onDismissRequest: () -> Unit) {
    val actionScope = rememberActionScope(state = state, onDismissRequest = onDismissRequest)

    TangemModal<MessageModalUM>(
        config = TangemBottomSheetConfig(
            isShown = true,
            content = state,
            onDismissRequest = actionScope::dismiss,
        ),
        onBack = actionScope::dismiss,
        scrollableContent = false,
        title = { content ->
            TangemModal.TopNavigation(
                endButton = {
                    TangemButton.Close(
                        modifier = Modifier.testTag(WarningBottomSheetTestTags.CLOSE_BUTTON),
                        onClick = actionScope::dismiss,
                    )
                },
                contentColumn = content.title?.let { title ->
                    { TangemNavigationText(text = title, role = TangemNavigationText.Role.Title) }
                },
            )
        },
    ) { content ->
        MessageModalContent(
            state = content,
            modifier = Modifier.weight(weight = 1f, fill = false),
            onDismissRequest = onDismissRequest,
        )
    }
}

/**
 * Body of [MessageModal] without the modal around it, for screens that show the message inline: the content,
 * scrolling when the space is bounded, and the footer buttons under it.
 *
 * @param state Content. See [messageModalUM].
 * @param modifier Modifier for the root column.
 * @param onDismissRequest Called after [MessageModalUM.onDismiss] when an action calls
 *   [TangemModalActionScope.dismiss].
 */
@Composable
fun MessageModalContent(state: MessageModalUM, modifier: Modifier = Modifier, onDismissRequest: () -> Unit = {}) {
    val actionScope = rememberActionScope(state = state, onDismissRequest = onDismissRequest)

    Column(modifier = modifier) {
        TangemModalContent(
            state = state.content,
            actionScope = actionScope,
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .verticalScroll(rememberScrollState()),
        )
        Buttons(buttons = state.buttons, actionScope = actionScope)
    }
}

@Composable
private fun rememberActionScope(state: MessageModalUM, onDismissRequest: () -> Unit): TangemModalActionScope {
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    return remember(state) {
        TangemModalActionScope {
            state.onDismiss()
            currentOnDismissRequest()
        }
    }
}

@Composable
private fun Buttons(buttons: ImmutableList<MessageModalUM.Button>, actionScope: TangemModalActionScope) {
    if (buttons.isEmpty()) return
    TangemFooter(
        modifier = Modifier.fillMaxWidth(),
        windowInsets = TangemFooter.ModalWindowInsets,
        fadeVariant = TangemFade.Variant.Hard,
        fadeEnabled = false,
    ) {
        TangemButtonGroup(modifier = Modifier.fillMaxWidth()) {
            buttons.fastForEach { button ->
                TangemButton(
                    modifier = Modifier.testTag(
                        if (button.variant == TangemButton.Variant.Primary) {
                            WarningBottomSheetTestTags.BUTTON_PRIMARY
                        } else {
                            WarningBottomSheetTestTags.BUTTON_SECONDARY
                        },
                    ),
                    variant = button.variant,
                    size = TangemButton.Size.X12,
                    iconStart = button.iconStart,
                    iconEnd = button.iconEnd,
                    text = button.text,
                    onClick = { button.onClick(actionScope) },
                )
            }
        }
    }
}

// region Preview
// The previews show the modal's body on a static card: TangemModal's preview path still opens a Material3
// sheet window, which layoutlib cannot compose.
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessageModalPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors3.bg.primary),
        ) {
            TangemModal.TopNavigation(endButton = { TangemButton.Close(onClick = {}) })
            MessageModalContent(
                state = messageModalUM {
                    status {
                        variant = TangemModalStatus.Variant.Warning
                        title = stringReference(value = "Backup your wallet")
                        description = stringReference(
                            value = "You haven't backed up this wallet yet. " +
                                "Without a backup you may lose access to your funds.",
                        )
                        glyph(Icons.ic_warning_28)
                        badge(text = stringReference(value = "Recommended"), status = TangemBadge.Status.Warning)
                    }
                    primaryButton { text = stringReference(value = "Back up now") }
                    secondaryButton { text = stringReference(value = "Later") }
                },
                modifier = Modifier.weight(weight = 1f, fill = false),
            )
        }
    }
}
// endregion