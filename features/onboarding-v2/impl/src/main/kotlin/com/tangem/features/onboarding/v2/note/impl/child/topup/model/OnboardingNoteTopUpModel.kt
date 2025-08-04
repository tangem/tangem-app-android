package com.tangem.features.onboarding.v2.note.impl.child.topup.model

import com.tangem.common.ui.bottomsheet.receive.TokenReceiveBottomSheetConfig
import com.tangem.core.analytics.Analytics
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.GetLegacyTopUpUrlUseCase
import com.tangem.domain.tokens.FetchCurrencyStatusUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.analytics.TokenReceiveAnalyticsEvent
import com.tangem.domain.tokens.wallet.WalletBalanceFetcher
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.onboarding.v2.common.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.note.impl.child.topup.OnboardingNoteTopUpComponent
import com.tangem.features.onboarding.v2.note.impl.child.topup.ui.state.OnboardingNoteTopUpUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.isPositive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class OnboardingNoteTopUpModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val getLegacyTopUpUrlUseCase: GetLegacyTopUpUrlUseCase,
    private val urlOpener: UrlOpener,
    private val clipboardManager: ClipboardManager,
    private val shareManager: ShareManager,
    private val rampStateManager: RampStateManager,
    private val cardRepository: CardRepository,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val walletBalanceFetcher: WalletBalanceFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) : Model() {

    private val params = paramsContainer.require<OnboardingNoteTopUpComponent.Params>()
    private val commonState = params.childParams.commonState
    private val scanResponse = params.childParams.commonState.value.scanResponse
    private var userWallet = params.childParams.commonState.value.userWallet

    private val _uiState = MutableStateFlow(
        OnboardingNoteTopUpUM(
            onRefreshBalanceClick = ::refreshBalance,
            onBuyCryptoClick = ::onBuyCryptoClick,
            onShowWalletAddressClick = ::onShowWalletAddressClick,
            onDismissBottomSheet = ::onDismissBottomSheet,
        ),
    )

    val uiState: StateFlow<OnboardingNoteTopUpUM> = _uiState

    init {
        Analytics.send(OnboardingEvent.Topup.ScreenOpened)
        observeArtwork()
        modelScope.launch {
            createUserWalletIfNull()
            cardRepository.finishCardActivation(scanResponse.card.cardId)
            observeCryptoCurrencyStatus()
            refreshBalance()
        }
    }

    private fun refreshBalance() {
        modelScope.launch {
            showBalanceLoadingProgress(true)
            createUserWalletIfNull()
            val userWalletId = requireNotNull(userWallet?.walletId)
            if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
                walletBalanceFetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))
                    .onLeft(Timber::e)
            } else {
                fetchCurrencyStatusUseCase(userWalletId = userWalletId, refresh = true)
            }
            showBalanceLoadingProgress(false)
        }
    }

    private fun onBuyCryptoClick() {
        val cryptoCurrencyStatus = params.childParams.commonState.value.cryptoCurrencyStatus ?: return
        modelScope.launch {
            getLegacyTopUpUrlUseCase(cryptoCurrencyStatus).onRight {
                urlOpener.openUrl(it)
            }
        }
        Analytics.send(OnboardingEvent.Topup.ButtonBuyCrypto(cryptoCurrencyStatus.currency))
    }

    private fun onShowWalletAddressClick() {
        val currencyStatus = params.childParams.commonState.value.cryptoCurrencyStatus ?: return
        val networkAddress = currencyStatus.value.networkAddress ?: return

        _uiState.update {
            it.copy(addressBottomSheetConfig = createReceiveBS(currencyStatus, networkAddress))
        }
        Analytics.send(OnboardingEvent.Topup.ButtonShowWalletAddress)
    }

    private fun onDismissBottomSheet() {
        _uiState.update {
            it.copy(addressBottomSheetConfig = null)
        }
    }

    private suspend fun createUserWalletIfNull() {
        if (userWallet != null) {
            return
        }
        val commonState = params.childParams.commonState.value
        userWallet = commonState.userWallet ?: createAndSaveUserWallet(scanResponse)
    }

    private fun observeArtwork() {
        modelScope.launch {
            params.childParams.commonState.collect {
                _uiState.value = _uiState.value.copy(
                    cardArtwork = it.cardArtwork,
                )
            }
        }
    }

    private fun observeCryptoCurrencyStatus() {
        val userWalletId = userWallet?.walletId ?: return
        getSingleCryptoCurrencyStatusUseCase.invokeSingleWallet(userWalletId = userWalletId)
            .map { it.getOrNull() }
            .filterNotNull()
            .onEach(::applyCryptoCurrencyStatusToState)
            .launchIn(modelScope)
    }

    private fun applyCryptoCurrencyStatusToState(status: CryptoCurrencyStatus) {
        if (commonState.value.cryptoCurrencyStatus == null) {
            loadAvailableForBuy(status)
        }

        commonState.update {
            it.copy(cryptoCurrencyStatus = status)
        }

        val amount = when (status.value) {
            is CryptoCurrencyStatus.Loaded -> status.value.amount
            is CryptoCurrencyStatus.NoAccount -> status.value.amount
            is CryptoCurrencyStatus.NoQuote -> status.value.amount
            else -> null
        }
        val hasCurrentNetworkTransactions = when (status.value) {
            is CryptoCurrencyStatus.Loaded -> status.value.hasCurrentNetworkTransactions
            is CryptoCurrencyStatus.NoAccount -> status.value.hasCurrentNetworkTransactions
            else -> false
        }
        val amountToCreateAccount = (status.value as? CryptoCurrencyStatus.NoAccount)?.amountToCreateAccount

        if (amount?.isPositive() == true || hasCurrentNetworkTransactions) {
            params.onDone()
        }

        _uiState.update {
            it.copy(
                amountToCreateAccount = amountToCreateAccount
                    ?.format {
                        crypto(
                            symbol = status.currency.symbol,
                            decimals = status.currency.decimals,
                        )
                    },
                balance = amount?.format {
                    crypto(
                        symbol = status.currency.symbol,
                        decimals = status.currency.decimals,
                    )
                }.orEmpty(),
                isTopUpDataLoading = status.value.networkAddress == null,
            )
        }
    }

    private fun showBalanceLoadingProgress(value: Boolean) {
        _uiState.update {
            it.copy(isRefreshing = value)
        }
    }

    private fun loadAvailableForBuy(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        modelScope.launch {
            val availableForBuy = rampStateManager.availableForBuy(
                userWallet = userWallet ?: return@launch,
                cryptoCurrency = cryptoCurrencyStatus.currency,
            )
            _uiState.update {
                it.copy(
                    availableForBuy = availableForBuy == ScenarioUnavailabilityReason.None,
                    availableForBuyLoading = false,
                )
            }
        }
    }

    private fun createReceiveBS(currencyStatus: CryptoCurrencyStatus, networkAddress: NetworkAddress) =
        TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = uiState.value.onDismissBottomSheet,
            content = TokenReceiveBottomSheetConfig(
                asset = TokenReceiveBottomSheetConfig.Asset.Currency(
                    name = currencyStatus.currency.name,
                    symbol = currencyStatus.currency.symbol,
                ),
                network = currencyStatus.currency.network,
                networkAddress = networkAddress,
                showMemoDisclaimer =
                currencyStatus.currency.network.transactionExtrasType != Network.TransactionExtrasType.NONE,
                onCopyClick = {
                    Analytics.send(TokenReceiveAnalyticsEvent.ButtonCopyAddress(currencyStatus.currency.symbol))
                    clipboardManager.setText(text = it, isSensitive = true)
                },
                onShareClick = {
                    Analytics.send(TokenReceiveAnalyticsEvent.ButtonShareAddress(currencyStatus.currency.symbol))
                    shareManager.shareText(text = it)
                },
            ),
        )

    private suspend fun createAndSaveUserWallet(scanResponse: ScanResponse): UserWallet {
        val wallet = requireNotNull(
            value = coldUserWalletBuilderFactory.create(scanResponse = scanResponse).build(),
            lazyMessage = { "User wallet not created" },
        )
        saveWalletUseCase(wallet, false)
        return wallet
    }
}