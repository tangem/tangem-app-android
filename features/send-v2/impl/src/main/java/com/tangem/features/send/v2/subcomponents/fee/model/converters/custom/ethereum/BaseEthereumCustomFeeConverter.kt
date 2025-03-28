package com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.ethereum

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.CustomFeeConverter
import com.tangem.features.send.v2.subcomponents.fee.ui.state.CustomFeeFieldUM
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
        customValues: ImmutableList<CustomFeeFieldUM>,
        index: Int,
        value: String,
    ): ImmutableList<CustomFeeFieldUM>
}