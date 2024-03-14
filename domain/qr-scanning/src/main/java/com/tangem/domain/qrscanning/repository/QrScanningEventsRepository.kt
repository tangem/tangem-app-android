package com.tangem.domain.qrscanning.repository

import com.tangem.domain.qrscanning.models.QrResult
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.tokens.model.CryptoCurrency
import kotlinx.coroutines.flow.Flow

interface QrScanningEventsRepository {

    suspend fun emitResult(type: SourceType, qrCode: String)

    fun subscribeToScanningResults(type: SourceType): Flow<String>

    fun parseQrCode(qrCode: String, cryptoCurrency: CryptoCurrency): QrResult
}