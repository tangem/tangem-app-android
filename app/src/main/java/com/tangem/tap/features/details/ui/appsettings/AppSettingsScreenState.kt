package com.tangem.tap.features.details.ui.appsettings

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class AppSettingsScreenState {

    object Loading : AppSettingsScreenState()

    data class Content(
        val items: ImmutableList<Item>,
        val dialog: Dialog?,
    ) : AppSettingsScreenState()

    @Immutable
    sealed class Item {

        abstract val id: String

        data class Card(
            override val id: String,
            @DrawableRes val iconResId: Int,
            val title: TextReference,
            val description: TextReference,
            val onClick: () -> Unit,
        ) : Item()

        data class Switch(
            override val id: String,
            val title: TextReference,
            val description: TextReference,
            val isEnabled: Boolean,
            val isChecked: Boolean,
            val onCheckedChange: (Boolean) -> Unit,
        ) : Item()

        data class Button(
            override val id: String,
            val title: TextReference,
            val description: TextReference,
            val isEnabled: Boolean,
            val onClick: () -> Unit,
        ) : Item()
    }

    @Immutable
    sealed class Dialog {

        abstract val onDismiss: () -> Unit

        data class Alert(
            val title: TextReference,
            val description: TextReference,
            val confirmText: TextReference,
            val onConfirm: () -> Unit,
            override val onDismiss: () -> Unit,
        ) : Dialog()

        data class Selector(
            val title: TextReference,
            val selectedItemIndex: Int,
            val items: ImmutableList<TextReference>,
            val onSelect: (Int) -> Unit,
            override val onDismiss: () -> Unit,
        ) : Dialog()
    }
}