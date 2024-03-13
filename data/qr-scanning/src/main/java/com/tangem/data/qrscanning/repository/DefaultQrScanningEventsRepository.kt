package com.tangem.data.qrscanning.repository

import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

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