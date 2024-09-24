package com.tangem.features.managetokens.entity.customtoken

import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM.TokenFormUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM.TokenFormUM.Field
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.serialization.Serializable

@Serializable
internal class CustomTokenFormValues(
    private val contractAddress: String = "",
    private val name: String = "",
    private val symbol: String = "",
    private val decimals: String = "",
) {

    constructor(form: TokenFormUM?) : this(
        contractAddress = form?.fields?.get(Field.CONTRACT_ADDRESS)?.value.orEmpty(),
        name = form?.fields?.get(Field.NAME)?.value.orEmpty(),
        symbol = form?.fields?.get(Field.SYMBOL)?.value.orEmpty(),
        decimals = form?.fields?.get(Field.DECIMALS)?.value.orEmpty(),
    )

    fun fillValues(to: TokenFormUM): TokenFormUM = to.copy(
        fields = to.fields.mapValues { (key, field) ->
            field.copy(
                value = when (key) {
                    Field.CONTRACT_ADDRESS -> contractAddress
                    Field.NAME -> name
                    Field.SYMBOL -> symbol
                    Field.DECIMALS -> decimals
                },
            )
        }.toPersistentMap(),
    )
}