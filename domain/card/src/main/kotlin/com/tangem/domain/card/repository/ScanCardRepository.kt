package com.tangem.domain.card.repository

import com.tangem.domain.card.model.ScanCardResult
import com.tangem.domain.core.utils.TextReference

interface ScanCardRepository {
    suspend fun scanCard(
        cardId: String?,
        message: TextReference?,
        allowRequestAccessCodeFromRepository: Boolean,
    ): Result<ScanCardResult>
}
