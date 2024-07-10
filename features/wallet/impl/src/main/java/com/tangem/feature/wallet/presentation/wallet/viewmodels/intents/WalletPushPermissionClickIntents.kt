package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.domain.settings.DelayPermissionRequestUseCase
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.domain.settings.SetFirstTimeAskingPermissionUseCase
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface WalletPushPermissionClickIntents {

    fun onRequestPushPermission()

    fun onDelayAskPushPermission()

    fun onNeverAskPushPermission()
}

@ViewModelScoped
internal class WalletPushPermissionClickIntentsImplementor @Inject constructor(
    private val setFirstTimeAskingPermissionUseCase: SetFirstTimeAskingPermissionUseCase,
    private val neverRequestPermissionUseCase: NeverRequestPermissionUseCase,
    private val delayPermissionRequestUseCase: DelayPermissionRequestUseCase,
) : BaseWalletClickIntents(), WalletPushPermissionClickIntents {

    override fun onRequestPushPermission() {
        viewModelScope.launch {
            setFirstTimeAskingPermissionUseCase(PUSH_PERMISSION)
        }
    }

    override fun onDelayAskPushPermission() {
        viewModelScope.launch {
            delayPermissionRequestUseCase(PUSH_PERMISSION)
        }
    }

    override fun onNeverAskPushPermission() {
        viewModelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
        }
    }
}