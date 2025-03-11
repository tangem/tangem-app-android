package com.tangem.features.send.impl.presentation.state.fee.custom

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList

internal interface CustomFeeConverter<T : Fee> : Converter<T, ImmutableList<SendTextField.CustomFee>> {
    fun convertBack(normalFee: T, value: ImmutableList<SendTextField.CustomFee>): T
}