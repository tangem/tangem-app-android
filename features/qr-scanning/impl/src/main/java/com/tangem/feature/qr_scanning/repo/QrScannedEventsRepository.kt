package com.tangem.feature.qr_scanning.repo

import com.tangem.feature.qr_scanning.SourceType
import kotlinx.coroutines.flow.Flow

interface QrScannedEventsRepository {

    fun emitScan(type: SourceType, qrCode: String)

    fun subscribeToScan(type: SourceType) : Flow<String>

}