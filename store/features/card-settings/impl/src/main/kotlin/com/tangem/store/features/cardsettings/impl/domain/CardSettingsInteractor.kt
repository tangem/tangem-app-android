package com.tangem.store.features.cardsettings.impl.domain

import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the card scanned on the card settings screen and the card rules shared by all card settings screens.
 */
@Singleton
internal class CardSettingsInteractor @Inject constructor() {

    val scannedScanResponse: StateFlow<ScanResponse?>
        field = MutableStateFlow<ScanResponse?>(value = null)

    fun initialize(scanResponse: ScanResponse) {
        scannedScanResponse.value = scanResponse
    }

    fun clear() {
        scannedScanResponse.value = null
    }

    fun requireScanResponse(): ScanResponse = requireNotNull(scannedScanResponse.value) { "Scan response is null" }

    fun getSecurityOption(card: CardDTO): SecurityOption = when {
        card.isAccessCodeSet -> SecurityOption.AccessCode
        card.isPasscodeSet == true -> SecurityOption.PassCode
        else -> SecurityOption.LongTap
    }

    fun getAllowedSecurityOptions(scanResponse: ScanResponse): Set<SecurityOption> {
        val cardTypesResolver = scanResponse.cardTypesResolver
        val currentSecurityOption = getSecurityOption(scanResponse.card)

        return when {
            cardTypesResolver.isStart2Coin() || cardTypesResolver.isTangemNote() -> EnumSet.of(SecurityOption.LongTap)
            scanResponse.card.settings.isBackupAllowed -> EnumSet.of(currentSecurityOption)
            else -> EnumSet.of(SecurityOption.LongTap, currentSecurityOption).apply {
                if (cardTypesResolver.isTangemTwins()) add(SecurityOption.PassCode)
            }
        }
    }

    fun isAccessCodeRecoveryAllowed(scanResponse: ScanResponse): Boolean = scanResponse.cardTypesResolver.isWallet2()

    fun isAccessCodeRecoveryEnabled(scanResponse: ScanResponse): Boolean {
        return isAccessCodeRecoveryAllowed(scanResponse) &&
            scanResponse.card.userSettings?.isUserCodeRecoveryAllowed == true
    }

    /** Call only after the [option] has actually been set on the card */
    fun updateSecurityOption(option: SecurityOption) {
        updateCard { card ->
            card.copy(
                isAccessCodeSet = option == SecurityOption.AccessCode,
                isPasscodeSet = option == SecurityOption.PassCode,
            )
        }
    }

    /** Call only after the access code recovery has actually been changed on the card */
    fun updateAccessCodeRecovery(isEnabled: Boolean) {
        updateCard { card ->
            card.copy(userSettings = card.userSettings?.copy(isUserCodeRecoveryAllowed = isEnabled))
        }
    }

    private fun updateCard(transform: (CardDTO) -> CardDTO) {
        scannedScanResponse.update { scanResponse ->
            scanResponse?.copy(card = transform(scanResponse.card)) ?: error("Scan response is null")
        }
    }
}