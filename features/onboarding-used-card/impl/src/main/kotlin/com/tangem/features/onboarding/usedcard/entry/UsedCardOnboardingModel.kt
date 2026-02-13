package com.tangem.features.onboarding.usedcard.entry

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.domain.settings.ShouldShowAskBiometryUseCase
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.onboarding.usedcard.UsedCardOnboardingComponent
import com.tangem.features.onboarding.usedcard.routing.UsedCardOnboardingRoute
import com.tangem.features.pushnotifications.api.PushNotificationsModelCallbacks
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class UsedCardOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val appRouter: AppRouter,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase,
    private val shouldShowAskBiometryUseCase: ShouldShowAskBiometryUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
) : Model() {

    private val params = paramsContainer.require<UsedCardOnboardingComponent.Params>()

    val alreadyActivatedCallbacks = AlreadyActivatedCallbacks()
    val askBiometryCallbacks = AskBiometryCallbacks()
    val pushNotificationsCallbacks = PushNotificationsCallbacks()
    val syncWalletCallbacks = SyncWalletCallbacks()

    val stackNavigation = StackNavigation<UsedCardOnboardingRoute>()
    val startRoute: UsedCardOnboardingRoute = UsedCardOnboardingRoute.AlreadyActivated
    val currentRoute: MutableStateFlow<UsedCardOnboardingRoute> = MutableStateFlow(startRoute)

    private var shouldShowBiometry: Boolean = false
    private var shouldShowPushNotifications: Boolean = false

    init {
        resolveAvailableSteps()
    }

    private fun resolveAvailableSteps() {
        modelScope.launch {
            val canBiometry = canUseBiometryUseCase() && shouldShowAskBiometryUseCase()
            val canPush = shouldAskPermissionUseCase(PUSH_PERMISSION)
            shouldShowBiometry = canBiometry
            shouldShowPushNotifications = canPush
        }
    }

    fun onBackClick() {
        when (currentRoute.value) {
            is UsedCardOnboardingRoute.AlreadyActivated -> router.pop()
            else -> Unit
        }
    }

    private fun proceedWithScanResponse(scanResponse: ScanResponse) {
        modelScope.launch {
            val userWallet = coldUserWalletBuilderFactory.create(scanResponse = scanResponse).build()

            if (userWallet == null) {
                Timber.e("User wallet not created")
                return@launch
            }

            saveWalletUseCase(userWallet = userWallet).fold(
                ifLeft = { error ->
                    when (error) {
                        is SaveWalletError.DataError -> {
                            Timber.e(error.toString(), "Unable to save user wallet")
                        }
                        is SaveWalletError.WalletAlreadySaved -> {
                            userWalletsListRepository.unlock(
                                userWalletId = userWallet.walletId,
                                unlockMethod = UserWalletsListRepository.UnlockMethod.Scan(scanResponse),
                            )
                        }
                    }
                },
                ifRight = {},
            )

            navigateAfterAlreadyActivated()
        }
    }

    private fun navigateAfterAlreadyActivated() {
        modelScope.launch {
            when {
                shouldShowBiometry -> {
                    stackNavigation.replaceAll(UsedCardOnboardingRoute.AskBiometry)
                }
                shouldShowPushNotifications -> {
                    stackNavigation.replaceAll(UsedCardOnboardingRoute.PushNotifications)
                }
                else -> {
                    stackNavigation.replaceAll(UsedCardOnboardingRoute.SyncWallet)
                }
            }
        }
    }

    private fun navigateAfterBiometry() {
        modelScope.launch {
            if (shouldShowPushNotifications) {
                stackNavigation.replaceAll(UsedCardOnboardingRoute.PushNotifications)
            } else {
                stackNavigation.replaceAll(UsedCardOnboardingRoute.SyncWallet)
            }
        }
    }

    private fun navigateToSyncWallet() {
        stackNavigation.replaceAll(UsedCardOnboardingRoute.SyncWallet)
    }

    inner class AlreadyActivatedCallbacks {
        fun onThisIsMyWalletClick() {
            proceedWithScanResponse(params.scanResponse)
        }

        fun onNewCardClick() {
            // TODO [REDACTED_TASK_KEY]
        }
    }

    inner class AskBiometryCallbacks : AskBiometryComponent.ModelCallbacks {
        override fun onAllowed() {
            navigateAfterBiometry()
        }

        override fun onDenied() {
            navigateAfterBiometry()
        }
    }

    inner class PushNotificationsCallbacks : PushNotificationsModelCallbacks {
        override fun onAllowSystemPermission() {
            navigateToSyncWallet()
        }

        override fun onDenySystemPermission() {
            navigateToSyncWallet()
        }

        override fun onDismiss() {
            navigateToSyncWallet()
        }
    }

    inner class SyncWalletCallbacks {
        fun onContinueClick() {
            appRouter.replaceAll(AppRoute.Wallet)
        }
    }
}