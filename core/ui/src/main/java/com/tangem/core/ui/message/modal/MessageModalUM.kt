package com.tangem.core.ui.message.modal

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModalActionScope
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.ds2.modal.TangemModalDsl
import com.tangem.core.ui.ds2.modal.TangemModalInformerBuilder
import com.tangem.core.ui.ds2.modal.TangemModalMenuBuilder
import com.tangem.core.ui.ds2.modal.TangemModalStatusBuilder
import com.tangem.core.ui.ds2.modal.tangemModalInformerUM
import com.tangem.core.ui.ds2.modal.tangemModalMenuUM
import com.tangem.core.ui.ds2.modal.tangemModalStatusUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * UI model of [MessageModal]: one content template with footer buttons. Build it with [messageModalUM].
 *
 * @param content Body of the modal: a status, an informer or a menu. See [TangemModalContentUM].
 * @param title Title of the modal's top navigation, for content without a heading of its own (an informer, a
 *   menu); `null` leaves only the close button.
 * @param buttons Footer actions, top to bottom; empty hides the footer.
 * @param onDismiss Called whenever the modal is dismissed — by the close button, back, the scrim, or an action
 *   calling [TangemModalActionScope.dismiss].
 */
@Immutable
data class MessageModalUM(
    val content: TangemModalContentUM,
    val title: TextReference? = null,
    val buttons: ImmutableList<Button> = persistentListOf(),
    val onDismiss: () -> Unit = {},
) : TangemBottomSheetConfigContent {

    /**
     * Footer action of [MessageModal].
     *
     * @param text Button label; `null` makes an icon-only button.
     * @param variant Button style; `primaryButton` and `secondaryButton` set it.
     * @param iconStart Optional leading icon.
     * @param iconEnd Optional trailing icon.
     * @param onClick Click action; call [TangemModalActionScope.dismiss] from it to close the modal.
     */
    @Immutable
    data class Button(
        val text: TextReference?,
        val variant: TangemButton.Variant,
        val iconStart: TangemIconUM? = null,
        val iconEnd: TangemIconUM? = null,
        val onClick: TangemModalActionScope.() -> Unit,
    )
}

/**
 * Builds a [MessageModalUM]: exactly one content block — [status][MessageModalBuilder.status],
 * [informer][MessageModalBuilder.informer] or [menu][MessageModalBuilder.menu] — plus footer buttons.
 *
 * ```
 * messageModalUM {
 *     status {
 *         variant = TangemModalStatus.Variant.Warning
 *         title = resourceReference(R.string.title)
 *         description = resourceReference(R.string.description)
 *     }
 *     primaryButton {
 *         text = resourceReference(R.string.back_up)
 *         onClick { openBackup(); dismiss() }
 *     }
 *     secondaryButton { text = resourceReference(R.string.later) }
 *     onDismiss { analytics.send(Closed) }
 * }
 * ```
 *
 * The `messageBottomSheetUM` grammar is accepted too (`infoBlock { … }`, `onClick = { closeBs() }`,
 * `icon = R.drawable.…`), so an old message migrates by switching the entry point; see `MessageModalLegacyDsl.kt`.
 */
fun messageModalUM(init: MessageModalBuilder.() -> Unit): MessageModalUM = MessageModalBuilder().apply(init).build()

@TangemModalDsl
class MessageModalBuilder internal constructor() {

    /** Title of the modal's top navigation. */
    var title: TextReference? = null
    var onDismissRequest: () -> Unit = {}

    private var content: TangemModalContentUM? = null
    private val buttons = mutableListOf<MessageModalUM.Button>()

    fun status(init: TangemModalStatusBuilder.() -> Unit) {
        content(tangemModalStatusUM(init))
    }

    fun informer(init: TangemModalInformerBuilder.() -> Unit) {
        content(tangemModalInformerUM(init))
    }

    fun menu(init: TangemModalMenuBuilder.() -> Unit) {
        content(tangemModalMenuUM(init))
    }

    fun content(content: TangemModalContentUM) {
        check(this.content == null) { "A message modal holds one content block" }
        this.content = content
    }

    @Deprecated(message = "Use status { } — infoBlock { } is the messageBottomSheetUM grammar")
    fun infoBlock(init: MessageModalInfoBlockBuilder.() -> Unit) {
        content(MessageModalInfoBlockBuilder().apply(init).build())
    }

    fun primaryButton(init: MessageModalButtonBuilder.() -> Unit) {
        buttons += MessageModalButtonBuilder(variant = TangemButton.Variant.Primary).apply(init).build()
    }

    fun secondaryButton(init: MessageModalButtonBuilder.() -> Unit) {
        buttons += MessageModalButtonBuilder(variant = TangemButton.Variant.Secondary).apply(init).build()
    }

    fun onDismiss(block: () -> Unit) {
        onDismissRequest = block
    }

    internal fun build(): MessageModalUM = MessageModalUM(
        content = checkNotNull(content) { "A message modal needs a status, informer or menu block" },
        title = title,
        buttons = buttons.toPersistentList(),
        onDismiss = onDismissRequest,
    )
}

@TangemModalDsl
class MessageModalButtonBuilder internal constructor(private val variant: TangemButton.Variant) {

    var text: TextReference? = null
    var iconStart: TangemIconUM? = null
    var iconEnd: TangemIconUM? = null

    /** Click action; without one the button just closes the modal. */
    var onClick: (TangemModalActionScope.() -> Unit)? = null

    /**
     * Icon placed by the `messageBottomSheetUM` rule: at the start when set before [text], at the end when set
     * after it.
     */
    @Deprecated(message = "Set iconStart or iconEnd")
    var icon: Int? = null
        set(value) {
            field = value
            val iconUM = value?.let { res -> TangemIconUM.Icon(iconRes = res) }
            if (text == null) {
                iconStart = iconUM
                iconEnd = null
            } else {
                iconStart = null
                iconEnd = iconUM
            }
        }

    fun onClick(block: TangemModalActionScope.() -> Unit) {
        onClick = block
    }

    internal fun build(): MessageModalUM.Button = MessageModalUM.Button(
        text = text,
        variant = variant,
        iconStart = iconStart,
        iconEnd = iconEnd,
        onClick = onClick ?: { dismiss() },
    )
}