package com.tangem.tap.features.details.redux

import com.tangem.blockchain.common.Wallet
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.entities.Button
import com.tangem.tap.common.entities.FiatCurrency
import org.rekotlin.StateType
import java.util.*

data class DetailsState(
    val scanResponse: ScanResponse? = null,
    val wallets: List<Wallet> = emptyList(),
    val cardSettingsState: CardSettingsState? = null,
    val privacyPolicyUrl: String? = null,
    val createBackupAllowed: Boolean = false,
    val appCurrency: FiatCurrency = FiatCurrency.Default,
    val appSettingsState: AppSettingsState = AppSettingsState(),
) : StateType

data class CardInfo(
    val cardId: String,
    val issuer: String,
    val signedHashes: Int,
    val isTwin: Boolean,
    val hasBackup: Boolean,
)

/**
 * @property enabledOnCard whether access code recovery is enabled on card
 * @property enabledSelection current selected option in app (not saved on card yet)
 */
data class AccessCodeRecoveryState(
    val enabledOnCard: Boolean,
    val enabledSelection: Boolean,
)

data class CardSettingsState(
    val cardInfo: CardInfo,
    val card: CardDTO,
    val manageSecurityState: ManageSecurityState?,
    val resetCardAllowed: Boolean,
    val resetConfirmed: Boolean = false,
    val accessCodeRecovery: AccessCodeRecoveryState? = null,
)

data class ManageSecurityState(
    val currentOption: SecurityOption = SecurityOption.LongTap,
    val selectedOption: SecurityOption = currentOption,
    val allowedOptions: EnumSet<SecurityOption> = EnumSet.allOf(SecurityOption::class.java),
    val buttonProceed: Button = Button(true),
)

data class AppSettingsState(
    val saveWallets: Boolean = false,
    val saveAccessCodes: Boolean = false,
    val isBiometricsAvailable: Boolean = false,
    val needEnrollBiometrics: Boolean = false,
    val isInProgress: Boolean = false,
)

enum class SecurityOption { LongTap, PassCode, AccessCode }

enum class AppSetting {
    SaveWallets, SaveAccessCode
}
