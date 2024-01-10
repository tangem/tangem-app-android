package com.tangem.feature.qrscanning.repo

import com.tangem.feature.qrscanning.SourceType
import kotlinx.coroutines.flow.*

internal class DefaultQrScanningEventsRepository : QrScanningEventsRepository {

    private data class QrScanningEvent(val type: SourceType, val qrCode: String)

    private val scannedEvents = MutableSharedFlow<QrScanningEvent>()

    override suspend fun emitResult(type: SourceType, qrCode: String) {
        scannedEvents.emit(QrScanningEvent(type, qrCode))
    }

    override fun subscribeToScanningResults(type: SourceType) = scannedEvents
        .filter { it.type == type }
        .map { it.qrCode }
}