package com.tangem.features.send.impl.presentation.state.fee.custom

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import kotlinx.collections.immutable.ImmutableList

/**
 * Base ethereum custom fee converter
 *
 * @param T subtype of [Fee.Ethereum]
 *
[REDACTED_AUTHOR]
 */
internal interface BaseEthereumCustomFeeConverter<T : Fee.Ethereum> : CustomFeeConverter<T> {

    fun getGasLimitIndex(feeValue: T): Int

    fun onValueChange(
        feeValue: T,
        customValues: ImmutableList<SendTextField.CustomFee>,
        index: Int,
        value: String,
    ): ImmutableList<SendTextField.CustomFee>
}