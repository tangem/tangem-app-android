package com.tangem.tap.features.details.ui.securitymode

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.bundle.unbundle
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.domain.sdk.TangemSdkManager
import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.tap.features.details.ui.common.utils.getAllowedSecurityOptions
import com.tangem.tap.features.details.ui.common.utils.getCurrentSecurityOption
import com.tangem.tap.store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SecurityModeViewModel @Inject constructor(
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val tangemSdkManager: TangemSdkManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userWalletId = savedStateHandle.get<Bundle>(AppRoute.DetailsSecurity.USER_WALLET_ID_KEY)
        ?.unbundle(UserWalletId.serializer())
        ?: error("UserWalletId is required for SecurityModeViewModel")

    val screenState = MutableStateFlow(
        value = getInitialState(),
    )

    private fun getInitialState(): SecurityModeScreenState {
        val userWallet = getUserWallet()
        val scanResponse = userWallet.scanResponse
        val card = scanResponse.card
        val cardTypesResolver = scanResponse.cardTypesResolver

        val currentSecurityOption = getCurrentSecurityOption(card)
        val allowedSecurityOptions = getAllowedSecurityOptions(card, cardTypesResolver, currentSecurityOption)

        return SecurityModeScreenState(
            availableOptions = allowedSecurityOptions.toList(),
            selectedSecurityMode = currentSecurityOption,
            isSaveChangesEnabled = false,
            onNewModeSelected = ::selectOption,
            onSaveChangesClicked = ::saveChanges,
        )
    }

    private fun selectOption(securityOption: SecurityOption) {
        screenState.update { state ->
            state.copy(
                selectedSecurityMode = securityOption,
                isSaveChangesEnabled = securityOption != getCurrentSecurityOption(getUserWallet().scanResponse.card),
            )
        }
    }

    private fun saveChanges() {
        val userWallet = getUserWallet()
        val cardId = userWallet.cardId
        val selectedOption = screenState.value.selectedSecurityMode

        viewModelScope.launch {
            val result = when (selectedOption) {
                SecurityOption.LongTap -> tangemSdkManager.setLongTap(cardId)
                SecurityOption.PassCode -> tangemSdkManager.setPasscode(cardId)
                SecurityOption.AccessCode -> tangemSdkManager.setAccessCode(cardId)
            }

            val paramValue = AnalyticsParam.SecurityMode.from(selectedOption)
            when (result) {
                is CompletionResult.Success -> {
                    Analytics.send(Settings.CardSettings.SecurityModeChanged(paramValue))

                    store.dispatchNavigationAction(AppRouter::pop)
                }
                is CompletionResult.Failure -> {
                    val error = result.error
                    if (error is TangemSdkError && error !is TangemSdkError.UserCancelled) {
                        Analytics.send(Settings.CardSettings.SecurityModeChanged(paramValue, error))
                    }
                }
                else -> Unit
            }
        }
    }

    private fun getUserWallet(): UserWallet {
        return getUserWalletUseCase(userWalletId).getOrElse {
            error("Unable to get user wallet $userWalletId: $it")
        }
    }
}