@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.modal

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.utils.WindowInsetsZero

/**
 * [TangemTopNavigation] preset for the [TangemModal] `title` slot — use it instead of the raw bar
 * whenever a modal needs a top navigation.
 *
 * Bakes in what every modal bar repeats: zero window insets (the card already clears the status
 * bar), horizontal-16dp content padding, centered content and no fade/blur behind the row (the
 * title is pinned by the modal itself).
 *
 * @param title title text
 * @param modifier modifier for the bar
 * @param subtitle optional subtitle below [title]
 * @param contentAlign horizontal alignment of the title column; centered by default
 * @param isEndButtonsGroupBackgroundShown whether [endButtonsGroup] keeps its material pill
 * @param onBack when non-null, shows the predefined back button (see [TangemButton.Back])
 * @param endButtonsGroup optional secondary actions placed before the close button
 * @param onClose when non-null, shows the predefined close button (see [TangemButton.Close])
 */
@Composable
fun TangemModal.TopNavigation(
    title: TextReference,
    modifier: Modifier = Modifier,
    subtitle: TextReference? = null,
    contentAlign: TangemTopNavigation.ContentAlign = TangemTopNavigation.ContentAlign.Center,
    isEndButtonsGroupBackgroundShown: Boolean = true,
    onBack: (() -> Unit)? = null,
    endButtonsGroup: (@Composable RowScope.() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    TangemTopNavigation(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        contentAlign = contentAlign,
        windowInsets = WindowInsetsZero,
        contentPadding = ModalTopNavigationPadding,
        blurBackground = false,
        fadeEnabled = false,
        isEndButtonsGroupBackgroundShown = isEndButtonsGroupBackgroundShown,
        onBack = onBack,
        endButtonsGroup = endButtonsGroup,
        onClose = onClose,
    )
}

/**
 * Slot-based variant of [TopNavigation] — same modal preset, custom [startButton] / [endButton] /
 * [contentColumn] slots for bars the text overload can't express.
 *
 * @param modifier modifier for the bar
 * @param contentAlign horizontal alignment of [contentColumn]; centered by default
 * @param isEndButtonsGroupBackgroundShown whether [endButtonsGroup] keeps its material pill
 * @param startButton leading slot, typically [TangemButton.Back]
 * @param endButtonsGroup optional secondary actions placed before [endButton]
 * @param endButton trailing slot, typically [TangemButton.Close]
 * @param contentColumn center slot for title/subtitle children
 */
@Composable
fun TangemModal.TopNavigation(
    modifier: Modifier = Modifier,
    contentAlign: TangemTopNavigation.ContentAlign = TangemTopNavigation.ContentAlign.Center,
    isEndButtonsGroupBackgroundShown: Boolean = true,
    startButton: (@Composable () -> Unit)? = null,
    endButtonsGroup: (@Composable RowScope.() -> Unit)? = null,
    endButton: (@Composable () -> Unit)? = null,
    contentColumn: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TangemTopNavigation(
        modifier = modifier,
        contentAlign = contentAlign,
        windowInsets = WindowInsetsZero,
        contentPadding = ModalTopNavigationPadding,
        blurBackground = false,
        fadeEnabled = false,
        isEndButtonsGroupBackgroundShown = isEndButtonsGroupBackgroundShown,
        startButton = startButton,
        endButtonsGroup = endButtonsGroup,
        endButton = endButton,
        contentColumn = contentColumn,
    )
}

private val ModalTopNavigationPadding = PaddingValues(horizontal = 16.dp)