package com.tangem.features.addressbook.addaddress.contract

import com.tangem.core.ui.extensions.TextReference

internal data class AddressFieldUM(
    val value: String,
    val placeholder: TextReference,
    val label: TextReference,
    val isError: Boolean = false,
    val error: TextReference? = null,
    val isValuePasted: Boolean = false,
    val blockchainAddress: String? = null,
)