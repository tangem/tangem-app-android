@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.transactionrow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.test.TransactionHistoryItemTestTags
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Design-system v2 (DS3) **Transaction Row** — a transaction history list item: a status-tinted
 * transaction avatar, a title with a subtitle underneath, the amount with an optional badge and caption at
 * the end, and an optional warning status message below. DS3 replacement for the legacy
 * `components/transactions/TransactionItem`.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=12487-2300)
 *
 * Behavior notes:
 * - [status] colours the avatar, the title and the amount (see [TangemTransactionRow.Status]).
 * - A [TangemTransactionRow.Status.Failed] amount is struck through, unless it is masked by
 *   [isBalanceHidden]. The row does not edit the amount text: drop its sign before passing it if the
 *   failed transaction should read unsigned, as in Figma.
 * - Pressing an interactive row draws the `interaction.press.default` overlay.
 *
 * Accessibility: colour alone does not tell the states apart, so the row announces [status] as its state
 * description — "In progress", "Pending" or "Transaction failed"; a finished row announces none.
 *
 * @param icon Transaction kind glyph (send, receive, swap…), drawn in a 40dp circle tinted by [status].
 * @param title Transaction title (e.g. `"Send"`). Single line, ellipsized.
 * @param status Lifecycle of the transaction. See [TangemTransactionRow.Status].
 * @param modifier Modifier applied to the row container.
 * @param subtitle Optional slot under the title — usually a [TangemTransactionRow.EntitySubtitle].
 * `null` hides the line.
 * @param amount Amount at the end (e.g. `"-$12,000.00"`). `null` hides it.
 * @param caption Caption under [amount] (e.g. a ticker or a time). `null` hides it.
 * @param badge Neutral badge label before [caption]. `null` hides it.
 * @param statusMessage Warning message under the row content, its icon aligned with the avatar. `null`
 * hides it.
 * @param isBalanceHidden When `true`, [amount] is masked with stars.
 * @param onClick Row click handler. `null` makes the row non-interactive.
 */
@Suppress("LongParameterList")
@Composable
fun TangemTransactionRow(
    icon: ImageVector,
    title: TextReference,
    status: TangemTransactionRow.Status,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    amount: TextReference? = null,
    caption: TextReference? = null,
    badge: TextReference? = null,
    statusMessage: TextReference? = null,
    isBalanceHidden: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val stateDescription = status.stateDescriptionRes()?.let { stringResourceSafe(it) }
    TangemRow(
        modifier = modifier.semantics { stateDescription?.let { this.stateDescription = it } },
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { TransactionRowHeadAvatar(icon = icon, status = status) },
        titleSlot = {
            TransactionRowTitle(title = title, status = status, modifier = Modifier.padding(end = 12.dp))
        },
        subtitleSlot = subtitle?.let { content ->
            { Box(modifier = Modifier.padding(end = 12.dp)) { content() } }
        },
        valueSlot = amount?.let { value ->
            {
                TransactionRowAmount(
                    amount = value,
                    status = status,
                    isBalanceHidden = isBalanceHidden,
                    modifier = Modifier.testTag(TransactionHistoryItemTestTags.AMOUNT),
                )
            }
        },
        subvalueSlot = if (badge != null || caption != null) {
            { TransactionRowEndBottom(badge = badge, caption = caption) }
        } else {
            null
        },
        extraBottomSlot = statusMessage?.let { message ->
            {
                TransactionRowStatusMessage(
                    text = message,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                )
            }
        },
        onClick = onClick,
    )
}

/** Public API surface of [TangemTransactionRow]. */
object TangemTransactionRow {

    val version = DsVersion(major = 1, minor = 0, patch = 0)

    /** Lifecycle of the transaction a row stands for, driving the avatar, title and amount colours. */
    enum class Status {
        /** Broadcast, awaiting confirmation — info-tinted avatar and title, secondary amount. */
        Progress,

        /** Confirmed — neutral avatar, primary title and amount. */
        Finished,

        /** Failed — error-tinted avatar and title, struck-through amount. */
        Failed,

        /** Waiting for an action before it is sent (e.g. a co-signature) — warning-tinted avatar and title. */
        Pending,
    }

    /**
     * Model of the subtitle rendered by the [State.Content] overload — one subtype per Figma variant of the
     * Transaction Detail Module.
     */
    @Immutable
    sealed class Subtitle {

        /**
         * `"from: 🟣 Entity • Description"`. Mirrors the [EntitySubtitle] parameters.
         *
         * @param template Text with one `%image%` marker where [avatar] is drawn.
         * @param avatar 16dp avatar drawn at the marker.
         * @param address Raw address [template] was formatted with; it is shortened from its middle.
         * @param description Text after a separator dot. `null` hides both.
         */
        data class Entity(
            val template: TextReference,
            val avatar: TangemAvatar.UiState,
            val address: String? = null,
            val description: TextReference? = null,
        ) : Subtitle()

        /**
         * `"from: 🟣 Entity to: 🔷 Entity"`. Mirrors the [EntityStackSubtitle] parameters.
         *
         * @param template Text with one `%image%` marker per entry of [avatars].
         * @param avatars 16dp avatars drawn at the markers, in order.
         */
        data class EntityStack(
            val template: TextReference,
            val avatars: ImmutableList<TangemAvatar.UiState>,
        ) : Subtitle()

        /**
         * `"Label • Description"`. Mirrors the [TextSubtitle] parameters.
         *
         * @param label Leading text.
         * @param description Text after a separator dot. `null` hides both.
         */
        data class Text(
            val label: TextReference,
            val description: TextReference? = null,
        ) : Subtitle()
    }

    /**
     * State model of [TangemTransactionRow] — one subtype per Figma variant. Render it with the
     * `TangemTransactionRow(state = …)` overload.
     */
    @Immutable
    sealed class State {

        /** Unique id, e.g. for `LazyColumn` item keys. */
        abstract val id: String

        /**
         * Default variant.
         *
         * @param id Unique id.
         * @param icon Transaction kind glyph.
         * @param title Transaction title.
         * @param status Transaction lifecycle. See [Status].
         * @param subtitle Line under the title. `null` hides it. See [Subtitle].
         * @param amount Amount at the end. `null` hides it.
         * @param caption Caption under the amount. `null` hides it.
         * @param badge Neutral badge label before the caption. `null` hides it.
         * @param statusMessage Warning message under the row content. `null` hides it.
         * @param onClick Row click handler. `null` makes the row non-interactive.
         */
        data class Content(
            override val id: String,
            val icon: ImageVector,
            val title: TextReference,
            val status: Status,
            val subtitle: Subtitle? = null,
            val amount: TextReference? = null,
            val caption: TextReference? = null,
            val badge: TextReference? = null,
            val statusMessage: TextReference? = null,
            val onClick: (() -> Unit)? = null,
        ) : State()

        /**
         * Loading variant — avatar and text-line shimmers.
         *
         * @param id Unique id.
         */
        data class Shimmer(
            override val id: String,
        ) : State()
    }
}

/**
 * Design-system v2 (DS3) **Transaction Row** — state-driven overload: renders the variant described by
 * [TangemTransactionRow.State].
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=12487-2300)
 *
 * @param state Row state model. See [TangemTransactionRow.State].
 * @param modifier Modifier applied to the row container.
 * @param isBalanceHidden When `true`, the amount is masked with stars. Kept outside [state] because it
 * is an app-wide setting.
 */
@Composable
fun TangemTransactionRow(
    state: TangemTransactionRow.State,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean = false,
) {
    when (state) {
        is TangemTransactionRow.State.Content -> TangemTransactionRow(
            icon = state.icon,
            title = state.title,
            status = state.status,
            modifier = modifier,
            subtitle = state.subtitle?.let { subtitle -> { TransactionRowSubtitle(subtitle = subtitle) } },
            amount = state.amount,
            caption = state.caption,
            badge = state.badge,
            statusMessage = state.statusMessage,
            isBalanceHidden = isBalanceHidden,
            onClick = state.onClick,
        )
        is TangemTransactionRow.State.Shimmer -> TangemTransactionRow.Shimmer(modifier = modifier)
    }
}

@Composable
private fun TransactionRowSubtitle(subtitle: TangemTransactionRow.Subtitle) {
    when (subtitle) {
        is TangemTransactionRow.Subtitle.Entity -> TangemTransactionRow.EntitySubtitle(
            template = subtitle.template,
            avatar = subtitle.avatar,
            address = subtitle.address,
            description = subtitle.description,
        )
        is TangemTransactionRow.Subtitle.EntityStack -> TangemTransactionRow.EntityStackSubtitle(
            template = subtitle.template,
            avatars = subtitle.avatars,
        )
        is TangemTransactionRow.Subtitle.Text -> TangemTransactionRow.TextSubtitle(
            label = subtitle.label,
            description = subtitle.description,
        )
    }
}

/**
 * Design-system v2 (DS3) **Transaction Row / Detail Module, Entity** — the line under the title naming the
 * counterparty, optionally followed by a description: `"from: 🟣 Entity • Description"`, all in secondary
 * caption text.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=12632-25806)
 *
 * The line is built from a localized [template] rather than separate prefix/avatar/name parts, so the word
 * order and the avatar position follow the language: `"from: %image% Main"` in English, `"%image% Main から"`
 * in Japanese.
 *
 * @param template Text with one `%image%` marker where [avatar] is drawn — a string resource formatted with
 * the entity name or the raw address (e.g. `transaction_history_from_inline_address`).
 * @param avatar Avatar drawn at [TangemAvatar.Size.X16] at the marker.
 * @param modifier Modifier applied to the subtitle row.
 * @param address Raw address [template] was formatted with. When set, the address is shortened from its
 * middle so both ends stay readable, and the text around it keeps its place. `null` cuts the line at its end.
 * @param description Text after a separator dot. `null` hides both.
 */
@Composable
fun TangemTransactionRow.EntitySubtitle(
    template: TextReference,
    avatar: TangemAvatar.UiState,
    modifier: Modifier = Modifier,
    address: String? = null,
    description: TextReference? = null,
) {
    TransactionRowDetailModule(description = description, modifier = modifier) {
        val resolvedTemplate = template.resolveReference()
        if (address != null && resolvedTemplate.contains(address)) {
            TransactionRowAddressEntity(
                template = resolvedTemplate,
                address = address,
                avatar = avatar,
                modifier = Modifier.weight(weight = 1f, fill = false),
            )
        } else {
            TransactionRowEntity(
                template = resolvedTemplate,
                avatars = persistentListOf(avatar),
                modifier = Modifier.weight(weight = 1f, fill = false),
            )
        }
    }
}

/**
 * Design-system v2 (DS3) **Transaction Row / Detail Module, Entity Stack** — two counterparties on one line:
 * `"from: 🟣 Entity to: 🔷 Entity"`, all in secondary caption text.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=12632-25806)
 *
 * Both entities come from one localized [template], so the language decides the order of the parts.
 *
 * @param template Text with one `%image%` marker per entry of [avatars].
 * @param avatars Avatars drawn at [TangemAvatar.Size.X16] at the markers, in order. A marker without an
 * avatar draws nothing.
 * @param modifier Modifier applied to the subtitle.
 */
@Composable
fun TangemTransactionRow.EntityStackSubtitle(
    template: TextReference,
    avatars: ImmutableList<TangemAvatar.UiState>,
    modifier: Modifier = Modifier,
) {
    TransactionRowEntity(template = template.resolveReference(), avatars = avatars, modifier = modifier)
}

/**
 * Design-system v2 (DS3) **Transaction Row / Detail Module, Text** — a plain line under the title, optionally
 * followed by a description: `"Label • Description"`, all in secondary caption text.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=12632-25806)
 *
 * @param label Leading text.
 * @param modifier Modifier applied to the subtitle row.
 * @param description Text after a separator dot. `null` hides both.
 */
@Composable
fun TangemTransactionRow.TextSubtitle(
    label: TextReference,
    modifier: Modifier = Modifier,
    description: TextReference? = null,
) {
    TransactionRowDetailModule(description = description, modifier = modifier) {
        TransactionRowSubtitleText(
            text = label.resolveReference(),
            modifier = Modifier.weight(weight = 1f, fill = false),
        )
    }
}

/**
 * Design-system v2 (DS3) **Transaction Row / Shimmer** — loading placeholder: circular avatar shimmer
 * plus text-line bars on both sides.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=12487-2300)
 *
 * @param modifier Modifier applied to the row container.
 */
@Composable
fun TangemTransactionRow.Shimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemAvatar(state = TangemAvatar.UiState.Shimmer(), size = TangemAvatar.Size.X40)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShimmerColumn(textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
            ShimmerColumn(textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ShimmerColumn(textAlign: TextAlign, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TangemShimmer(style = TangemTheme.typography3.body.medium, textAlign = textAlign)
        TangemShimmer(style = TangemTheme.typography3.caption.medium, textAlign = textAlign)
    }
}

// region Previews

private const val PREVIEW_ADDRESS = "0x1D1479C185d32EB90533a08b36B3CFa5F84A0E6B"

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemTransactionRowPreview() {
    PreviewContainer {
        TangemTransactionRow.Status.entries.forEach { status ->
            TangemTransactionRow(
                state = TangemTransactionRow.State.Content(
                    id = status.name,
                    icon = Icons.ic_arrow_up_20,
                    title = stringReference("Send"),
                    status = status,
                    subtitle = TangemTransactionRow.Subtitle.Entity(
                        template = stringReference("from: %image% Entity"),
                        avatar = TangemAvatar.UiState.Address(address = PREVIEW_ADDRESS),
                    ),
                    amount = stringReference(
                        if (status == TangemTransactionRow.Status.Failed) "$12,000.00" else "-$12,000.00",
                    ),
                    caption = stringReference("Caption"),
                    onClick = {},
                ),
            )
        }
    }
}

@Preview(name = "Extras Light", showBackground = true, widthDp = 360)
@Preview(name = "Extras Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemTransactionRowExtrasPreview() {
    PreviewContainer {
        TangemTransactionRow(
            icon = Icons.ic_arrow_up_20,
            title = stringReference("Send"),
            status = TangemTransactionRow.Status.Progress,
            subtitle = {
                TangemTransactionRow.EntitySubtitle(
                    template = stringReference("to: %image% $PREVIEW_ADDRESS"),
                    avatar = TangemAvatar.UiState.Address(address = PREVIEW_ADDRESS),
                    address = PREVIEW_ADDRESS,
                    description = stringReference("Description"),
                )
            },
            amount = stringReference("-$12,000.00"),
            caption = stringReference("Caption"),
            badge = stringReference("Label"),
            statusMessage = stringReference("KYC verification required by provider"),
            onClick = {},
        )
        TangemTransactionRow(
            icon = Icons.ic_arrow_up_20,
            title = stringReference("Swap"),
            status = TangemTransactionRow.Status.Finished,
            subtitle = {
                TangemTransactionRow.EntityStackSubtitle(
                    template = stringReference("from: %image% Main to: %image% Savings"),
                    avatars = persistentListOf(
                        TangemAvatar.UiState.User(initial = 'M', color = TangemAvatar.FillColor.AzureBlue),
                        TangemAvatar.UiState.Address(address = PREVIEW_ADDRESS),
                    ),
                )
            },
            amount = stringReference("-$12,000.00"),
            caption = stringReference("Caption"),
        )
        TangemTransactionRow(
            icon = Icons.ic_arrow_up_20,
            title = stringReference("Send"),
            status = TangemTransactionRow.Status.Finished,
            subtitle = {
                TangemTransactionRow.TextSubtitle(
                    label = stringReference("Label"),
                    description = stringReference("Description"),
                )
            },
            amount = stringReference("-$12,000.00"),
            caption = stringReference("Caption"),
            isBalanceHidden = true,
        )
        TangemTransactionRow.Shimmer()
    }
}

@Composable
private fun PreviewContainer(content: @Composable () -> Unit) {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            content()
        }
    }
}

// endregion