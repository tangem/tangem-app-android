package com.tangem.features.welcome.impl.model

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.bottomsheets.BottomSheetOption
import com.tangem.core.ui.components.bottomsheets.OptionsBottomSheetContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.core.wallets.error.UnlockWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.features.wallet.utils.UserWalletsFetcher
import com.tangem.features.welcome.impl.R
import com.tangem.features.welcome.impl.ui.state.WelcomeUM
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class WelcomeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val walletsRepository: WalletsRepository,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val urlOpener: UrlOpener,
    userWalletsFetcherFactory: UserWalletsFetcher.Factory,
) : Model() {

    val uiState: StateFlow<WelcomeUM>
        field = MutableStateFlow<WelcomeUM>(WelcomeUM.Plain)

    private val walletsFetcher = userWalletsFetcherFactory.create(
        messageSender = uiMessageSender,
        onlyMultiCurrency = false,
        isAuthMode = true,
        onWalletClick = { walletId ->
            modelScope.launch {
                val userWallets = userWalletsListRepository.userWalletsSync()
                val userWallet = userWallets.first { it.walletId == walletId }
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
                        it.handle(null, onUserCancelled = { tryToUnlockWithAccessCodeRightAway() })
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
            val userWallets = userWalletsListRepository.userWalletsSync()
            val userWallet = userWallets.first()
            uiState.value = WelcomeUM.Empty
            unlockWallet(userWallet.walletId, UserWalletsListRepository.UnlockMethod.AccessCode)
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
        updateSelectState { currentState ->
            currentState.copy(
                addWalletBottomSheet = TangemBottomSheetConfig(
                    isShown = true,
                    content = OptionsBottomSheetContent(
                        options = persistentListOf(
                            BottomSheetOption(
                                key = ADD_WALLET_KEY_CREATE,
                                label = resourceReference(R.string.home_button_create_new_wallet),
                            ),
                            BottomSheetOption(
                                key = ADD_WALLET_KEY_ADD,
                                label = resourceReference(R.string.home_button_add_existing_wallet),
                            ),
                            BottomSheetOption(
                                key = ADD_WALLET_KEY_BUY,
                                label = resourceReference(R.string.details_buy_wallet),
                            ),
                        ),
                        onOptionClick = { optionKey ->
                            updateSelectState {
                                it.copy(addWalletBottomSheet = it.addWalletBottomSheet.copy(isShown = false))
                            }
                            onAddWalletOptionClick(optionKey)
                        },
                    ),
                    onDismissRequest = {
                        updateSelectState {
                            it.copy(addWalletBottomSheet = it.addWalletBottomSheet.copy(isShown = false))
                        }
                    },
                ),
            )
        }
    }

    private fun onAddWalletOptionClick(optionKey: String) {
        when (optionKey) {
            ADD_WALLET_KEY_CREATE -> router.push(AppRoute.CreateWalletSelection)
            ADD_WALLET_KEY_ADD -> router.push(AppRoute.AddExistingWallet)
            ADD_WALLET_KEY_BUY -> modelScope.launch {
                generateBuyTangemCardLinkUseCase.invoke().let { urlOpener.openUrl(it) }
            }
        }
    }

    private suspend fun onlyOneHotWalletWithAccessCode(): Boolean {
        val userWalletsWithLock = userWalletsListRepository.userWalletsSync().filter { it.isLocked }
        if (userWalletsWithLock.size != 1) return false
        val wallet = userWalletsWithLock.first()
        return wallet is UserWallet.Hot && wallet.hotWalletId.authType != HotWalletId.AuthType.NoPassword
    }

    private fun onUserWalletClick(userWallet: UserWallet) = modelScope.launch {
        if (userWallet.isLocked.not()) {
            // If the wallet is not locked, we can proceed to the wallet screen directly
            userWalletsListRepository.select(userWallet.walletId)
            router.replaceAll(AppRoute.Wallet)
            return@launch
        }

        val unlockMethod = when (userWallet) {
            is UserWallet.Cold -> UserWalletsListRepository.UnlockMethod.Scan()
            is UserWallet.Hot -> {
                uiState.value = WelcomeUM.Empty
                UserWalletsListRepository.UnlockMethod.AccessCode
            }
        }

        unlockWallet(userWallet.walletId, unlockMethod)
        setSelectWalletState()
    }

    private suspend fun canUnlockWithBiometrics(): Boolean {
        return canUseBiometryUseCase() && walletsRepository.useBiometricAuthentication()
    }

    suspend fun unlockWallet(userWalletId: UserWalletId, unlockMethod: UserWalletsListRepository.UnlockMethod) {
        userWalletsListRepository.unlock(userWalletId, unlockMethod)
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
        when (this) {
            UnlockWalletError.AlreadyUnlocked -> {
                // this should not happen, as we check for locked state before this
                specificWalletId?.let { userWalletsListRepository.select(it) }
                router.replaceAll(AppRoute.Wallet)
            }
            UnlockWalletError.ScannedCardWalletNotMatched -> {
                // TODO Scanned card does not match the wallet
            }
            UnlockWalletError.UnableToUnlock -> {
                // TODO Unable to unlock the wallet"
            }
            UnlockWalletError.UserCancelled -> onUserCancelled()
            UnlockWalletError.UserWalletNotFound -> {
                // This should never happen in this flow, as we always check for the wallet existence before unlocking
                Timber.e("User wallet not found for unlock: $specificWalletId")
                uiMessageSender.send(
                    SnackbarMessage(TextReference.Res(R.string.generic_error)),
                )
            }
        }
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

    companion object {
        private const val ADD_WALLET_KEY_CREATE = "create"
        private const val ADD_WALLET_KEY_ADD = "add"
        private const val ADD_WALLET_KEY_BUY = "buy"
    }
}