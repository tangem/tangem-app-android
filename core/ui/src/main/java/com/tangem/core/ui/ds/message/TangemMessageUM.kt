package com.tangem.core.ui.ds.message

import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.button.*
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Data model representing the properties of a Tangem message.
 *
 * @param id             Unique identifier for the message.
 * @param title          TextReference for the message title.
 * @param subtitle       TextReference for the message subtitle.
 * @param messageEffect  TangemMessageEffect defining the visual effect of the message.
 * @param iconUM         Optional TangemIconUM representing the icon to be displayed in the message.
 * @param iconSize       Dp value defining the size of the icon (default is 28.dp).
 * @param isCentered     Boolean indicating whether the icon is centered.
 * @param buttonsUM      ImmutableList of TangemMessageButtonUM representing the buttons in the message.
 * @param onClick        Lambda to be invoked when the message is clicked (optional).
 * @param onCloseClick   Lambda to be invoked when the close button is clicked (optional).
 */
data class TangemMessageUM(
    val id: String,
    val title: TextReference,
    val subtitle: TextReference,
    val messageEffect: TangemMessageEffect = TangemMessageEffect.None,
    val iconUM: TangemIconUM? = null,
    val iconSize: Dp = 28.dp,
    val isCentered: Boolean = false,
    val buttonsUM: ImmutableList<TangemMessageButtonUM> = persistentListOf(),
    val onClick: (() -> Unit)? = null,
    val onCloseClick: (() -> Unit)? = null,
)

/**
 * Data model representing a button within a Tangem message.
 *
 * @param text      TextReference for the button label.
 * @param type      TangemButtonType defining the style type of the button.
 * @param iconRes   Drawable resource ID for the icon to be displayed in the button (optional).
 * @param onClick   Lambda to be invoked when the button is clicked.
 */
data class TangemMessageButtonUM(
    val text: TextReference,
    val type: TangemButtonType,
    @DrawableRes val iconRes: Int? = null,
    val onClick: () -> Unit,
) {
    /** Creates a TangemButtonUM representation of this message button. */
    val tangemButtonUM = TangemButtonUM(
        text = text,
        size = TangemButtonSize.X9,
        shape = TangemButtonShape.Rounded,
        iconRes = iconRes,
        iconPosition = TangemButtonIconPosition.End,
        type = type,
        onClick = onClick,
    )
}