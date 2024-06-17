package com.tangem.features.pushnotifications.impl.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.domain.settings.DelayPermissionRequestUseCase
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.domain.settings.SetFirstTimeAskingPermissionUseCase
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.features.pushnotifications.impl.navigation.DefaultPushNotificationsRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PushNotificationViewModel @Inject constructor(
    private val setFirstTimeAskingPermissionUseCase: SetFirstTimeAskingPermissionUseCase,
    private val delayPermissionRequestUseCase: DelayPermissionRequestUseCase,
    private val neverRequestPermissionUseCase: NeverRequestPermissionUseCase,
    private val router: DefaultPushNotificationsRouter,
    private val settingsManager: SettingsManager,
) : ViewModel(), PushNotificationsClickIntents {

    override fun onAskLater() {
        viewModelScope.launch {
            delayPermissionRequestUseCase(PUSH_PERMISSION)
            setFirstTimeAskingPermissionUseCase(PUSH_PERMISSION)
        }
        router.openHome()
    }

    override fun onAllowPermission() {
        viewModelScope.launch {
            setFirstTimeAskingPermissionUseCase(PUSH_PERMISSION)
        }
    }

    override fun onAllowedPermission() {
        viewModelScope.launch {
            neverRequestPermissionUseCase(PUSH_PERMISSION)
        }
        router.openHome()
    }

    override fun openSettings() = settingsManager.openSettings()
}