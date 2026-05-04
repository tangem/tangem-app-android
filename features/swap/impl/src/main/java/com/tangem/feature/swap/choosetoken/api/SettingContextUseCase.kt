package com.tangem.feature.swap.choosetoken.api

import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// todo swap move to some common module
class SettingContextUseCase @Inject constructor(
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
) {

    suspend fun invokeSync(): SettingContext = invoke().first()

    operator fun invoke(): Flow<SettingContext> = combine(
        flow = isAccountsModeEnabledUseCase.invoke(),
        flow2 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
        flow3 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
        transform = { isAccountsModeEnabled, selectedAppCurrency, balanceHidingSettings ->
            SettingContext(
                isAccountsMode = isAccountsModeEnabled,
                appCurrency = selectedAppCurrency,
                isBalanceHidden = balanceHidingSettings,
            )
        },
    ).distinctUntilChanged()
}

data class SettingContext(
    val isAccountsMode: Boolean,
    val appCurrency: AppCurrency,
    val isBalanceHidden: Boolean,
)