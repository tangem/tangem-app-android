@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.modal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.modalinformer.TangemModalInformer
import com.tangem.core.ui.ds2.modalmenu.TangemModalMenu
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.modal.MessageModal
import com.tangem.core.ui.message.modal.MessageModalUM
import com.tangem.core.ui.message.modal.closeBs
import com.tangem.core.ui.message.modal.messageModalUM
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_20
import com.tangem.core.ui.res.generated.icons.ic_info_28
import com.tangem.core.ui.res.generated.icons.ic_warning_28
import com.tangem.feature.tester.presentation.storybook.entity.MessageModalSample
import com.tangem.feature.tester.presentation.storybook.entity.TangemModalInformerVariant
import com.tangem.feature.tester.presentation.storybook.entity.TangemModalStatusExample
import com.tangem.feature.tester.presentation.storybook.entity.TangemModalTemplate
import com.tangem.feature.tester.presentation.storybook.entity.TangemModalTemplatesState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

private val TetherGreen = Color(0xFF26A17B)
private val MerchantBlack = Color(0xFF1C1C1E)
private val RecipientPurple = Color(0xFF7B61FF)

private const val STATUS_TITLE = "KYC in progress"
private const val STATUS_AMOUNT_TITLE = "-$166.84"
private const val STATUS_DESCRIPTION = "Use the buttons below to view your current KYC status or cancel it"
private const val INFORMER_TEXT =
    "The network fee goes to validators, not to Tangem. It grows when the network is busy."
private val InformerItems = persistentListOf(
    TangemModalContentUM.Informer.Item(
        title = stringReference(value = "What is staking?"),
        description = stringReference(value = "Locking tokens to help secure the network for rewards."),
    ),
    TangemModalContentUM.Informer.Item(
        title = stringReference(value = "When are rewards paid?"),
        description = stringReference(value = "Every epoch, straight to your address."),
    ),
)

/**
 * Content-template part of the TangemModal story: the selected template previewed on a card, its controls, a
 * button that opens it in a [MessageModal], and the Figma examples. The modals it opens are rendered by
 * [TangemModalTemplateModals], which the page places inside its modal host.
 */
@Composable
internal fun TangemModalTemplatesSection(
    state: TangemModalTemplatesState,
    onChange: (TangemModalTemplatesState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(text = "Content templates")
        ChipSelector(
            items = TangemModalTemplate.entries,
            selected = state.template,
            onSelect = { template -> onChange(state.copy(template = template)) },
        )
        TemplatePreview(state = state)
        when (state.template) {
            TangemModalTemplate.Status -> StatusControls(state = state, onChange = onChange)
            TangemModalTemplate.Informer -> InformerControls(state = state, onChange = onChange)
            TangemModalTemplate.Menu -> MenuControls(state = state, onChange = onChange)
        }
        ShowModalButton(
            label = "Show ${state.template.name} in MessageModal",
            onClick = { onChange(state.copy(isTemplateMessageShown = true)) },
        )
        SectionLabel(text = "Examples")
        TangemModalStatusExample.entries.forEach { example ->
            ShowModalButton(label = example.label, onClick = { onChange(state.copy(openedExample = example)) })
        }
        MessageModalSample.entries.forEach { sample ->
            ShowModalButton(label = sample.label, onClick = { onChange(state.copy(openedMessage = sample)) })
        }
    }
}

/** The modals [TangemModalTemplatesSection] opens; place it inside the page's modal host. */
@Composable
internal fun TangemModalTemplateModals(
    state: TangemModalTemplatesState,
    onChange: (TangemModalTemplatesState) -> Unit,
) {
    ExampleModal(example = state.openedExample, onDismiss = { onChange(state.copy(openedExample = null)) })
    state.openedMessage?.let { sample ->
        MessageModal(
            state = remember(sample) { sampleMessage(sample) },
            onDismissRequest = { onChange(state.copy(openedMessage = null)) },
        )
    }
    if (state.isTemplateMessageShown) {
        MessageModal(
            state = remember { templateMessage(state) },
            onDismissRequest = { onChange(state.copy(isTemplateMessageShown = false)) },
        )
    }
}

@Composable
private fun TemplatePreview(state: TangemModalTemplatesState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.primary),
    ) {
        when (state.template) {
            TangemModalTemplate.Status -> StatusPreview(state = state)
            TangemModalTemplate.Informer -> InformerPreview(state = state)
            TangemModalTemplate.Menu -> MenuPreview(state = state)
        }
    }
}

@Composable
private fun StatusPreview(state: TangemModalTemplatesState) {
    val isAmount = state.statusTitleStyle == TangemModalStatus.TitleStyle.Amount
    TangemModalStatus(
        variant = state.statusVariant,
        title = stringReference(value = if (isAmount) STATUS_AMOUNT_TITLE else STATUS_TITLE),
        description = if (state.hasStatusDescription) stringReference(value = STATUS_DESCRIPTION) else null,
        titleStyle = state.statusTitleStyle,
        asset = if (state.hasStatusCustomAsset) {
            { PreviewLogo(color = TetherGreen, glyph = "₮") }
        } else {
            null
        },
        extraBottom = if (state.hasStatusExtraBottom) {
            { TangemBadge(text = stringReference(value = "Good luck next time"), status = TangemBadge.Status.Error) }
        } else {
            null
        },
        details = if (state.hasStatusDetails) {
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

@Composable
private fun InformerPreview(state: TangemModalTemplatesState) {
    val extraBottom: (@Composable () -> Unit)? = if (state.hasInformerExtraBottom) {
        { TangemBadge(text = stringReference(value = "Extra bottom")) }
    } else {
        null
    }
    when (state.informerVariant) {
        TangemModalInformerVariant.Plain -> TangemModalInformer(
            text = stringReference(value = INFORMER_TEXT),
            extraBottom = extraBottom,
        )
        TangemModalInformerVariant.Rich -> TangemModalInformer(items = InformerItems, extraBottom = extraBottom)
    }
}

@Composable
private fun MenuPreview(state: TangemModalTemplatesState) {
    TangemModalMenu {
        menuSamples(state).forEach { item ->
            TangemModalMenu.Item(
                title = item.title,
                subtitle = item.subtitle,
                avatar = item.avatar,
                onClick = {},
            )
        }
    }
}

@Composable
private fun StatusControls(state: TangemModalTemplatesState, onChange: (TangemModalTemplatesState) -> Unit) {
    ChipSelector(
        items = TangemModalStatus.Variant.entries,
        selected = state.statusVariant,
        onSelect = { variant -> onChange(state.copy(statusVariant = variant)) },
    )
    ChipSelector(
        items = TangemModalStatus.TitleStyle.entries,
        selected = state.statusTitleStyle,
        onSelect = { style -> onChange(state.copy(statusTitleStyle = style)) },
    )
    ToggleRow(
        label = "description",
        checked = state.hasStatusDescription,
        onToggle = { onChange(state.copy(hasStatusDescription = !state.hasStatusDescription)) },
    )
    ToggleRow(
        label = "custom asset",
        checked = state.hasStatusCustomAsset,
        onToggle = { onChange(state.copy(hasStatusCustomAsset = !state.hasStatusCustomAsset)) },
    )
    ToggleRow(
        label = "extra bottom (badge)",
        checked = state.hasStatusExtraBottom,
        onToggle = { onChange(state.copy(hasStatusExtraBottom = !state.hasStatusExtraBottom)) },
    )
    ToggleRow(
        label = "details",
        checked = state.hasStatusDetails,
        onToggle = { onChange(state.copy(hasStatusDetails = !state.hasStatusDetails)) },
    )
}

@Composable
private fun InformerControls(state: TangemModalTemplatesState, onChange: (TangemModalTemplatesState) -> Unit) {
    ChipSelector(
        items = TangemModalInformerVariant.entries,
        selected = state.informerVariant,
        onSelect = { variant -> onChange(state.copy(informerVariant = variant)) },
    )
    ToggleRow(
        label = "extra bottom (badge)",
        checked = state.hasInformerExtraBottom,
        onToggle = { onChange(state.copy(hasInformerExtraBottom = !state.hasInformerExtraBottom)) },
    )
}

@Composable
private fun MenuControls(state: TangemModalTemplatesState, onChange: (TangemModalTemplatesState) -> Unit) {
    ToggleRow(
        label = "avatars",
        checked = state.hasMenuAvatars,
        onToggle = { onChange(state.copy(hasMenuAvatars = !state.hasMenuAvatars)) },
    )
    ToggleRow(
        label = "subtitles",
        checked = state.hasMenuSubtitles,
        onToggle = { onChange(state.copy(hasMenuSubtitles = !state.hasMenuSubtitles)) },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        modifier = Modifier.padding(top = 16.dp),
        text = text,
        style = TangemTheme.typography.subtitle1,
        color = TangemTheme.colors.text.primary1,
    )
}

private fun menuSamples(state: TangemModalTemplatesState): List<TangemModalContentUM.Menu.Item> = listOf(
    TangemModalContentUM.Menu.Item(
        title = stringReference(value = "Create a new wallet"),
        subtitle = stringReference(value = "Generate a fresh seed phrase").takeIf { state.hasMenuSubtitles },
        avatar = TangemAvatar.UiState.Icon(Icons.ic_checkmark_20, TangemAvatar.Appearance.Success)
            .takeIf { state.hasMenuAvatars },
        onClick = { dismiss() },
    ),
    TangemModalContentUM.Menu.Item(
        title = stringReference(value = "Import a wallet"),
        subtitle = stringReference(value = "Use an existing seed phrase").takeIf { state.hasMenuSubtitles },
        avatar = TangemAvatar.UiState.Icon(Icons.ic_info_28, TangemAvatar.Appearance.Info)
            .takeIf { state.hasMenuAvatars },
        onClick = { dismiss() },
    ),
)

/** The previewed template as a [MessageModal]; the DSL has no free-form asset, so a custom asset becomes a glyph. */
private fun templateMessage(state: TangemModalTemplatesState): MessageModalUM = messageModalUM {
    when (state.template) {
        TangemModalTemplate.Status -> status {
            val isAmount = state.statusTitleStyle == TangemModalStatus.TitleStyle.Amount
            variant = state.statusVariant
            title = stringReference(value = if (isAmount) STATUS_AMOUNT_TITLE else STATUS_TITLE)
            titleStyle = state.statusTitleStyle
            if (state.hasStatusDescription) description = stringReference(value = STATUS_DESCRIPTION)
            if (state.hasStatusCustomAsset) glyph(Icons.ic_info_28)
            if (state.hasStatusExtraBottom) {
                badge(text = stringReference(value = "Good luck next time"), status = TangemBadge.Status.Error)
            }
            if (state.hasStatusDetails) {
                detail(title = stringReference(value = "Qty of Tokens"), value = stringReference(value = "248.50"))
                detail(title = stringReference(value = "Price"), value = stringReference(value = "$0.81"))
                detail(title = stringReference(value = "Fee"), value = stringReference(value = "$5.00"))
            }
        }
        TangemModalTemplate.Informer -> {
            title = stringReference(value = "Informer")
            when (state.informerVariant) {
                TangemModalInformerVariant.Plain -> informer { text = stringReference(value = INFORMER_TEXT) }
                TangemModalInformerVariant.Rich -> content(TangemModalContentUM.Informer.Rich(items = InformerItems))
            }
        }
        TangemModalTemplate.Menu -> {
            title = stringReference(value = "Menu")
            content(TangemModalContentUM.Menu(items = menuSamples(state).toPersistentList()))
        }
    }
    primaryButton { text = stringReference(value = "Got it") }
}

private fun sampleMessage(sample: MessageModalSample): MessageModalUM = when (sample) {
    MessageModalSample.Status -> messageModalUM {
        status {
            variant = TangemModalStatus.Variant.Warning
            title = stringReference(value = "Backup your wallet")
            description = stringReference(
                value = "You haven't backed up this wallet yet. Without a backup you may lose access to your funds.",
            )
            glyph(Icons.ic_warning_28)
            badge(text = stringReference(value = "Recommended"), status = TangemBadge.Status.Warning)
            detail(title = stringReference(value = "Wallet"), value = stringReference(value = "Wallet 1"))
            detail(title = stringReference(value = "Cards"), value = stringReference(value = "1 of 3"))
        }
        primaryButton { text = stringReference(value = "Back up now") }
        secondaryButton { text = stringReference(value = "Later") }
    }
    MessageModalSample.Informer -> messageModalUM {
        title = stringReference(value = "Staking")
        informer {
            item(
                title = stringReference(value = "What is staking?"),
                description = stringReference(value = "Locking tokens to help secure the network for rewards."),
            )
            item(
                title = stringReference(value = "When are rewards paid?"),
                description = stringReference(value = "Every epoch, straight to your address."),
            )
        }
        primaryButton { text = stringReference(value = "Got it") }
    }
    MessageModalSample.Menu -> messageModalUM {
        title = stringReference(value = "Add wallet")
        menu {
            item {
                title = stringReference(value = "Create a new wallet")
                subtitle = stringReference(value = "Generate a fresh seed phrase")
                avatar = TangemAvatar.UiState.Icon(Icons.ic_checkmark_20, TangemAvatar.Appearance.Success)
            }
            item {
                title = stringReference(value = "Import a wallet")
                subtitle = stringReference(value = "Use an existing seed phrase")
                avatar = TangemAvatar.UiState.Icon(Icons.ic_warning_28, TangemAvatar.Appearance.Warning)
            }
        }
    }
    MessageModalSample.Legacy -> legacyMessage()
}

/** The `messageBottomSheetUM` grammar, unchanged, under the `messageModalUM` entry point. */
@Suppress("DEPRECATION")
private fun legacyMessage(): MessageModalUM = messageModalUM {
    infoBlock {
        vector(Icons.ic_warning_28) { type = MessageBottomSheetUM.Vector.Type.Attention }
        title = stringReference(value = "Backup your wallet")
        body = stringReference(value = "You haven't backed up this wallet yet.")
        chip(text = stringReference(value = "Recommended")) { type = MessageBottomSheetUM.Chip.Type.Warning }
    }
    primaryButton {
        text = stringReference(value = "Back up now")
        onClick { closeBs() }
    }
    secondaryButton {
        text = stringReference(value = "Later")
        onClick = { closeBs() }
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
            .background(TangemTheme.colors3.bg.transparent.primary),
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