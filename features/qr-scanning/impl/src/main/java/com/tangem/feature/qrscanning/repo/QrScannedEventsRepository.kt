package com.tangem.feature.qrscanning.repo

import com.tangem.feature.qrscanning.SourceType
import kotlinx.coroutines.flow.Flow

interface QrScannedEventsRepository {

    fun emitScan(type: SourceType, qrCode: String)

    fun subscribeToScan(type: SourceType): Flow<String>
}
