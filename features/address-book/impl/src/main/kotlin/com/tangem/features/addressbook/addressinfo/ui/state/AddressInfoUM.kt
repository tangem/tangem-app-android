package com.tangem.features.addressbook.addressinfo.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal data class AddressInfoUM(
    val address: String,
    val networkCount: Int,
    val onCopy: () -> Unit,
    val onEditAddress: () -> Unit,
    val onDeleteAddress: () -> Unit,
)