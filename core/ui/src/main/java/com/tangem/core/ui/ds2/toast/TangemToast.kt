@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.toast

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.TangemToastMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_eye_cross_20

/**
 * Design-system v2 (DS3) **Toast** — a short, non-blocking notice on a material pill: a leading icon, a label,
 * an optional action button and an optional close button.
 *
 * Version: 1.1.1
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=13978-477)
 *
 * Behavior notes:
 * - The toast hugs its content up to the width it is given, then wraps the label onto up to three lines and
 *   ellipsizes the rest.
 * - This composable draws a toast at rest. To show toasts with the show / auto-dismiss animation and stacking,
 *   send a [TangemToastMessage] through the UI message sender or call [TangemToastStackState.show] — both render
 *   in a [TangemToastStack].
 * - TalkBack announces the label as a polite live region; the close button reads "Close".
 *
 * @param label Message, rendered in subheading/medium typography, up to three lines.
 * @param modifier Modifier applied to the toast.
 * @param appearance Leading icon and its tint. [TangemToast.Appearance.Neutral] uses [icon]; the status
 *   appearances use their fixed status icon and ignore [icon].
 * @param icon Leading 20dp icon of the [TangemToast.Appearance.Neutral] appearance. `null` hides it.
 * @param actionLabel Label of the secondary action button. The button is shown only when both [actionLabel]
 *   and [onActionClick] are set.
 * @param onActionClick Invoked when the action button is tapped.
 * @param onCloseClick Invoked when the close button is tapped. `null` hides the close button.
 */
@Composable
fun TangemToast(
    label: TextReference,
    modifier: Modifier = Modifier,
    appearance: TangemToast.Appearance = TangemToast.Appearance.Neutral,
    icon: ImageVector? = null,
    actionLabel: TextReference? = null,
    onActionClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null,
) {
    ToastSurface(modifier = modifier) {
        ToastContent(
            label = label,
            appearance = appearance,
            icon = icon,
            actionLabel = actionLabel,
            onActionClick = onActionClick,
            onCloseClick = onCloseClick,
        )
    }
}

/**
 * Design-system v2 (DS3) **Toast** drawn from a [TangemToastMessage] — see the base [TangemToast] overload.
 *
 * Version: 1.1.1
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=13978-477)
 *
 * @param message Label, appearance, icon, action and closability of the toast. Its duration and dismiss callback
 *   are ignored: they matter only to a [TangemToastStack].
 * @param modifier Modifier applied to the toast.
 * @param onCloseClick Invoked when the close button is tapped; the button shows only when
 *   [TangemToastMessage.isClosable] is `true`.
 */
@Composable
fun TangemToast(message: TangemToastMessage, modifier: Modifier = Modifier, onCloseClick: () -> Unit = {}) {
    TangemToast(
        label = message.label,
        modifier = modifier,
        appearance = message.appearance,
        icon = message.icon,
        actionLabel = message.actionLabel,
        onActionClick = message.action,
        onCloseClick = onCloseClick.takeIf { message.isClosable },
    )
}

/** Public API surface of [TangemToast]. */
object TangemToast {

    val version = DsVersion(major = 1, minor = 1, patch = 1)

    /** Leading icon and its tint. */
    enum class Appearance {
        /** Custom icon passed by the caller, tinted with the primary icon color. */
        Neutral,

        /** Fixed error icon, tinted with the error status color. */
        Error,

        /** Fixed info icon, tinted with the info status color. */
        Info,

        /** Fixed success icon, tinted with the success status color. */
        Success,
    }

    /**
     * How long a [TangemToastStack] keeps the toast on screen before dismissing it. A label that wraps onto
     * several lines is always kept for [Long].
     */
    enum class Duration {
        /** 2 seconds. */
        Short,

        /** 3 seconds. */
        Long,
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemToastPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .width(402.dp)
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TangemToast.Appearance.entries.forEach { appearance ->
                TangemToast(
                    label = stringReference("Toast label"),
                    appearance = appearance,
                    icon = Icons.ic_eye_cross_20,
                    onCloseClick = {},
                )
            }
            TangemToast(
                label = stringReference("Balance is currently hidden"),
                icon = Icons.ic_eye_cross_20,
                actionLabel = stringReference("Unhide"),
                onActionClick = {},
            )
            TangemToast(
                label = stringReference("Address copied"),
                appearance = TangemToast.Appearance.Success,
                onCloseClick = null,
            )
            TangemToast(
                label = stringReference(
                    "Long toast message that wraps onto more lines and keeps going until it is cut after the " +
                        "third line, with an ellipsis at the end of it",
                ),
                icon = Icons.ic_eye_cross_20,
                onCloseClick = {},
            )
        }
    }
}