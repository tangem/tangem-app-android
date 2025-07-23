package com.tangem.domain.qrscanning.repository

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.qrscanning.models.QrResult
import com.tangem.domain.qrscanning.models.RawQrResult
import com.tangem.domain.qrscanning.models.SourceType
import kotlinx.coroutines.flow.Flow

interface QrScanningEventsRepository {

    suspend fun emitResult(qrCode: RawQrResult)

    fun subscribeToScanningResults(type: SourceType): Flow<RawQrResult>

    fun parseQrCode(qrCode: String, cryptoCurrency: CryptoCurrency): QrResult
}