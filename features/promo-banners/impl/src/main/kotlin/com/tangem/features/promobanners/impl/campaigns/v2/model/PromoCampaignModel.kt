package com.tangem.features.promobanners.impl.campaigns.v2.model

import com.tangem.common.ui.account.AccountIconItemStateConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoEnrollment
import com.tangem.domain.promo.models.PromoRegistrationError
import com.tangem.domain.promo.usecase.GetPromoCampaignsUseCase
import com.tangem.domain.promo.usecase.RegisterPromoCampaignUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.commonfeatures.api.choosetoken.ChooserBlock
import com.tangem.features.commonfeatures.api.choosetoken.PaymentAccountTokens
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.model.PredefinedTokenResolver
import com.tangem.features.promobanners.impl.campaigns.v2.component.PromoCampaignBottomSheetComponent
import com.tangem.features.promobanners.impl.campaigns.v2.component.PromoCampaignRoute
import com.tangem.features.promobanners.impl.campaigns.v2.converters.PromoCampaignStatusUMConverter
import com.tangem.features.promobanners.impl.campaigns.v2.converters.PromoCampaignUMConverter
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoCampaignStatusUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoCampaignUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoSelectedAccountUM
import com.tangem.features.wallet.utils.UserWalletsFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class PromoCampaignModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    chooseTokenBridgeFactory: ChooseTokenBridge.Factory,
    predefinedTokenResolver: PredefinedTokenResolver,
    private val router: Router,
    private val multiAccountListSupplier: MultiAccountListSupplier,
    private val urlOpener: UrlOpener,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val registerPromoCampaignUseCase: RegisterPromoCampaignUseCase,
    private val getPromoCampaignsUseCase: GetPromoCampaignsUseCase,
    private val userWalletsFetcherFactory: UserWalletsFetcher.Factory,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    @GlobalUiMessageSender private val messageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<PromoCampaignBottomSheetComponent.Params>()

    private val converter = PromoCampaignUMConverter(
        onLinkClick = urlOpener::openUrl,
        onActionClick = ::onActionClick,
        onChooseTokenClick = ::onChooseTokenClick,
    )

    val uiState: StateFlow<PromoCampaignUM>
        field = MutableStateFlow(converter.convert(params.campaign))

    val statusState: StateFlow<PromoCampaignStatusUM?>
        field = MutableStateFlow(null)

    private var chosenPayout: ChosenPayout? = null
    private var enrollJob: Job? = null

    val bridge: ChooseTokenBridge = chooseTokenBridgeFactory.create(
        modelScope = modelScope,
        settings = ChooseTokenBridge.Settings(
            title = resourceReference(R.string.common_choose_token),
            chooserBlock = ChooserBlock.Predefined(
                MutableStateFlow(predefinedTokenResolver.resolve(params.campaign.payout.tokens)),
            ),
            paymentAccountTokens = PaymentAccountTokens.Hidden,
            isShowSingleCurrencyWallets = true,
            isAppBarShown = false,
        ),
    )

    init {
        bridge.onCurrencyChosen.receiveAsFlow()
            .onEach(::onTokenChosen)
            .launchIn(modelScope)

        bridge.onClose.receiveAsFlow()
            .onEach { router.pop() }
            .launchIn(modelScope)
    }

    private fun onActionClick() {
        val tokenSelector = uiState.value.tokenSelector
        if (tokenSelector != null && tokenSelector.selectedToken == null) {
            onChooseTokenClick()
        } else {
            onEnrollClick()
        }
    }

    private fun onEnrollClick() {
        if (enrollJob?.isActive == true) return

        enrollJob = modelScope.launch {
            setEnrolling(isEnrolling = true)
            val wallets = getWalletsUseCase.invokeSync()
            val enrollments = PromoEnrollmentsFactory.create(
                campaign = params.campaign,
                mainWalletId = params.userWalletId,
                wallets = wallets,
                payout = chosenPayout?.target,
            )
            val result = registerPromoCampaignUseCase(promotionId = params.campaign.id, enrollments = enrollments)
                .onLeft { error ->
                    TangemLogger.e(
                        "Error registering for campaign ${params.campaign.id}: $error",
                        (error as? PromoRegistrationError.Generic)?.cause,
                    )
                }
                .onRight { refreshCampaigns(enrollments, wallets) }
            setEnrolling(isEnrolling = false)

            showOutcome(
                outcome = PromoRegistrationOutcome.resolve(params.userWalletId, enrollments, result),
                wallets = wallets,
            )
        }
    }

    /**
     * The campaigns cache still says "not enrolled" after registering. Awaited before the outcome is shown, because
     * leaving the sheet cancels [modelScope] and a background refresh would never reach the cache.
     */
    private suspend fun refreshCampaigns(enrollments: List<PromoEnrollment>, wallets: List<UserWallet>) {
        val enrolledWalletIds = enrollments.map(PromoEnrollment::userWalletId).toSet()

        coroutineScope {
            wallets.filter { it.walletId in enrolledWalletIds }
                .map { wallet ->
                    async {
                        getPromoCampaignsUseCase(wallet, forceRefresh = true).onLeft { error ->
                            TangemLogger.e("Failed to refresh promo campaigns after registration", error)
                        }
                    }
                }
                .awaitAll()
        }
    }

    private fun setEnrolling(isEnrolling: Boolean) {
        uiState.update { state -> state.copy(footer = state.footer.copy(isLoading = isEnrolling)) }
    }

    private fun showOutcome(outcome: PromoRegistrationOutcome, wallets: List<UserWallet>) {
        val isPayoutChosen = outcome.isPayoutChosen()
        val recipientWalletName = if (isPayoutChosen) {
            chosenPayout?.walletName
        } else {
            wallets.firstOrNull { it.walletId == params.userWalletId }?.name
        }
        val selectedToken = uiState.value.tokenSelector?.selectedToken
        val status = PromoCampaignStatusUMConverter(
            campaign = params.campaign,
            recipientWalletName = stringReference(recipientWalletName.orEmpty()),
            recipientIcon = selectedToken?.iconState.takeIf { isPayoutChosen },
            onDismiss = { router.pop() },
        ).convert(outcome)

        if (status == null) {
            showFailedDialog()
            return
        }

        statusState.value = status
        router.replaceAll(PromoCampaignRoute.Status)
        if (outcome is PromoRegistrationOutcome.Enrolled && status.details != null) {
            loadEnrolledWallets(outcome.enrolledWalletIds.toSet())
        }
    }

    /**
     * An earlier registration may pay out to another token or address, whose icon and owning wallet we don't
     * know — the main wallet's name is then the best label.
     */
    private fun PromoRegistrationOutcome.isPayoutChosen(): Boolean {
        val payout = (this as? PromoRegistrationOutcome.AlreadyEnrolled)?.payout ?: return false
        val chosen = chosenPayout?.target ?: return false

        return payout.tokenAddress.equals(chosen.tokenAddress, ignoreCase = true) &&
            payout.networkId == chosen.networkId &&
            payout.userAddress.equals(chosen.userAddress, ignoreCase = true)
    }

    private fun loadEnrolledWallets(walletIds: Set<UserWalletId>) {
        userWalletsFetcherFactory.create(
            messageSender = messageSender,
            onlyMultiCurrency = false,
            isAuthMode = false,
            isClickableIfLocked = false,
            onWalletClick = {},
        ).userWallets
            .onEach { items ->
                val enrolled = items.filter { item -> walletIds.any { it.stringValue == item.id } }
                statusState.update { status ->
                    status?.copy(details = PromoCampaignStatusUM.Details.EnrolledWallets(enrolled.toImmutableList()))
                }
            }
            .launchIn(modelScope)
    }

    private fun showFailedDialog() {
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = resourceReference(R.string.common_try_again_later),
                firstActionBuilder = {
                    EventMessageAction(title = resourceReference(R.string.common_support), onClick = ::onSupportClick)
                },
                secondActionBuilder = {
                    EventMessageAction(title = resourceReference(R.string.common_close), onClick = onDismissRequest)
                },
            ),
        )
    }

    private fun onSupportClick() {
        modelScope.launch {
            getWalletMetaInfoUseCase(params.userWalletId)
                .onRight { metaInfo -> sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(metaInfo)) }
                .onLeft { error -> TangemLogger.e("Error getting wallet meta info for support email", error) }
        }
    }

    private fun ChooseTokenResult.toPayoutTarget(): PromoEnrollment.PromoPayoutTarget? {
        val token = currency.currency
        val address = currency.value.networkAddress?.defaultAddress?.value ?: return null

        return PromoEnrollment.PromoPayoutTarget(
            userAddress = address,
            tokenAddress = (token as? CryptoCurrency.Token)?.contractAddress,
            networkId = token.network.rawId,
            tokenId = token.id.rawCurrencyId?.value,
        )
    }

    private fun onChooseTokenClick() {
        router.push(PromoCampaignRoute.ChooseToken)
    }

    private fun onTokenChosen(result: ChooseTokenResult) {
        val target = result.toPayoutTarget()
        if (target == null) {
            TangemLogger.e("Chosen payout token ${result.currency.currency.id} has no payout address")
            return
        }
        chosenPayout = ChosenPayout(target = target, walletName = result.wallet.name)
        modelScope.launch {
            val selectedAccount = if (hasMultipleCryptoPortfolioAccounts()) {
                (result.account.account as? Account.CryptoPortfolio)?.let { account ->
                    PromoSelectedAccountUM(
                        iconState = AccountIconItemStateConverter(size = AccountIconSize.ExtraSmall).convert(account),
                        name = account.accountName.toUM().value,
                    )
                }
            } else {
                null
            }

            val selectedToken = TokenItemStateConverter(
                appCurrency = AppCurrency.Default,
                subtitleStateProvider = { status ->
                    TokenItemState.SubtitleState.TextContent(
                        value = resourceReference(
                            R.string.domain_receive_assets_onboarding_network_name,
                            wrappedList(status.currency.network.name),
                        ),
                    )
                },
            ).convert(result.currency)

            uiState.update { state ->
                state.copy(
                    footer = converter.convertFooter(campaign = params.campaign, isPayoutTokenPending = false),
                    tokenSelector = state.tokenSelector?.copy(
                        selectedToken = selectedToken,
                        selectedAccount = selectedAccount,
                    ),
                )
            }
            router.pop()
        }
    }

    private suspend fun hasMultipleCryptoPortfolioAccounts(): Boolean {
        return multiAccountListSupplier.invoke()
            .first()
            .any { accountList -> accountList.accounts.filterIsInstance<Account.CryptoPortfolio>().size > 1 }
    }

    private data class ChosenPayout(val target: PromoEnrollment.PromoPayoutTarget, val walletName: String)
}