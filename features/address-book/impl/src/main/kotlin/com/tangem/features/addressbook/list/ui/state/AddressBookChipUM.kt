package com.tangem.features.addressbook.list.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class AddressBookChipUM(
    val id: String,
    val text: TextReference,
    val isSelected: Boolean,
    val onClick: () -> Unit,
    @DrawableRes val iconRes: Int? = null,
)