package com.tangem.feature.referral.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrency
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetAccountCurrencyByAddressUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.referral.analytics.ReferralEvents
import com.tangem.feature.referral.analytics.ReferralEventsAccounts
import com.tangem.feature.referral.api.ReferralComponent
import com.tangem.feature.referral.domain.ReferralInteractor
import com.tangem.feature.referral.domain.errors.ReferralError
import com.tangem.feature.referral.domain.models.DiscountType
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.ReferralInfo
import com.tangem.feature.referral.models.DemoModeException
import com.tangem.feature.referral.models.ReferralStateHolder
import com.tangem.feature.referral.models.ReferralStateHolder.*
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioSelectorComponent
import com.tangem.features.account.PortfolioSelectorController
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class ReferralModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val referralInteractor: ReferralInteractor,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val shareManager: ShareManager,
    private val urlOpener: UrlOpener,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val portfolioFetcherFactory: PortfolioFetcher.Factory,
    val portfolioSelectorController: PortfolioSelectorController,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getAccountCurrencyByAddressUseCase: GetAccountCurrencyByAddressUseCase,
    private val appRouter: AppRouter,
) : Model() {

    private val params = paramsContainer.require<ReferralComponent.Params>()
    val bottomSheetNavigation: SlotNavigation<Unit> = SlotNavigation()
    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.Wallet(params.userWalletId),
            scope = modelScope,
        )
    }

    private var referralData: MutableStateFlow<ReferralData?> = MutableStateFlow(null)
    private val lastReferralData get() = referralData.value

    var uiState: ReferralStateHolder by mutableStateOf(createInitiallyUiState())
        private set

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { bottomSheetNavigation.dismiss() }
        override val onBack: () -> Unit = { bottomSheetNavigation.dismiss() }
    }

    init {
        analyticsEventHandler.send(ReferralEvents.ReferralScreenOpened())
        if (accountsFeatureToggles.isFeatureEnabled) {
            combine(
                flow = referralData.filterNotNull().onEach(::selectAccount),
                flow2 = portfolioSelectorController.isAccountMode,
                transform = { referralData, isAccountMode -> referralData to isAccountMode },
            ).transformLatest { (referralData, isAccountMode) ->
                when (isAccountMode) {
                    false -> showContent(referralData)
                    true -> combineAccountUI(referralData)
                        .map { referralData to it }
                        .collect(::emit)
                }
            }
                .onEach { (referralData, accountAward) -> showContent(referralData, accountAward) }
                .launchIn(modelScope)
        } else {
            referralData.filterNotNull()
                .onEach(::showContent)
                .launchIn(modelScope)
        }
        loadReferralData()
    }

    private fun combineAccountUI(referralData: ReferralData): Flow<AccountAward?> = combine(
        flow = portfolioSelectorController.selectedAccountWithData(portfolioFetcher)
            .onEach { analyticsEventHandler.send(ReferralEventsAccounts.ListChooseAccount()) }
            .onEach { bottomSheetNavigation.dismiss() },
        flow2 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
        flow3 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
        flow4 = portfolioFetcher.data,
    ) { pair, isBalanceHidden, appCurrency, portfolios ->
        val selectedAccount = pair?.second ?: return@combine null

        val cryptoPortfolio = when (selectedAccount) {
            is AccountStatus.CryptoPortfolio -> selectedAccount
        }

        val awardCryptoCurrency = referralInteractor.getCryptoCurrency(
            userWalletId = params.userWalletId,
            tokenData = referralData.getToken(),
            accountIndex = cryptoPortfolio.account.derivationIndex,
        ) ?: return@combine null

        val accountAwardToken = cryptoPortfolio.tokenList.flattenCurrencies()
            .find { it.currency.id == awardCryptoCurrency.id }

        return@combine AccountAwardConverter(
            isBalanceHidden = isBalanceHidden,
            awardCryptoCurrency = awardCryptoCurrency,
            accountAwardToken = accountAwardToken,
            cryptoPortfolio = cryptoPortfolio,
            appCurrency = appCurrency,
            isSingleAccount = portfolios.isSingleChoice,
            onAccountClick = { bottomSheetNavigation.activate(Unit) },
        ).convert(Unit)
    }

    private fun createInitiallyUiState() = ReferralStateHolder(
        headerState = HeaderState(
            onBackClicked = appRouter::pop,
        ),
        referralInfoState = ReferralInfoState.Loading,
        errorSnackbar = null,
        analytics = Analytics(
            onAgreementClicked = ::onAgreementClicked,
            onCopyClicked = ::onCopyClicked,
            onShareClicked = ::onShareClicked,
        ),
    )

    private fun loadReferralData() {
        uiState = uiState.copy(referralInfoState = ReferralInfoState.Loading)
        modelScope.launch {
            runCatching {
                referralInteractor.getReferralStatus(params.userWalletId).apply {
                    referralData.value = this
                }
            }
                .onFailure(::showErrorSnackbar)
        }
    }

    private fun participate() {
        val userWallet = getUserWalletUseCase(params.userWalletId).getOrNull() ?: error("User wallet not found")

        if (userWallet is UserWallet.Cold && isDemoCardUseCase(cardId = userWallet.cardId)) {
            showErrorSnackbar(DemoModeException())
        } else {
            analyticsEventHandler.send(ReferralEvents.ClickParticipate())
            val lastInfoState = uiState.referralInfoState
            uiState = uiState.copy(referralInfoState = ReferralInfoState.Loading)
            modelScope.launch {
                val portfolioId = when (accountsFeatureToggles.isFeatureEnabled) {
                    true -> PortfolioId(requireNotNull(portfolioSelectorController.selectedAccountSync))
                    false -> PortfolioId(params.userWalletId)
                }
                runCatching { referralInteractor.startReferral(portfolioId) }
                    .onSuccess { referral ->
                        analyticsEventHandler.send(ReferralEvents.ParticipateSuccessful())
                        referralData.value = referral
                    }
                    .onFailure { throwable ->
                        if (throwable is ReferralError.UserCancelledException) {
                            uiState = uiState.copy(referralInfoState = lastInfoState)
                        } else {
                            showErrorSnackbar(throwable)
                        }
                    }
            }
        }
    }

    private fun showContent(referralData: ReferralData, accountAward: AccountAward? = null) {
        uiState = uiState.copy(referralInfoState = referralData.convertToReferralInfoState(accountAward))
    }

    private fun showErrorSnackbar(throwable: Throwable) {
        uiState = uiState.copy(
            errorSnackbar = ErrorSnackbar(throwable = throwable, onOkClicked = appRouter::pop),
        )
    }

    private fun onAgreementClicked() {
        analyticsEventHandler.send(ReferralEvents.ClickTaC())

        lastReferralData?.tosLink?.let(urlOpener::openUrl)
    }

    private fun onCopyClicked() {
        analyticsEventHandler.send(ReferralEvents.ClickCopy())
    }

    private fun onShareClicked(text: String) {
        analyticsEventHandler.send(ReferralEvents.ClickShare())

        shareManager.shareText(text = text)
    }

    private fun ReferralData.convertToReferralInfoState(accountAward: AccountAward?): ReferralInfoState = when (this) {
        is ReferralData.ParticipantData -> ReferralInfoState.ParticipantContent(
            award = getAwardValue(),
            networkName = getNetworkName(),
            address = referral.getAddressValue(),
            discount = getDiscountValue(),
            purchasedWalletCount = referral.walletsPurchased,
            code = referral.promocode,
            shareLink = referral.shareLink,
            url = tosLink,
            expectedAwards = expectedAwards,
            accountAward = accountAward,
        )
        is ReferralData.NonParticipantData -> {
            val userWallet = getUserWalletUseCase(params.userWalletId).getOrNull() ?: error("User wallet not found")

            ReferralInfoState.NonParticipantContent(
                award = getAwardValue(),
                networkName = getNetworkName(),
                discount = getDiscountValue(),
                url = tosLink,
                onParticipateClicked = ::participate,
                participateButtonIcon = walletInterationIcon(userWallet),
                accountAward = accountAward,
            )
        }
    }

    private fun ReferralData.getAwardValue(): String = "$award ${getToken().symbol}"

    private fun ReferralData.getNetworkName(): String = getToken().networkId.replaceFirstChar(Char::uppercase)

    private fun walletAccounts(walletId: UserWalletId): Flow<AccountStatusList> = singleAccountStatusListSupplier(
        SingleAccountStatusListProducer.Params(walletId),
    )

    private suspend fun findAccountByAddress(address: String): AccountCryptoCurrency? {
        return getAccountCurrencyByAddressUseCase(address).getOrNull()
    }

    private suspend fun selectAccount(referralData: ReferralData) {
        when (referralData) {
            is ReferralData.NonParticipantData -> if (portfolioSelectorController.selectedAccountSync == null) {
                portfolioSelectorController.selectAccount(
                    accountId = walletAccounts(params.userWalletId).first().mainAccount.account.accountId,
                )
            }
            is ReferralData.ParticipantData -> portfolioSelectorController.selectAccount(
                accountId = findAccountByAddress(referralData.referral.address)?.account?.accountId,
            )
        }
    }

    @Suppress("MagicNumber")
    private fun ReferralInfo.getAddressValue(): String {
        check(address.length > 5) { "Invalid address" }
        return address.substring(startIndex = 0, endIndex = 4) + "..." +
            address.substring(startIndex = address.length - 5, endIndex = address.length)
    }

    private fun ReferralData.getDiscountValue(): String {
        val discountSymbol = when (discountType) {
            DiscountType.PERCENTAGE -> "%"
            DiscountType.VALUE -> this.getToken().symbol
        }
        return "$discount$discountSymbol"
    }

    private fun ReferralData.getToken() = requireNotNull(tokens.firstOrNull()) { "Token list is empty" }
}