package com.tangem.core.ui.components.bottomsheets.tokenreceive

import com.tangem.core.ui.extensions.TextReference

data class AddressModel(
    val displayName: TextReference,
    val value: String,
    val type: Type,
) {
    enum class Type {
        Legacy, Default
    }
}