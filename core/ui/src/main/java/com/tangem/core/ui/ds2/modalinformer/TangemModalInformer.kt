@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.modalinformer

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.ds2.modal.tangemModalInformerUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.collections.immutable.ImmutableList

/**
 * Design-system v2 modal informer: start-aligned explanatory text with an optional extra-bottom slot — the body
 * of an informational modal. Place it in a [com.tangem.core.ui.ds2.modal.TangemModal] with the actions in a
 * footer.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10940-4116)
 *
 * Behavior notes:
 * - This is the slot form. The Figma variants have their own overloads: Plain (one paragraph, `text`) and
 *   Rich (Title—Description items, `items`).
 * - The block is as tall as its content and does not scroll — the hosting modal scrolls it.
 *
 * @param modifier Modifier for the root container.
 * @param extraBottom Slot under the text; `null` hides it.
 * @param content Text of the informer, laid out full width with the text inset.
 */
@Composable
fun TangemModalInformer(
    modifier: Modifier = Modifier,
    extraBottom: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
            content = content,
        )
        extraBottom?.invoke()
    }
}

/**
 * Plain variant of [TangemModalInformer]: one paragraph in the primary text color.
 *
 * @param text Paragraph; annotated references keep their spans and links.
 * @param modifier Modifier for the root container.
 * @param extraBottom Slot under the text; `null` hides it.
 */
@Composable
fun TangemModalInformer(
    text: TextReference,
    modifier: Modifier = Modifier,
    extraBottom: (@Composable () -> Unit)? = null,
) {
    TangemModalInformer(modifier = modifier, extraBottom = extraBottom) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text.resolveAnnotatedReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

/**
 * Rich variant of [TangemModalInformer]: Title—Description items, top to bottom. See [TangemModalInformer.Item].
 *
 * @param items Items to show.
 * @param modifier Modifier for the root container.
 * @param extraBottom Slot under the items; `null` hides it.
 */
@Composable
fun TangemModalInformer(
    items: ImmutableList<TangemModalContentUM.Informer.Item>,
    modifier: Modifier = Modifier,
    extraBottom: (@Composable () -> Unit)? = null,
) {
    TangemModalInformer(modifier = modifier, extraBottom = extraBottom) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            items.fastForEach { item ->
                TangemModalInformer.Item(title = item.title, description = item.description)
            }
        }
    }
}

/**
 * [TangemModalInformer] rendered from a [TangemModalContentUM.Informer] — for content kept in UI state or built
 * with the [tangemModalInformerUM] DSL.
 *
 * @param state Informer content. See [TangemModalContentUM.Informer].
 * @param modifier Modifier for the root container.
 */
@Composable
fun TangemModalInformer(state: TangemModalContentUM.Informer, modifier: Modifier = Modifier) {
    when (state) {
        is TangemModalContentUM.Informer.Plain -> TangemModalInformer(text = state.text, modifier = modifier)
        is TangemModalContentUM.Informer.Rich -> TangemModalInformer(items = state.items, modifier = modifier)
    }
}

object TangemModalInformer {

    /** Public API surface of [TangemModalInformer]. */
    val version = DsVersion(major = 1, minor = 0, patch = 0)

    /**
     * One item of the Rich [TangemModalInformer]: a title over its description, both start-aligned.
     *
     * @param title Item title, in the primary text color.
     * @param description Text under the title, in the secondary text color; annotated references keep their
     *   spans and links.
     * @param modifier Modifier for the item column.
     */
    @Composable
    fun Item(title: TextReference, description: TextReference, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = description.resolveAnnotatedReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 386)
@Preview(showBackground = true, widthDp = 386, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemModalInformerPlainPreview() {
    TangemThemePreviewRedesign {
        TangemModalInformer(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
            text = stringReference(
                value = "The network fee is paid to validators and does not go to Tangem. " +
                    "It depends on how busy the network is.",
            ),
            extraBottom = { TangemBadge(text = stringReference(value = "Extra bottom")) },
        )
    }
}

@Preview(showBackground = true, widthDp = 386)
@Preview(showBackground = true, widthDp = 386, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemModalInformerRichPreview() {
    TangemThemePreviewRedesign {
        TangemModalInformer(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
            state = tangemModalInformerUM {
                item(
                    title = stringReference(value = "What is staking?"),
                    description = stringReference(value = "Locking tokens to help secure the network."),
                )
                item(
                    title = stringReference(value = "Rewards"),
                    description = stringReference(value = "Paid out every epoch to your address."),
                )
            },
        )
    }
}
// endregion