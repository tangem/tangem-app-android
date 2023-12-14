package com.tangem.feature.qrscanning.repo

import com.tangem.feature.qrscanning.SourceType
import kotlinx.coroutines.flow.Flow

interface QrScanningEventsRepository {

    fun emitResult(type: SourceType, qrCode: String)

    fun subscribeToScanningResults(type: SourceType): Flow<String>
}
