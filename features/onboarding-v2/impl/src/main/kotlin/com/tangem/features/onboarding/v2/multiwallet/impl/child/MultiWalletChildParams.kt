package com.tangem.features.onboarding.v2.multiwallet.impl.child

import com.tangem.common.card.Card.Manufacturer
import com.tangem.common.card.FirmwareVersion
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.MultiWalletInnerNavigationState
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import kotlinx.coroutines.flow.MutableStateFlow

class MultiWalletChildParams(
    val multiWalletState: MutableStateFlow<OnboardingMultiWalletState>,
    val innerNavigation: MutableStateFlow<MultiWalletInnerNavigationState>,
    val backups: MutableStateFlow<Backup>,
    val parentParams: OnboardingMultiWalletComponent.Params,
) {
    data class Backup(
        val card2: BackupCardInfo? = null,
        val card3: BackupCardInfo? = null,
    ) {
        data class BackupCardInfo(
            val cardId: String,
            val cardPublicKey: ByteArray,
            val manufacturer: Manufacturer,
            val firmwareVersion: FirmwareVersion,
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as BackupCardInfo

                if (cardId != other.cardId) return false
                if (!cardPublicKey.contentEquals(other.cardPublicKey)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = cardId.hashCode()
                result = 31 * result + cardPublicKey.contentHashCode()
                return result
            }
        }
    }
}