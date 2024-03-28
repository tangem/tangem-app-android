package com.tangem.tap.features.details.redux

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.entities.Button
import org.rekotlin.StateType
import java.util.EnumSet

data class DetailsState(
    val scanResponse: ScanResponse? = null,
    val cardSettingsState: CardSettingsState? = null,
    val privacyPolicyUrl: String? = null,
    val createBackupAllowed: Boolean = false,
    val isScanningInProgress: Boolean = false,
    val error: TextReference? = null,
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
    val resetButtonEnabled: Boolean,
    val condition1Checked: Boolean,
    val condition2Checked: Boolean,
    val accessCodeRecovery: AccessCodeRecoveryState? = null,
    val isShowPasswordResetRadioButton: Boolean,
    val isLastWarningDialogShown: Boolean,
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
    val isHidingEnabled: Boolean = false,
    val isInProgress: Boolean = false,
    val selectedAppCurrency: AppCurrency = AppCurrency.Default,
    val selectedThemeMode: AppThemeMode = AppThemeMode.DEFAULT,
)

enum class SecurityOption { LongTap, PassCode, AccessCode }

enum class AppSetting {
    SaveWallets, SaveAccessCode
}
