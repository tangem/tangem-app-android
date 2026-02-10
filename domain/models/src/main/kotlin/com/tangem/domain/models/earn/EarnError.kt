package com.tangem.domain.models.earn

import java.util.UUID

sealed interface EarnError {

    data class HttpError(
        val id: String = UUID.randomUUID().toString(),
        val code: Int,
        val message: String,
    ) : EarnError

    data class NotHttpError(
        val id: String = UUID.randomUUID().toString(),
    ) : EarnError
}