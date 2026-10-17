@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.modalstatus

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.*
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_20
import com.tangem.core.ui.res.generated.icons.ic_warning_28
import com.tangem.feature.tester.presentation.storybook.entity.TangemModalStatusExample
import com.tangem.feature.tester.presentation.storybook.entity.TangemModalStatusStory
import com.tangem.feature.tester.presentation.storybook.page.ds.TransparentSystemBarsEffect
import dev.chrisbanes.haze.rememberHazeState

private val TetherGreen = Color(0xFF26A17B)
private val MerchantBlack = Color(0xFF1C1C1E)
private val RecipientPurple = Color(0xFF7B61FF)

@Composable
internal fun TangemModalStatusStory(state: TangemModalStatusStory, modifier: Modifier = Modifier) {
    TransparentSystemBarsEffect()

    // The tester activity has no root modal host, so the page brings its own — the examples then open
    // as real same-window modals with the backdrop blur, as they do in the app.
    val modalHostState = rememberTangemModalHostState()
    val modalHazeState = rememberHazeState()

    CompositionLocalProvider(LocalTangemModalHost provides modalHostState) {
        Box(modifier = modifier.fillMaxSize()) {
            Controls(
                state = state,
                modifier = Modifier
                    .matchParentSize()
                    .hazeSourceTangem(state = modalHazeState),
            )
            TangemModalHost(
                state = modalHostState,
                hazeState = modalHazeState,
                modifier = Modifier.matchParentSize(),
            )
        }

        ExampleModal(example = state.openedExample, onDismiss = state.onExampleDismiss)
        if (state.isMessageSheetShown) {
            MessageBottomSheetV3(
                state = sampleMessage(),
                onDismissRequest = { state.onMessageSheetShownChange(false) },
            )
        }
    }
}

@Composable
private fun Controls(state: TangemModalStatusStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .statusBarsPadding()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComponentPreview(state = state)
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section(label = "Variant") {
                ChipGrid(
                    items = TangemModalStatus.Variant.entries,
                    label = { it.name },
                    isSelected = { it == state.variant },
                    onSelect = state.onVariantChange,
                )
            }
            Section(label = "Title style") {
                ChipGrid(
                    items = TangemModalStatus.TitleStyle.entries,
                    label = { it.name },
                    isSelected = { it == state.titleStyle },
                    onSelect = state.onTitleStyleChange,
                )
            }
            SlotToggles(state = state)
            Examples(onOpen = state.onExampleOpen, onMessageSheetOpen = { state.onMessageSheetShownChange(true) })
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemModalStatusStory) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.primary),
    ) {
        val isAmount = state.titleStyle == TangemModalStatus.TitleStyle.Amount
        TangemModalStatus(
            variant = state.variant,
            title = stringReference(value = if (isAmount) "-$166.84" else "KYC in progress"),
            description = if (state.hasDescription) {
                stringReference(value = "Use the buttons below to view your current KYC status or cancel it")
            } else {
                null
            },
            titleStyle = state.titleStyle,
            asset = if (state.hasCustomAsset) {
                { PreviewLogo(color = TetherGreen, glyph = "₮") }
            } else {
                null
            },
            extraBottom = if (state.hasExtraBottom) {
                {
                    TangemBadge(
                        text = stringReference(value = "Good luck next time"),
                        status = TangemBadge.Status.Error,
                    )
                }
            } else {
                null
            },
            details = if (state.hasDetails) {
                {
                    DetailsRow(title = "Qty of Tokens", value = "248.50", divider = true)
                    DetailsRow(title = "Price", value = "$0.81", divider = true)
                    DetailsRow(title = "Fee", value = "$5.00", divider = false)
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun SlotToggles(state: TangemModalStatusStory) {
    Section(label = "Slots") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "description", checked = state.hasDescription, onToggle = state.onDescriptionToggle)
            ToggleRow(label = "custom asset", checked = state.hasCustomAsset, onToggle = state.onCustomAssetToggle)
            ToggleRow(
                label = "extra bottom (badge)",
                checked = state.hasExtraBottom,
                onToggle = state.onExtraBottomToggle,
            )
            ToggleRow(label = "details", checked = state.hasDetails, onToggle = state.onDetailsToggle)
        }
    }
}

@Composable
private fun Examples(onOpen: (TangemModalStatusExample) -> Unit, onMessageSheetOpen: () -> Unit) {
    Section(label = "Examples") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemModalStatusExample.entries.forEach { example ->
                ActionRow(label = example.label, onClick = { onOpen(example) })
            }
            ActionRow(label = "MessageBottomSheetV3 (DSL)", onClick = onMessageSheetOpen)
        }
    }
}

/** A message built with the shared `messageBottomSheetUM` DSL, rendered by [MessageBottomSheetV3]. */
private fun sampleMessage(): MessageBottomSheetUM = messageBottomSheetUM {
    infoBlock {
        vector(Icons.ic_warning_28) { type = MessageBottomSheetUM.Vector.Type.Attention }
        title = stringReference(value = "Backup your wallet")
        body = stringReference(
            value = "You haven't backed up this wallet yet. Without a backup you may lose access to your funds.",
        )
        chip(text = stringReference(value = "Recommended")) { type = MessageBottomSheetUM.Chip.Type.Warning }
    }
    primaryButton {
        text = stringReference(value = "Back up now")
        onClick { closeBs() }
    }
    secondaryButton {
        text = stringReference(value = "Later")
        onClick { closeBs() }
    }
}

@Composable
private fun ExampleModal(example: TangemModalStatusExample?, onDismiss: () -> Unit) {
    val displayedExample = rememberLastNonNull(example)

    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = example != null,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onDismiss,
        scrollableContent = false,
        title = {
            TangemModal.TopNavigation(endButton = { TangemButton.Close(onClick = onDismiss) })
        },
    ) {
        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            if (displayedExample != null) {
                ExampleStatus(example = displayedExample)
            }
        }
        if (displayedExample != null) {
            ExampleFooter(example = displayedExample, onClick = onDismiss)
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ExampleStatus(example: TangemModalStatusExample) {
    when (example) {
        TangemModalStatusExample.KycInProgress -> TangemModalStatus(
            variant = TangemModalStatus.Variant.Info,
            title = stringReference(value = "KYC in progress"),
            description = stringReference(
                value = "Use the buttons below to view your current KYC status or cancel it",
            ),
        )
        TangemModalStatusExample.ErrorWithTwoActions -> TangemModalStatus(
            variant = TangemModalStatus.Variant.Warning,
            title = stringReference(value = "An error occured"),
            description = stringReference(
                value = "Something went wrong, but you still have an unopened pack. You can open it right now " +
                    "or stay here and continue. Pack will be available on account's main screen",
            ),
        )
        TangemModalStatusExample.NotAvailable -> TangemModalStatus(
            variant = TangemModalStatus.Variant.Error,
            title = stringReference(value = "Virtual account not available"),
            description = stringReference(
                value = "Unfortunately, virtual accounts are not available in your country yet. " +
                    "We're working on expanding to more regions",
            ),
        )
        TangemModalStatusExample.PredictionAmount -> TangemModalStatus(
            variant = TangemModalStatus.Variant.Error,
            title = stringReference(value = "-$166.84"),
            description = stringReference(value = "Will Ethereum reach $5,000 before Sep 30?"),
            titleStyle = TangemModalStatus.TitleStyle.Amount,
            asset = { PreviewLogo(color = TetherGreen, glyph = "₮") },
            extraBottom = {
                TangemBadge(text = stringReference(value = "Good luck next time"), status = TangemBadge.Status.Error)
            },
            details = {
                DetailsRow(title = "Qty of Tokens", value = "248.50", divider = true)
                DetailsRow(title = "Price", value = "$0.81", divider = true)
                DetailsRow(title = "Qty of Tokens", value = "248.50", divider = true)
                DetailsRow(title = "Fee", value = "$5.00", divider = true)
                DetailsRow(title = "Notional Gross", value = "$166.50", divider = false)
            },
        )
        TangemModalStatusExample.PaymentCompleted -> TangemModalStatus(
            variant = TangemModalStatus.Variant.Success,
            title = stringReference(value = "-$10.00"),
            description = stringReference(value = "NuAppleric"),
            titleStyle = TangemModalStatus.TitleStyle.Amount,
            asset = { PreviewLogo(color = MerchantBlack, glyph = "") },
            details = {
                CompletedRow()
                DetailsRow(title = "Network fee", value = "$5.00", divider = false)
            },
        )
        TangemModalStatusExample.PredictionLoss -> TangemModalStatus(
            variant = TangemModalStatus.Variant.Error,
            title = stringReference(value = "Your Prediction Loss"),
            description = stringReference(value = "Will Ethereum reach $5,000 before Sep 30?"),
            asset = { PreviewLogo(color = TetherGreen, glyph = "₮") },
            extraBottom = {
                TangemBadge(text = stringReference(value = "Your bet: Yes"), status = TangemBadge.Status.Success)
            },
            details = {
                RecipientCard(name = "Danil Kolbasenko")
                DetailsRow(title = "Network fee", value = "$5.00", divider = false)
            },
        )
    }
}

@Composable
private fun ExampleFooter(example: TangemModalStatusExample, onClick: () -> Unit) {
    TangemFooter(
        modifier = Modifier.fillMaxWidth(),
        windowInsets = TangemFooter.ModalWindowInsets,
        fadeVariant = TangemFade.Variant.Hard,
        fadeEnabled = false,
    ) {
        if (example == TangemModalStatusExample.ErrorWithTwoActions) {
            FooterButton(text = "Open pack", variant = TangemButton.Variant.Secondary, onClick = onClick)
            Box(modifier = Modifier.size(8.dp))
        }
        val primaryText = when (example) {
            TangemModalStatusExample.KycInProgress,
            TangemModalStatusExample.PaymentCompleted,
            -> "Got it"
            TangemModalStatusExample.ErrorWithTwoActions -> "Continue"
            TangemModalStatusExample.NotAvailable -> "Hide from home screen"
            TangemModalStatusExample.PredictionAmount,
            TangemModalStatusExample.PredictionLoss,
            -> "Go to Predict"
        }
        FooterButton(text = primaryText, variant = TangemButton.Variant.Primary, onClick = onClick)
    }
}

@Composable
private fun FooterButton(text: String, variant: TangemButton.Variant, onClick: () -> Unit) {
    TangemButton(
        modifier = Modifier.fillMaxWidth(),
        variant = variant,
        size = TangemButton.Size.X12,
        text = stringReference(value = text),
        onClick = onClick,
    )
}

/** Stand-in for a token icon or merchant logo in the 72dp asset slot. */
@Composable
private fun PreviewLogo(color: Color, glyph: String) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = TangemTheme.typography3.display.medium,
            color = TangemTheme.colors3.text.staticLight.primary,
        )
    }
}

@Composable
private fun DetailsRow(title: String, value: String, divider: Boolean) {
    TangemRow(
        divider = divider,
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        valueSlot = { TangemRowText(text = value, role = TangemRowTextRole.Value) },
    )
}

@Composable
private fun CompletedRow() {
    TangemRow(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.status.successSubtle),
        titleSlot = {
            Text(
                text = "Completed",
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.status.success,
            )
        },
        endSlot = {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.ic_checkmark_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.status.success,
            )
        },
    )
}

@Composable
private fun RecipientCard(name: String) {
    TangemRow(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.opaque.primary),
        startSlot = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = RecipientPurple, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.take(1),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.staticLight.primary,
                )
            }
        },
        titleSlot = { TangemRowText(text = "Recipient", role = TangemRowTextRole.Subtitle) },
        subtitleSlot = { TangemRowText(text = name, role = TangemRowTextRole.Title) },
    )
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = label,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        content()
    }
}

@Composable
private fun <T> ChipGrid(items: List<T>, label: (T) -> String, isSelected: (T) -> Boolean, onSelect: (T) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(TangemTheme.colors2.surface.level2)
            .border(
                width = 1.dp,
                color = TangemTheme.colors2.border.neutral.secondary,
                shape = shape,
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            Chip(
                label = label(item),
                selected = isSelected(item),
                onClick = { onSelect(item) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(
                if (selected) TangemTheme.colors2.surface.level3 else TangemTheme.colors2.surface.level2,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = if (selected) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TangemTheme.colors2.surface.level2)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = if (checked) "ON" else "OFF",
            style = TangemTheme.typography.caption2,
            color = if (checked) TangemTheme.colors.text.accent else TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TangemTheme.colors2.surface.level2)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = "open",
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.accent,
        )
    }
}