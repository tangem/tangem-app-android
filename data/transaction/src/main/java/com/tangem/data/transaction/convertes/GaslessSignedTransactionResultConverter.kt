package com.tangem.data.transaction.convertes

import com.tangem.datasource.api.gasless.models.GaslessSignedTransactionResultDTO
import com.tangem.domain.transaction.models.GaslessSignedTransactionResult
import com.tangem.utils.converter.Converter

/**
 * Converts DTO GaslessSignedTransactionResult from API to domain model.
 * Transforms string representations of gas parameters to BigInteger for type safety.
 */
class GaslessSignedTransactionResultConverter :
    Converter<GaslessSignedTransactionResultDTO, GaslessSignedTransactionResult> {

    override fun convert(value: GaslessSignedTransactionResultDTO): GaslessSignedTransactionResult {
        return GaslessSignedTransactionResult(
            txHash = value.txHash,
        )
    }
}