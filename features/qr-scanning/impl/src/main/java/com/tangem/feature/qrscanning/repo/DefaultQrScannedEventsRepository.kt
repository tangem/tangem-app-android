package com.tangem.feature.qrscanning.repo

import com.tangem.feature.qrscanning.SourceType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

internal class DefaultQrScannedEventsRepository : QrScannedEventsRepository {

    private data class QrScanEvent(val type: SourceType, val qrCode: String)

    private val scannedEvents = MutableSharedFlow<QrScanEvent>()

    override fun emitScan(type: SourceType, qrCode: String) {
        scannedEvents.tryEmit(QrScanEvent(type, qrCode))
    }

    override fun subscribeToScan(type: SourceType) = scannedEvents
        .filter { it.type == type }
        .map { it.qrCode }
}
