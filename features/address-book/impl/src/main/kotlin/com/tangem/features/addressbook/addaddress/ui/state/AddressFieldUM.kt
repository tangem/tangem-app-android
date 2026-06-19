package com.tangem.features.addressbook.addaddress.ui.state

import com.tangem.core.ui.extensions.TextReference

internal data class AddressFieldUM(
    val value: String,
    val placeholder: TextReference,
    val label: TextReference,
    val isError: Boolean = false,
)