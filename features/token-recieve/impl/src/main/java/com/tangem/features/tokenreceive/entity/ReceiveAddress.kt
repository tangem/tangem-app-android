package com.tangem.features.tokenreceive.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

internal data class ReceiveAddress(
    val value: String,
    val type: Type,
) {

    @Immutable
    sealed interface Type {
        data object Ens : Type

        sealed interface Primary : Type {

            val displayName: TextReference

            data class Default(override val displayName: TextReference) : Primary

            data class Legacy(override val displayName: TextReference) : Primary
        }
    }
}