package com.tangem.tap.features.details.ui.cardsettings.coderecovery

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.common.doOnSuccess
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
import com.tangem.tap.features.details.ui.common.utils.isAccessCodeRecoveryEnabled
import com.tangem.tap.store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AccessCodeRecoveryViewModel @Inject constructor(
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val tangemSdkManager: TangemSdkManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userWalletId = savedStateHandle.get<Bundle>(AppRoute.AccessCodeRecovery.USER_WALLET_ID_KEY)
        ?.unbundle(UserWalletId.serializer())
        ?: error("UserWalletId is required for AccessCodeRecoveryViewModel")

    val screenState = MutableStateFlow(
        value = getInitialState(),
    )

    private fun getInitialState(): AccessCodeRecoveryScreenState {
        val userWallet = getUserWallet()

        val isEnabled = isAccessCodeRecoveryEnabled(
            typeResolver = userWallet.scanResponse.cardTypesResolver,
            card = userWallet.scanResponse.card,
        )

        return AccessCodeRecoveryScreenState(
            enabledOnCard = isEnabled,
            enabledSelection = isEnabled,
            isSaveChangesEnabled = false,
            onSaveChangesClick = ::saveChanges,
            onOptionClick = ::selectOption,
        )
    }

    private fun saveChanges() = viewModelScope.launch {
        val userWallet = getUserWallet()
        val isEnabled = screenState.value.enabledSelection

        tangemSdkManager
            .setAccessCodeRecoveryEnabled(userWallet.cardId, isEnabled)
            .doOnSuccess {
                Analytics.send(
                    Settings.CardSettings.AccessCodeRecoveryChanged(
                        AnalyticsParam.AccessCodeRecoveryStatus.from(isEnabled),
                    ),
                )

                store.dispatchNavigationAction(AppRouter::pop)
            }
    }

    private fun selectOption(isEnabled: Boolean) {
        screenState.update {
            it.copy(
                isSaveChangesEnabled = isEnabled != it.enabledOnCard,
            )
        }
    }

    private fun getUserWallet(): UserWallet {
        return getUserWalletUseCase(userWalletId).getOrElse {
            error("Unable to get user wallet $userWalletId: $it")
        }
    }
}