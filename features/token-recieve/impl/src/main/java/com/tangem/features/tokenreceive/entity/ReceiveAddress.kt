package com.tangem.features.tokenreceive.entity

import com.tangem.core.ui.extensions.TextReference

internal data class ReceiveAddress(
    val value: String,
    val type: Type,
) {
    sealed interface Type {
        data object Ens : Type

        data class Default(
            val displayName: TextReference,
        ) : Type
    }
}