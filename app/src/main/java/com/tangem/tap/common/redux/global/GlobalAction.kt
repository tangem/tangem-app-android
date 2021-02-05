package com.tangem.tap.common.redux.global

import com.tangem.tap.domain.config.ConfigManager
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.features.details.redux.SecurityOption
import org.rekotlin.Action
import java.math.BigDecimal

sealed class GlobalAction : Action {

    data class SaveScanNoteResponse(val scanNoteResponse: ScanNoteResponse) : GlobalAction()
    data class SetFiatRate(
            val fiatRates: Pair<CryptoCurrencyName, BigDecimal>
    ) : GlobalAction()
    data class ChangeAppCurrency(val appCurrency: FiatCurrencyName) : GlobalAction()
    object RestoreAppCurrency : GlobalAction() {
        data class Success(val appCurrency: FiatCurrencyName) : GlobalAction()
    }
    data class UpdateWalletSignedHashes(val walletSignedHashes: Int?) : GlobalAction()
    data class SetConfigManager(val configManager: ConfigManager) : GlobalAction()
    data class UpdateSecurityOptions(val securityOption: SecurityOption) : GlobalAction()
}