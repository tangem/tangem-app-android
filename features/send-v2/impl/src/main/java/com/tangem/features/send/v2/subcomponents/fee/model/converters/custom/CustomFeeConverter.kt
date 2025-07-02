package com.tangem.features.send.v2.subcomponents.fee.model.converters.custom

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.features.feeselector.api.entity.CustomFeeFieldUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList

internal interface CustomFeeConverter<T : Fee> : Converter<T, ImmutableList<CustomFeeFieldUM>> {
    fun convertBack(normalFee: T, value: ImmutableList<CustomFeeFieldUM>): T
}

internal fun MutableList<CustomFeeFieldUM>.setEmpty(index: Int) {
    set(index, this[index].copy(value = ""))
}