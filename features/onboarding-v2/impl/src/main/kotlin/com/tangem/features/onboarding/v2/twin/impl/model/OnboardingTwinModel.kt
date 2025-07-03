package com.tangem.features.onboarding.v2.twin.impl.model

import com.tangem.Message
import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.ui.bottomsheet.receive.TokenReceiveBottomSheetConfig
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.toWrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.datasource.local.config.issuers.IssuersConfigStorage
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.common.TwinCardNumber
import com.tangem.domain.common.getTwinCardNumber
import com.tangem.domain.common.util.twinsIsTwinned
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.onboarding.SaveTwinsOnboardingShownUseCase
import com.tangem.domain.onramp.GetLegacyTopUpUrlUseCase
import com.tangem.domain.tokens.FetchCurrencyStatusUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.analytics.TokenReceiveAnalyticsEvent
import com.tangem.domain.tokens.wallet.WalletBalanceFetcher
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.onboarding.v2.common.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.common.ui.interruptBackupDialog
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent.Params.Mode
import com.tangem.features.onboarding.v2.twin.impl.DefaultOnboardingTwinComponent
import com.tangem.features.onboarding.v2.twin.impl.ui.TwinWalletArtworkUM
import com.tangem.features.onboarding.v2.twin.impl.ui.state.OnboardingTwinUM
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.sdk.extensions.localizedDescriptionRes
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class OnboardingTwinModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val userWalletsListManager: UserWalletsListManager,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val saveTwinsOnboardingShownUseCase: SaveTwinsOnboardingShownUseCase,
    private val tangemSdkManager: TangemSdkManager,
    private val issuersConfigStorage: IssuersConfigStorage,
    private val cardRepository: CardRepository,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val getLegacyTopUpUrlUseCase: GetLegacyTopUpUrlUseCase,
    private val urlOpener: UrlOpener,
    private val uiMessageSender: UiMessageSender,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val clipboardManager: ClipboardManager,
    private val shareManager: ShareManager,
    private val tokensFeatureToggles: TokensFeatureToggles,
    private val walletBalanceFetcher: WalletBalanceFetcher,
) : Model() {

    private val params = paramsContainer.require<OnboardingTwinComponent.Params>()
    private val firstCardTwinNumber = params.scanResponse.card.getTwinCardNumber() ?: error("Not twin")
    private val cryptoCurrencyStatusJobHolder = JobHolder()

    private val _uiState = MutableStateFlow(
        when (params.mode) {
            Mode.WelcomeOnly -> {
                OnboardingTwinUM.Welcome(
                    pairCardNumber = firstCardTwinNumber.pairNumber().number,
                    onContinueClick = ::saveWalletAndDone,
                )
            }
            Mode.RecreateWallet -> {
                OnboardingTwinUM.ResetWarning(
                    onAcceptClick = { toggle ->
                        update<OnboardingTwinUM.ResetWarning> { it.copy(acceptToggle = toggle) }
                    },
                    onContinueClick = {
                        navigateToFirstScan()
                    },
                )
            }
            Mode.CreateWallet -> {
                if (params.scanResponse.twinsIsTwinned()) {
                    OnboardingTwinUM.TopUpPrepare
                } else {
                    OnboardingTwinUM.Welcome(
                        pairCardNumber = firstCardTwinNumber.pairNumber().number,
                        onContinueClick = ::navigateToFirstScan,
                    )
                }
            }
        },
    )

    val uiState = _uiState.asStateFlow()
    val innerNavigationState = MutableStateFlow(DefaultOnboardingTwinComponent.TwinInnerNavigationState(1))

    init {
        when (_uiState.value) {
            is OnboardingTwinUM.Welcome -> {
                analyticsEventHandler.send(OnboardingEvent.Twins.ScreenOpened)
                modelScope.launch {
                    saveTwinsOnboardingShownUseCase()
                }
            }
            OnboardingTwinUM.TopUpPrepare -> {
                modelScope.launch {
                    setTopUpState(params.scanResponse)
                }
            }
            else -> {}
        }
    }

    fun onBack() {
        val state = _uiState.value
        val twinningInProgress = state is OnboardingTwinUM.ScanCard &&
            (state.step == OnboardingTwinUM.ScanCard.Step.Second || state.step == OnboardingTwinUM.ScanCard.Step.Third)

        if (twinningInProgress) {
            uiMessageSender.send(TwinProcessNotCompleted)
        } else {
            uiMessageSender.send(
                interruptBackupDialog(
                    onConfirm = { params.modelCallbacks.onBack() },
                ),
            )
        }
    }

    private fun navigateToFirstScan() {
        _uiState.value = OnboardingTwinUM.ScanCard(
            artworkStep = when (firstCardTwinNumber) {
                TwinCardNumber.First -> TwinWalletArtworkUM.Leapfrog.Step.FirstCard
                TwinCardNumber.Second -> TwinWalletArtworkUM.Leapfrog.Step.SecondCard
            },
            step = OnboardingTwinUM.ScanCard.Step.First,
            onScanClick = { createFirstWallet() },
        )
    }

    private fun createFirstWallet() {
        modelScope.launch {
            setLoading(true)

            val result = tangemSdkManager.createFirstTwinWallet(
                cardId = params.scanResponse.card.cardId,
                initialMessage = Message(
                    tangemSdkManager.getString(
                        R.string.twins_recreate_title_format,
                        firstCardTwinNumber.number,
                    ),
                ),
            )

            when (result) {
                is CompletionResult.Failure -> {
                    setLoading(false)
                    showCardVerificationFailedDialog(result.error)
                }
                is CompletionResult.Success -> {
                    if (params.mode == Mode.CreateWallet) {
                        cardRepository.startCardActivation(result.data.cardId)
                    }

                    // remove wallet only after first step of retwin
                    if (params.mode == Mode.RecreateWallet) {
                        userWalletsListManager.delete(
                            listOfNotNull(UserWalletIdBuilder.scanResponse(params.scanResponse).build()),
                        )
                    }

                    analyticsEventHandler.send(OnboardingEvent.CreateWallet.WalletCreatedSuccessfully())

                    update<OnboardingTwinUM.ScanCard> {
                        it.copy(
                            isLoading = false,
                            artworkStep = it.artworkStep.next(),
                            step = OnboardingTwinUM.ScanCard.Step.Second,
                            onScanClick = {
                                createSecondWallet(firstPublicKey = result.data.wallet.publicKey.toHexString())
                            },
                        )
                    }

                    innerNavigationState.update {
                        it.copy(stackSize = 2)
                    }
                }
            }
        }
    }

    private fun createSecondWallet(firstPublicKey: String) {
        setLoading(true)

        modelScope.launch {
            val secondCardNumber = firstCardTwinNumber.pairNumber().number
            val result = tangemSdkManager.createSecondTwinWallet(
                firstPublicKey = firstPublicKey,
                firstCardId = params.scanResponse.card.cardId,
                issuerKeys = getIssuerKeys(),
                preparingMessage = Message(
                    tangemSdkManager.getString(R.string.twins_recreate_title_preparing),
                ),
                creatingWalletMessage = Message(
                    tangemSdkManager.getString(R.string.twins_recreate_title_creating_wallet),
                ),
                initialMessage = Message(
                    tangemSdkManager.getString(
                        R.string.twins_recreate_title_format,
                        secondCardNumber,
                    ),
                ),
            )

            when (result) {
                is CompletionResult.Failure -> {
                    setLoading(false)
                    showCardVerificationFailedDialog(result.error)
                }
                is CompletionResult.Success -> {
                    if (params.mode == Mode.CreateWallet) {
                        cardRepository.startCardActivation(result.data.cardId)
                    }

                    update<OnboardingTwinUM.ScanCard> {
                        it.copy(
                            isLoading = false,
                            artworkStep = it.artworkStep.next(),
                            step = OnboardingTwinUM.ScanCard.Step.Third,
                            onScanClick = {
                                createThirdWallet(
                                    secondCardPublicKey = result.data.wallet.publicKey,
                                )
                            },
                        )
                    }

                    innerNavigationState.update {
                        it.copy(stackSize = 3)
                    }
                }
            }
        }
    }

    private fun createThirdWallet(secondCardPublicKey: ByteArray) {
        modelScope.launch {
            setLoading(true)

            val result = tangemSdkManager.finalizeTwin(
                secondCardPublicKey = secondCardPublicKey,
                issuerKeyPair = getIssuerKeys(),
                cardId = params.scanResponse.card.cardId,
                initialMessage = Message(
                    tangemSdkManager.getString(
                        R.string.twins_recreate_title_format,
                        firstCardTwinNumber.number,
                    ),
                ),
            )

            when (result) {
                is CompletionResult.Failure -> {
                    setLoading(false)
                    showCardVerificationFailedDialog(result.error)
                }
                is CompletionResult.Success -> {
                    scanComplete(result.data)
                }
            }
        }
    }

    private fun scanComplete(scanResponse: ScanResponse) {
        analyticsEventHandler.send(OnboardingEvent.Twins.SetupFinished)

        when (params.mode) {
            Mode.WelcomeOnly -> saveWalletAndDone()
            Mode.RecreateWallet -> params.modelCallbacks.onDone()
            Mode.CreateWallet -> {
                modelScope.launch {
                    setLoading(true)
                    setTopUpState(scanResponse)
                }.saveIn(cryptoCurrencyStatusJobHolder)
            }
        }
    }

    private suspend fun setTopUpState(scanResponse: ScanResponse) = coroutineScope {
        val userWallet = coldUserWalletBuilderFactory.create(scanResponse).build() ?: run {
            Timber.e("User wallet not created")
            setLoading(false)
            return@coroutineScope
        }

        userWalletsListManager.save(userWallet, canOverride = true)

        cardRepository.finishCardActivation(params.scanResponse.card.cardId)

        if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
            walletBalanceFetcher(params = WalletBalanceFetcher.Params(userWalletId = userWallet.walletId))
        } else {
            fetchCurrencyStatusUseCase.invoke(userWalletId = userWallet.walletId, refresh = true)
        }
            .onLeft {
                Timber.e("Unable to fetch currency status: $it")
                setLoading(false)
            }

        val cryptoCurrencyStatus = getSingleCryptoCurrencyStatusUseCase.invokeSingleWallet(userWallet.walletId)
            .firstOrNull()?.getOrNull()
            ?: run {
                setLoading(false)
                Timber.e("Unable to get currency status")
                return@coroutineScope
            }

        launch {
            getSingleCryptoCurrencyStatusUseCase.invokeSingleWallet(userWallet.walletId)
                .collect {
                    it.onRight { status ->
                        applyCryptoCurrencyStatusToState(status)
                    }
                }
        }

        _uiState.value = OnboardingTwinUM.TopUp(
            onBuyCryptoClick = { onBuyCryptoClick(cryptoCurrencyStatus) },
            onRefreshClick = { onRefreshBalanceClick(userWallet) },
            onShowAddressClick = { onShowAddressClick(cryptoCurrencyStatus) },
            isLoading = true,
        )

        innerNavigationState.update {
            it.copy(stackSize = 4)
        }
    }

    private fun applyCryptoCurrencyStatusToState(status: CryptoCurrencyStatus) {
        val amount = (status.value as? CryptoCurrencyStatus.Loaded)?.amount ?: return
        if (amount > BigDecimal.ZERO) {
            params.modelCallbacks.onDone()
        } else {
            update<OnboardingTwinUM.TopUp> {
                it.copy(
                    balance = BigDecimal.ZERO.format { crypto(status.currency) },
                    onBuyCryptoClick = { onBuyCryptoClick(status) },
                    onShowAddressClick = { onShowAddressClick(status) },
                    isLoading = false,
                )
            }
        }
    }

    private fun onBuyCryptoClick(status: CryptoCurrencyStatus) {
        modelScope.launch {
            getLegacyTopUpUrlUseCase(status).onRight {
                urlOpener.openUrl(it)
            }
        }
    }

    private fun onShowAddressClick(status: CryptoCurrencyStatus) {
        val currency = status.currency
        val networkAddress = status.value.networkAddress ?: return

        update<OnboardingTwinUM.TopUp> {
            it.copy(
                bottomSheetConfig = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {
                        update<OnboardingTwinUM.TopUp> {
                            it.copy(bottomSheetConfig = TangemBottomSheetConfig.Empty)
                        }
                    },
                    content = TokenReceiveBottomSheetConfig(
                        asset = TokenReceiveBottomSheetConfig.Asset.Currency(
                            name = currency.name,
                            symbol = currency.symbol,
                        ),
                        network = currency.network,
                        networkAddress = networkAddress,
                        showMemoDisclaimer =
                        currency.network.transactionExtrasType != Network.TransactionExtrasType.NONE,
                        onCopyClick = {
                            Analytics.send(TokenReceiveAnalyticsEvent.ButtonCopyAddress(currency.symbol))
                            clipboardManager.setText(text = it, isSensitive = true)
                        },
                        onShareClick = {
                            Analytics.send(TokenReceiveAnalyticsEvent.ButtonShareAddress(currency.symbol))
                            shareManager.shareText(text = it)
                        },
                    ),
                ),
            )
        }
    }

    private fun onRefreshBalanceClick(userWallet: UserWallet) {
        update<OnboardingTwinUM.TopUp> {
            it.copy(isLoading = true)
        }
        modelScope.launch {
            if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
                walletBalanceFetcher(params = WalletBalanceFetcher.Params(userWalletId = userWallet.walletId))
                    .onLeft(Timber::e)
            } else {
                fetchCurrencyStatusUseCase(userWalletId = userWallet.walletId, refresh = true)
            }
        }
    }

    private fun saveWalletAndDone() {
        setLoading(true)

        modelScope.launch {
            val userWallet = coldUserWalletBuilderFactory.create(params.scanResponse).build() ?: run {
                Timber.e("User wallet not created")
                setLoading(false)
                return@launch
            }

            userWalletsListManager.save(userWallet, canOverride = true)
            params.modelCallbacks.onDone()
        }
    }

    private fun showCardVerificationFailedDialog(error: TangemError) {
        if (error !is TangemSdkError.CardVerificationFailed) return

        analyticsEventHandler.send(OnboardingEvent.OfflineAttestationFailed(AnalyticsParam.ScreensSources.Onboarding))

        val resource = error.localizedDescriptionRes()
        val resId = resource.resId ?: R.string.common_unknown_error
        val resArgs = resource.args.map { it.value }

        uiMessageSender.send(
            cardVerificationFailedDialog(
                errorDescription = resourceReference(id = resId, resArgs.toWrappedList()),
                onRequestSupport = {
                    modelScope.launch {
                        sendFeedbackEmailUseCase(type = FeedbackEmailType.CardAttestationFailed)
                    }
                },
            ),
        )
    }

    private suspend fun getIssuerKeys(): KeyPair {
        val issuer = issuersConfigStorage.getConfig()
            .first { it.publicKey == params.scanResponse.card.issuer.publicKey.toHexString() }

        return KeyPair(
            publicKey = issuer.publicKey.hexToBytes(),
            privateKey = issuer.privateKey.hexToBytes(),
        )
    }

    private inline fun <reified T : OnboardingTwinUM> update(updateBlock: (T) -> OnboardingTwinUM) {
        _uiState.update { st ->
            (st as? T)?.let { updateBlock(it) } ?: st
        }
    }

    private fun setLoading(flag: Boolean) {
        _uiState.update { it.copySealed(isLoading = flag) }
    }

    private fun TwinWalletArtworkUM.Leapfrog.Step.next(): TwinWalletArtworkUM.Leapfrog.Step = when (this) {
        TwinWalletArtworkUM.Leapfrog.Step.FirstCard -> TwinWalletArtworkUM.Leapfrog.Step.SecondCard
        TwinWalletArtworkUM.Leapfrog.Step.SecondCard -> TwinWalletArtworkUM.Leapfrog.Step.FirstCard
    }
}