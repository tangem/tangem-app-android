@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.modalmenu

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.modal.TangemModalActionScope
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.ds2.modal.tangemModalMenuUM
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_20
import com.tangem.core.ui.res.generated.icons.ic_info_28
import com.tangem.core.ui.res.generated.icons.ic_warning_28

/**
 * Design-system v2 modal menu: a list of navigation rows — the body of a modal that offers a choice. Place it in a
 * [com.tangem.core.ui.ds2.modal.TangemModal]; fill it with [TangemModalMenu.Item]s.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10943-16131)
 *
 * Behavior notes:
 * - The rows run edge to edge and inset their own content; the menu adds only bottom spacing.
 * - The block is as tall as its content and does not scroll — the hosting modal scrolls it.
 *
 * @param modifier Modifier for the root container.
 * @param content Menu rows, typically [TangemModalMenu.Item]s.
 */
@Composable
fun TangemModalMenu(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/**
 * [TangemModalMenu] rendered from a [TangemModalContentUM.Menu] — for content kept in UI state or built with the
 * [tangemModalMenuUM] DSL.
 *
 * @param state Menu content. See [TangemModalContentUM.Menu].
 * @param actionScope Receiver of the rows' click actions; lets a row close the modal.
 * @param modifier Modifier for the root container.
 */
@Composable
fun TangemModalMenu(
    state: TangemModalContentUM.Menu,
    actionScope: TangemModalActionScope,
    modifier: Modifier = Modifier,
) {
    TangemModalMenu(modifier = modifier) {
        state.items.fastForEach { item ->
            TangemModalMenu.Item(
                title = item.title,
                subtitle = item.subtitle,
                avatar = item.avatar,
                onClick = { item.onClick(actionScope) },
            )
        }
    }
}

object TangemModalMenu {

    /** Public API surface of [TangemModalMenu]. */
    val version = DsVersion(major = 1, minor = 0, patch = 0)

    /**
     * Row of [TangemModalMenu]: an optional 40dp avatar, a title with an optional subtitle, and a trailing
     * chevron. The whole row is clickable; the chevron is decorative.
     *
     * @param title Row title.
     * @param onClick Click handler of the row.
     * @param modifier Modifier for the row.
     * @param subtitle Line under the title; `null` hides it.
     * @param avatar Avatar at the start of the row; `null` hides it.
     */
    @Composable
    fun Item(
        title: TextReference,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        subtitle: TextReference? = null,
        avatar: TangemAvatar.UiState? = null,
    ) {
        TangemRow(
            modifier = modifier,
            verticalAlignment = TangemRowVerticalAlignment.Center,
            startSlot = avatar?.let { state -> { TangemAvatar(state = state, size = TangemAvatar.Size.X40) } },
            titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
            subtitleSlot = subtitle?.let { text -> { TangemRowText(text = text, role = TangemRowTextRole.Subtitle) } },
            endSlot = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.ic_chevron_right_20,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.tertiary,
                )
            },
            onClick = onClick,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 386)
@Preview(showBackground = true, widthDp = 386, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemModalMenuPreview() {
    TangemThemePreviewRedesign {
        TangemModalMenu(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
            actionScope = {},
            state = tangemModalMenuUM {
                item {
                    title = stringReference(value = "Learn more")
                    subtitle = stringReference(value = "How it works")
                    avatar = TangemAvatar.UiState.Icon(Icons.ic_info_28, TangemAvatar.Appearance.Info)
                }
                item {
                    title = stringReference(value = "Report a problem")
                    subtitle = stringReference(value = "We reply within a day")
                    avatar = TangemAvatar.UiState.Icon(Icons.ic_warning_28, TangemAvatar.Appearance.Warning)
                }
                item { title = stringReference(value = "Without avatar") }
            },
        )
    }
}
// endregion