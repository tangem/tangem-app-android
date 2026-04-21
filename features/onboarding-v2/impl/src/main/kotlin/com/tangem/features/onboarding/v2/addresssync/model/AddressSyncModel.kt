package com.tangem.features.onboarding.v2.addresssync.model

import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.domain.settings.ShouldShowAskBiometryUseCase
import com.tangem.features.onboarding.v2.addresssync.navigation.AddressSyncStep
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(DelicateDecomposeApi::class)
@ModelScoped
internal class AddressSyncModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val shouldShowAskBiometryUseCase: ShouldShowAskBiometryUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase,
) : Model() {

    val stackNavigation = StackNavigation<AddressSyncStep>()

    fun onIntent(intent: AddressSyncIntent) {
        when (intent) {
            is AddressSyncIntent.Next -> nextScreen(intent)
            AddressSyncIntent.Back -> goBack()
        }
    }

    private fun nextScreen(next: AddressSyncIntent.Next) {
        val (nextStep, replace) = next
        if (replace) {
            stackNavigation.replaceCurrent(configuration = nextStep)
        } else {
            stackNavigation.push(configuration = nextStep)
        }
        modelScope.launch { trySkippingScreen(next) }
    }

    private suspend fun trySkippingScreen(next: AddressSyncIntent.Next) {
        when (next.step) {
            AddressSyncStep.ASK_BIOMETRY -> {
                val showBiometry = canUseBiometryUseCase.strict() && shouldShowAskBiometryUseCase()
                if (showBiometry.not()) {
                    nextScreen(AddressSyncIntent.Next(step = AddressSyncStep.ASK_NOTIFICATIONS, replace = true))
                }
            }
            AddressSyncStep.ASK_NOTIFICATIONS -> {
                val showNotification = shouldAskPermissionUseCase(PUSH_PERMISSION)
                if (showNotification.not()) {
                    nextScreen(AddressSyncIntent.Next(step = AddressSyncStep.ADDRESS_SYNC, replace = true))
                }
            }
            AddressSyncStep.ADDRESS_SYNC -> Unit
        }
    }

    private fun goBack() {
        stackNavigation.pop()
    }
}