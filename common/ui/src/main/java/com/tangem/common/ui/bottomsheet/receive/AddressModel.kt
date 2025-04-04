package com.tangem.common.ui.bottomsheet.receive

import com.tangem.core.ui.extensions.TextReference

data class AddressModel(
    val displayName: TextReference,
    val fullName: TextReference,
    val value: String,
    val type: Type,
) {
    enum class Type {
        Legacy, Default
    }
}