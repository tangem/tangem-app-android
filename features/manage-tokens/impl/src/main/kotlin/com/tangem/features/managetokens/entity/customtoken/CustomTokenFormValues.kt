package com.tangem.features.managetokens.entity.customtoken

import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM.TokenFormUM
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
internal value class CustomTokenFormValues private constructor(private val values: List<String>) {

    constructor() : this(values = emptyList())

    constructor(form: TokenFormUM?) : this(
        values = if (form == null) {
            emptyList()
        } else {
            listOf(
                form.contractAddress.value,
                form.name.value,
                form.symbol.value,
                form.decimals.value,
            )
        },
    )

    fun fillValues(to: TokenFormUM): TokenFormUM = to.copy(
        contractAddress = to.contractAddress.copy(value = values.getOrElse(index = 0) { "" }),
        name = to.name.copy(value = values.getOrElse(index = 1) { "" }),
        symbol = to.symbol.copy(value = values.getOrElse(index = 2) { "" }),
        decimals = to.decimals.copy(value = values.getOrElse(index = 3) { "" }),
    )
}