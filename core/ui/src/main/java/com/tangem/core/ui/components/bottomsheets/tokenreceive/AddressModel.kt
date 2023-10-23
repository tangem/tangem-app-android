package com.tangem.core.ui.components.bottomsheets.tokenreceive

data class AddressModel(
    val value: String,
    val type: Type = Type.Default,
) {
    enum class Type {
        Legacy, Default
    }
}