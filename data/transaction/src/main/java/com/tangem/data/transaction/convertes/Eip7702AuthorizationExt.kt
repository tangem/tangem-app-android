package com.tangem.data.transaction.convertes

import com.tangem.datasource.api.gasless.models.Eip7702AuthorizationDTO
import com.tangem.domain.transaction.models.Eip7702Authorization

/**
 * Converts domain [Eip7702Authorization] to its DTO representation.
 * Shared by both single-transaction and batch-transaction request builders.
 */
internal fun Eip7702Authorization.toDTO(): Eip7702AuthorizationDTO {
    return Eip7702AuthorizationDTO(
        chainId = chainId,
        address = address,
        nonce = nonce.toString(),
        yParity = yParity,
        r = r,
        s = s,
    )
}