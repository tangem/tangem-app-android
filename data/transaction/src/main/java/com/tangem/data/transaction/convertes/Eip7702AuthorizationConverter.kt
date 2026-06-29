package com.tangem.data.transaction.convertes

import com.tangem.datasource.api.gasless.models.Eip7702AuthorizationDTO
import com.tangem.domain.transaction.models.Eip7702Authorization
import com.tangem.utils.converter.Converter

/**
 * Converts domain [Eip7702Authorization] to its DTO representation.
 * Shared by both single-transaction and batch-transaction request builders.
 */
class Eip7702AuthorizationConverter : Converter<Eip7702Authorization, Eip7702AuthorizationDTO> {

    override fun convert(value: Eip7702Authorization): Eip7702AuthorizationDTO {
        return Eip7702AuthorizationDTO(
            chainId = value.chainId,
            address = value.address,
            nonce = value.nonce.toString(),
            yParity = value.yParity,
            r = value.r,
            s = value.s,
        )
    }
}