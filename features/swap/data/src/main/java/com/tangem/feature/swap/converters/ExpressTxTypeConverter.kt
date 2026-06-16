package com.tangem.feature.swap.converters

import com.tangem.datasource.api.express.models.response.TxType
import com.tangem.feature.swap.domain.models.domain.ExpressTxType

internal fun TxType.toDomain(): ExpressTxType = when (this) {
    TxType.SEND -> ExpressTxType.SEND
    TxType.SWAP -> ExpressTxType.SWAP
}