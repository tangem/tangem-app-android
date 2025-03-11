package com.tangem.tap.features.details.ui.cardsettings.domain

import com.tangem.domain.models.scan.ScanResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interactor for sharing logic and data between all card settings screens
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class CardSettingsInteractor @Inject constructor() {

    private val _scannedScanResponse = MutableStateFlow<ScanResponse?>(value = null)
    val scannedScanResponse: StateFlow<ScanResponse?> = _scannedScanResponse

    fun initialize(scanResponse: ScanResponse) {
        _scannedScanResponse.value = scanResponse
    }

    fun update(transform: (ScanResponse) -> ScanResponse) {
        _scannedScanResponse.update {
            requireNotNull(it)
            transform(it)
        }
    }

    fun clear() {
        _scannedScanResponse.value = null
    }
}