package com.tangem.features.welcome.impl.model

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.userwallet.handle
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.event.SignIn
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.UnlockWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.NonBiometricUnlockWalletUseCase
import com.tangem.features.wallet.utils.UserWalletsFetcher
import com.tangem.features.welcome.impl.ui.state.WelcomeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class WelcomeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val nonBiometricUnlockWalletUseCase: NonBiometricUnlockWalletUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val walletsRepository: WalletsRepository,
    private val trackingContextProxy: TrackingContextProxy,
    private val analyticsEventHandler: AnalyticsEventHandler,
    userWalletsFetcherFactory: UserWalletsFetcher.Factory,
) : Model() {

    val uiState: StateFlow<WelcomeUM>
        field = MutableStateFlow<WelcomeUM>(WelcomeUM.Plain)

    private val walletsFetcher = userWalletsFetcherFactory.create(
        messageSender = uiMessageSender,
        onlyMultiCurrency = false,
        isAuthMode = true,
        isClickableIfLocked = true,
        onWalletClick = { walletId ->
            modelScope.launch {
                val userWallets = userWalletsListRepository.userWalletsSync()
                val userWallet = userWallets.first { it.walletId == walletId }
                trackingContextProxy.proceedWithContext(userWallet) {
                    val signInType = when {
                        !userWallet.isLocked -> SignIn.ButtonWallet.SignInType.NoSecurity
                        userWallet is UserWallet.Cold -> SignIn.ButtonWallet.SignInType.Card
                        else -> SignIn.ButtonWallet.SignInType.AccessCode
                    }
                    analyticsEventHandler.send(SignIn.ButtonWallet(signInType))
                }
                onUserWalletClick(userWallet)
            }
        },
    )
    private val walletsFetcherJobHolder = JobHolder()
    private val wallets = MutableStateFlow<ImmutableList<UserWalletItemUM>>(persistentListOf())
    private var routedOut = false

    init {
        modelScope.launch {
            userWalletsListRepository.load()
            wallets.value = walletsFetcher.userWallets.first()

            analyticsEventHandler.send(SignIn.ScreenOpened(wallets.value.size))

            launch {
                walletsFetcher.userWallets
                    .collectLatest {
                        if (it.isEmpty()) {
                            router.replaceAll(AppRoute.Home())
                        }

                        wallets.value = it
                    }
            }

            tryToUnlockRightAway()
        }
    }

    private fun tryToUnlockRightAway() {
        modelScope.launch {
            if (canUnlockWithBiometrics()) {
                userWalletsListRepository.unlockAllWallets()
                    .onRight {
                        routedOut = true
                        router.replaceAll(AppRoute.Wallet)
                    }
                    .onLeft {
                        it.handle(
                            specificWalletId = null,
                            onUserCancelled = { tryToUnlockWithAccessCodeRightAway() },
                        )
                        setSelectWalletState()
                    }
            } else {
                tryToUnlockWithAccessCodeRightAway()
                setSelectWalletState()
            }
        }
    }

    private suspend fun tryToUnlockWithAccessCodeRightAway() {
        if (onlyOneHotWalletWithAccessCode()) {
            val hotWalletLockedWithAccessCode = userWalletsListRepository.userWalletsSync()
                .first { it.isLocked && it is UserWallet.Hot } as UserWallet.Hot
            uiState.value = WelcomeUM.Empty
            nonBiometricUnlockWallet(hotWalletLockedWithAccessCode.walletId)
        }
    }

    private fun setSelectWalletState() {
        modelScope.launch {
            if (routedOut || uiState.value is WelcomeUM.SelectWallet) return@launch

            uiState.value = WelcomeUM.SelectWallet(
                wallets = walletsFetcher.userWallets.first(),
                showUnlockWithBiometricButton = canUnlockWithBiometrics(),
                addWalletClick = ::addWalletClick,
                onUnlockWithBiometricClick = {
                    analyticsEventHandler.send(SignIn.ButtonUnlockAllWithBiometric())
                    modelScope.launch {
                        userWalletsListRepository.unlockAllWallets()
                            .onRight {
                                router.replaceAll(AppRoute.Wallet)
                            }
                            .onLeft {
                                it.handle(null, onUserCancelled = { /* ignore */ })
                            }
                    }
                },
            )

            wallets.collectLatest { wallets ->
                updateSelectState {
                    it.copy(wallets = wallets)
                }
            }
        }.saveIn(walletsFetcherJobHolder)
    }

    private fun addWalletClick() {
        analyticsEventHandler.send(SignIn.ButtonAddWallet(AnalyticsParam.ScreensSources.SignIn))
        router.push(AppRoute.CreateWalletSelection)
    }

    private suspend fun onlyOneHotWalletWithAccessCode(): Boolean {
        val userWallets = userWalletsListRepository.userWalletsSync()
        if (userWallets.size != 1) return false
        val wallet = userWallets.first()
        return wallet is UserWallet.Hot && wallet.isLocked
    }

    private fun onUserWalletClick(userWallet: UserWallet) = modelScope.launch {
        if (userWallet.isLocked.not()) {
            // If the wallet is not locked, we can proceed to the wallet screen directly
            userWalletsListRepository.select(userWallet.walletId)
            trackSignInEvent(userWallet, Basic.SignedIn.SignInType.NoSecurity)
            router.replaceAll(AppRoute.Wallet)
            return@launch
        }

        if (userWallet is UserWallet.Hot) {
            uiState.value = WelcomeUM.Empty
        }

        nonBiometricUnlockWallet(userWallet.walletId)
        setSelectWalletState()
    }

    private suspend fun canUnlockWithBiometrics(): Boolean {
        return canUseBiometryUseCase() && walletsRepository.useBiometricAuthentication()
    }

    suspend fun nonBiometricUnlockWallet(userWalletId: UserWalletId) {
        nonBiometricUnlockWalletUseCase(userWalletId)
            .onRight {
                routedOut = true
                userWalletsListRepository.select(userWalletId)
                router.replaceAll(AppRoute.Wallet)
            }
            .onLeft { error ->
                error.handle(specificWalletId = userWalletId, onUserCancelled = { /* ignore*/ })
            }
    }

    suspend fun UnlockWalletError.handle(specificWalletId: UserWalletId?, onUserCancelled: suspend () -> Unit = { }) {
        handle(
            onAlreadyUnlocked = {
                // this should not happen, as we check for locked state before this
                specificWalletId?.let { userWalletsListRepository.select(it) }
                router.replaceAll(AppRoute.Wallet)
            },
            onUserCancelled = { onUserCancelled() },
            showMessage = uiMessageSender::send,
        )
    }

    private fun updateSelectState(block: (WelcomeUM.SelectWallet) -> WelcomeUM.SelectWallet) {
        uiState.update { currentState ->
            if (currentState is WelcomeUM.SelectWallet) {
                block(currentState)
            } else {
                currentState
            }
        }
    }

    private suspend fun trackSignInEvent(userWallet: UserWallet, type: Basic.SignedIn.SignInType) {
        val walletsCount = userWalletsListRepository.userWalletsSync().size
        trackingContextProxy.proceedWithContext(userWallet) {
            analyticsEventHandler.send(
                event = Basic.SignedIn(
                    signInType = type,
                    walletsCount = walletsCount,
                ),
            )
        }
    }
}