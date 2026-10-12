package com.tangem.features.txhistory.model

import android.content.Context
import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.entity.AddressBookOpenMode
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.addressbook.usecase.IsAddressBookCompatibleUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.staking.GetStakingTargetsByAddressUseCase
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.tokens.model.analytics.TokenExchangeAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenOnrampAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.txhistory.model.explorerHash
import com.tangem.domain.txhistory.model.idToCopy
import com.tangem.domain.txhistory.usecase.GetCachedTokenInfoUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.rating.RatingComponent
import com.tangem.features.txhistory.analytics.TxHistoryAnalyticsEvent
import com.tangem.features.txhistory.analytics.toAnalyticsStatus
import com.tangem.features.txhistory.analytics.toAnalyticsType
import com.tangem.features.txhistory.component.TxHistoryDetailsComponent
import com.tangem.features.txhistory.converter.ExpressTxToShareTextConverter
import com.tangem.features.txhistory.converter.FiatConversion
import com.tangem.features.txhistory.converter.TxHistoryInfoToTxHistoryDetailsUMConverter
import com.tangem.features.txhistory.converter.fiatCode
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class TxHistoryDetailsModel @Inject constructor(
    @ApplicationContext private val context: Context,
    override val dispatchers: CoroutineDispatcherProvider,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val getStakingTargetsByAddressUseCase: GetStakingTargetsByAddressUseCase,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val balanceHidingSettings: GetBalanceHidingSettingsUseCase,
    private val router: Router,
    isAddressBookCompatibleUseCase: IsAddressBookCompatibleUseCase,
    getCachedTokenInfoUseCase: GetCachedTokenInfoUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    ownerLookupProducer: TxHistoryOwnerLookupProducer,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: TxHistoryDetailsComponent.Params = paramsContainer.require()

    private val shareTextConverter = ExpressTxToShareTextConverter()
    private val txHistoryInfo = params.historyTxListManager.txExpressHistoryItemFlow(params.txId)

    /**
     * Known staking targets (StakeKit validators and P2P vaults) keyed by on-chain address, used to resolve the target
     * a staking [TxInfo] interacts with to its display name and page.
     *
     * Read from the persisted targets rather than the live "enabled yields" cache, so a validator that is no longer
     * active — or no longer returned by the provider at all — still resolves for an old transaction. The lookup is
     * subscribed only while the viewed tx is a staking op ([requiresValidatorLookup]); a Send / Swap / onramp has no
     * target to resolve and stays on the empty map.
     */
    private val targetsByAddress: Flow<Map<String, StakingTarget>> = txHistoryInfo
        .map { it.requiresValidatorLookup() }
        .distinctUntilChanged()
        .flatMapLatest { requiresLookup ->
            if (requiresLookup) getStakingTargetsByAddressUseCase() else flowOf(emptyMap())
        }
        .onStart { emit(emptyMap()) }
        .distinctUntilChanged()
        .flowOn(dispatchers.io)

    /**
     * The portfolio token a refunded DEX-bridge swap was refunded in — drives the "Refunded in {token}" terminal.
     * `null` while unresolved or when the deal carries no refund token.
     */
    private val refundCurrency = MutableStateFlow<CryptoCurrency?>(null)

    /**
     * Provider-rating (CSAT) card slot. Activated once the viewed tx turns out to be an express swap that reached a
     * terminal state, mirroring the legacy express-status sheet. Stays dismissed for onramp / on-chain txs.
     */
    val ratingSlotNavigation = SlotNavigation<RatingComponent.Params>()

    private var isRatingActivationStarted = false

    /**
     * One-shot guard for the screen-opened analytics, sent from the [uiState] combine below instead of a dedicated
     * `.first()` collection of [txHistoryInfo]: that flow is cold and re-triggers its own standalone express-tx
     * subscription per collector (see `DefaultHistoryTxListManager.txExpressHistoryItemFlow`), so a second collector
     * would double the load for a tx not yet in the paginated list.
     */
    private var isScreenOpenedAnalyticsSent = false

    init {
        // One-shot: the portfolio add must not re-run when the UI resubscribes.
        modelScope.launch(dispatchers.default) {
            val refundAssetId = txHistoryInfo
                .mapNotNull { it.bridgeRefundTx()?.refundAssetId }
                .first()
            refundCurrency.value = addRefundTokenToPortfolio(refundAssetId)
        }
    }

    private val networkTokensFlow: Flow<Set<CryptoCurrency.Token>> = flow {
        emit(getCachedTokenInfoUseCase(params.userWalletId, params.currency.network.rawId))
    }
        .onStart { emit(emptySet()) }
        .flowOn(dispatchers.io)

    /**
     * Rate of the viewed currency and the app currency it is quoted in, for the fiat line under the details amount.
     * Both sources can keep the card waiting — a currency the account list has not yielded yet never emits a status
     * at all — so the pair starts at [FiatConversion.NONE]: the card renders at once and the fiat line appears with
     * the rate.
     */
    private val fiatConversionFlow: Flow<FiatConversion> = combine(
        flow = getAccountCurrencyStatusUseCase(userWalletId = params.userWalletId, currency = params.currency)
            .map { it.status.value.fiatRate },
        flow2 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
    ) { rate, appCurrency -> FiatConversion(rate = rate, appCurrency = appCurrency) }
        .onStart { emit(FiatConversion.NONE) }
        .distinctUntilChanged()
        .flowOn(dispatchers.io)

    /**
     * The owner/contact lookup, whether saving a contact is possible at all (an address book stored by a newer build
     * cannot be written to, so "Add contact" must not be offered), the wallet's cached tokens and the fiat
     * conversion. Combined here rather than in the [uiState] `combine` below, which is already at the five-source
     * overload.
     */
    private val detailsInputs: Flow<DetailsInputs> =
        combine(
            flow = ownerLookupProducer(params.userWalletId),
            flow2 = isAddressBookCompatibleUseCase(params.userWalletId),
            flow3 = networkTokensFlow,
            flow4 = fiatConversionFlow,
        ) { lookup, isCompatible, tokens, fiatConversion ->
            DetailsInputs(
                lookup = lookup,
                isAddressBookCompatible = isCompatible,
                networkTokens = tokens,
                fiatConversion = fiatConversion,
            )
        }

    val uiState: StateFlow<TxHistoryDetailsUM?> = combine(
        flow = txHistoryInfo,
        flow2 = detailsInputs,
        flow3 = targetsByAddress,
        flow4 = refundCurrency,
        flow5 = balanceHidingSettings.isBalanceHidden(),
    ) { txInfo, inputs, stakingTargets, refundToken, isBalanceHidden ->
        val lookup = inputs.lookup
        // Each header-menu row drops with the data behind it: no explorer hash (an express op with no on-chain leg
        // yet, or a blank on-chain hash) drops "Explore"; only an express deal can describe itself as text, so an
        // on-chain row has no "Share"; a blank id drops "Transaction ID".
        val explorerHash = txInfo.explorerHash?.ifBlank { null }
        val idToCopy = txInfo.idToCopy.ifBlank { null }
        val shareText = (txInfo as? ExpressTx)?.let(shareTextConverter::convert)
        val contactCandidate = txInfo
            .contactCandidate(currency = params.currency, lookup = lookup)
            .takeIf { inputs.isAddressBookCompatible }
        TxHistoryInfoToTxHistoryDetailsUMConverter(
            currency = params.currency,
            onCopyAddress = { address -> onCopyAddress(address, txInfo) },
            onGoToProvider = { url -> onGoToProvider(url, txInfo) },
            onCopyTxId = idToCopy?.let { id -> { onCopyTxId(id, txInfo) } },
            shareText = shareText,
            onShare = { text -> onShare(text, txInfo) },
            onExplore = explorerHash?.let { hash -> { explore(hash, txInfo) } },
            refundCurrency = refundToken,
            onLearnMoreAboutRefundsClick = ::onLearnMoreAboutRefunds,
            onGoToRefundedTokenClick = params.onOpenTokenDetails,
            lookup = lookup,
            targetsByAddress = stakingTargets,
            isBalanceHidden = isBalanceHidden,
            onExploreProvider = urlOpener::openUrl,
            onAddContact = contactCandidate?.let { candidate -> { openContactCreation(candidate) } },
            networkTokens = inputs.networkTokens,
            fiatConversion = inputs.fiatConversion,
        ).convert(txInfo).also { um ->
            if (!isScreenOpenedAnalyticsSent) {
                isScreenOpenedAnalyticsSent = true
                sendScreenOpenedAnalytics(txInfo = txInfo, title = um.header.title)
            }
        }
    }
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = null)

    /** Opens the contact editor prefilled with the counterparty address and its network. */
    private fun openContactCreation(candidate: ContactCandidate) {
        router.push(
            AppRoute.AddressBook(
                AddressBookOpenMode.WithContactCreation(
                    address = candidate.address,
                    networkId = candidate.networkId.value,
                    source = AddressBookOpenMode.ContactCreationSource.TransactionDetails,
                ),
            ),
        )
    }

    /**
     * Activates the rating slot for an express swap. The rating key is the provider-side deal id when present, the
     * express id otherwise — same as the legacy surface, so ratings stay shared between the old and new UI.

     *
     * Only a completed deal gets the widget: the survey vendor caps API calls per day and a swap in flight
     * is re-opened many times while the user watches its status. The flow keeps collecting, so the slot
     * still appears by itself once a deal watched from this screen completes.
     */
    fun activateRatingForSwap() {
        if (isRatingActivationStarted) return
        isRatingActivationStarted = true
        txHistoryInfo
            .mapNotNull { it as? ExpressTx.Swap }
            .filter { swap -> swap.tx.status.isRateable }
            .map { swap ->
                RatingKey(
                    txExternalId = swap.externalTxId ?: swap.txId,
                    providerName = swap.provider?.name.orEmpty(),
                    txExternalUrl = swap.externalTxUrl.orEmpty(),
                )
            }
            .distinctUntilChanged()
            .onEach { key ->
                ratingSlotNavigation.activate(
                    RatingComponent.Params(
                        txExternalId = key.txExternalId,
                        providerName = key.providerName,
                        txExternalUrl = key.txExternalUrl,
                        userWalletId = params.userWalletId,
                        isRedesign = true,
                    ),
                )
            }
            .launchIn(modelScope)
    }

    /**
     * Whether the row is an on-chain tx that could name a staking target (see [mayCarryStakingTarget]). A transfer /
     * swap / approval, or any express row, never does, so the lookup is skipped for those.
     */
    private fun TxHistoryInfo.requiresValidatorLookup(): Boolean =
        (this as? OnChainTx.BSDK)?.txInfo?.mayCarryStakingTarget() == true

    /** Copies a counterparty address to the clipboard — wired into the detail card's copy button via the converter. */
    private fun onCopyAddress(address: String, txInfo: TxHistoryInfo) {
        analyticsEventHandler.send(
            TokenScreenAnalyticsEvent.ButtonCopyAddress(
                token = params.currency.symbol,
                blockchain = params.currency.network.name,
                type = txInfo.toAnalyticsType(),
                source = TokenScreenAnalyticsEvent.ButtonCopyAddress.CopyAddressActionSource.TransactionDetail,
            ),
        )
        clipboardManager.setText(text = address, isSensitive = false)
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(R.string.wallet_notification_address_copied),
                startIconId = R.drawable.ic_check_24,
            ),
        )
    }

    /** Copies the transaction id to the clipboard — wired into the header menu's "Transaction ID" row. */
    private fun onCopyTxId(id: String, txInfo: TxHistoryInfo) {
        analyticsEventHandler.send(
            TxHistoryAnalyticsEvent.ButtonCopyTransactionId(
                token = params.currency.symbol,
                blockchain = params.currency.network.name,
                type = txInfo.toAnalyticsType(),
            ),
        )
        clipboardManager.setText(text = id, isSensitive = false)
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(R.string.express_transaction_id_copied),
                startIconId = R.drawable.ic_check_24,
            ),
        )
    }

    /** Shares the transaction summary — wired into the header menu's "Share" row. */
    private fun onShare(text: String, txInfo: TxHistoryInfo) {
        analyticsEventHandler.send(
            TokenScreenAnalyticsEvent.ButtonShare(
                token = params.currency.symbol,
                blockchain = params.currency.network.name,
                type = txInfo.toAnalyticsType(),
                source = TokenScreenAnalyticsEvent.ButtonShare.ShareActionSource.TransactionDetail,
            ),
        )
        shareManager.shareText(text)
    }

    /** Opens the transaction in the blockchain explorer — wired into the header menu's "Explore" row. */
    private fun explore(txHash: String, txInfo: TxHistoryInfo) {
        analyticsEventHandler.send(
            TokenScreenAnalyticsEvent.ButtonExplore(
                token = params.currency.symbol,
                source = TokenScreenAnalyticsEvent.ButtonExplore.ExploreActionSource.TransactionDetail,
                blockchain = params.currency.network.name,
                type = txInfo.toAnalyticsType(),
            ),
        )
        getExplorerTransactionUrlUseCase(txHash = txHash, currency = params.currency).fold(
            ifLeft = { TangemLogger.e(it.toString()) },
            ifRight = { urlOpener.openUrl(url = it) },
        )
    }

    /**
     * Opens a provider link — wired into both the "Go to provider"/"Go to verification" bottom CTA and the provider
     * info row of an express deal. Restores the pre-redesign "Button - Go To Provider" analytics (see the legacy
     * `ExpressStatusFactory`/`TokenDetailsSwapTransactionsStateConverter`): Place is KYC while the swap is under
     * verification, Status otherwise; an onramp has no Place breakdown in the reused event.
     */
    private fun onGoToProvider(url: String, txInfo: TxHistoryInfo) {
        when (txInfo) {
            is ExpressTx.Swap -> analyticsEventHandler.send(
                if (txInfo.tx.status == ExpressExchangeStatus.Verifying) {
                    TokenExchangeAnalyticsEvent.GoToProviderKYC(params.currency.symbol)
                } else {
                    TokenExchangeAnalyticsEvent.GoToProviderStatus(params.currency.symbol)
                },
            )
            is ExpressTx.Onramp -> analyticsEventHandler.send(TokenOnrampAnalyticsEvent.GoToProvider())
            else -> Unit
        }
        urlOpener.openUrl(url)
    }

    /**
     * One-shot "Transaction Detail Screen Opened" (new, [REDACTED_TASK_KEY]) plus the pre-redesign "Swap Status Opened" /
     * "Onramp Status Opened" (restored — see `TokenExchangeAnalyticsEvent`/`TokenOnrampAnalyticsEvent`, previously sent
     * only from the legacy `tokendetails` bottom sheet that the new detail screen replaces).
     */
    private fun sendScreenOpenedAnalytics(txInfo: TxHistoryInfo, title: TextReference) {
        analyticsEventHandler.send(
            TxHistoryAnalyticsEvent.TransactionDetailScreenOpened(
                token = params.currency.symbol,
                blockchain = params.currency.network.name,
                type = txInfo.toAnalyticsType(),
                status = txInfo.toAnalyticsStatus(),
                title = title.resolveReference(context.resources),
            ),
        )
        when (txInfo) {
            is ExpressTx.Swap -> analyticsEventHandler.send(
                TokenExchangeAnalyticsEvent.CexTxStatusOpened(
                    token = params.currency.symbol,
                    provider = txInfo.provider?.name.orEmpty(),
                ),
            )
            is ExpressTx.Onramp -> analyticsEventHandler.send(
                TokenOnrampAnalyticsEvent.OnrampStatusOpened(
                    tokenSymbol = params.currency.symbol,
                    provider = txInfo.provider?.name.orEmpty(),
                    fiatCurrency = txInfo.tx.fromFiat.fiatCode,
                ),
            )
            else -> Unit
        }
    }

    /** Opens the cross-chain-bridges blog article — wired into the refunded banner's "Learn more" link. */
    private fun onLearnMoreAboutRefunds() {
        modelScope.launch {
            urlOpener.openUrl(TangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.AboutCrossChainBridges))
        }
    }

    /**
     * Adds the refund token to the account of the viewed currency and returns it (idempotent);
     * `null` when the account or the token cannot be resolved (e.g. offline).
     */
    private suspend fun addRefundTokenToPortfolio(ref: ExpressAsset.ID): CryptoCurrency? {
        val accountId = getAccountCurrencyStatusUseCase
            .invokeSync(userWalletId = params.userWalletId, currency = params.currency)
            .map { it.account.accountId }
            .getOrElse {
                TangemLogger.e("Unable to resolve account for refund token ${params.currency.id}")
                return null
            }

        return manageCryptoCurrenciesUseCase.add(
            accountId = accountId,
            networkId = ref.networkId,
            contractAddress = ref.contractAddress,
        )
            .onLeft { TangemLogger.e("Unable to resolve refund token", it) }
            .getOrNull()
    }
}

/** The per-wallet sources the details converter needs, combined apart from the already five-wide `uiState` combine. */
private data class DetailsInputs(
    val lookup: TxHistoryLookupContext,
    val isAddressBookCompatible: Boolean,
    val networkTokens: Set<CryptoCurrency.Token>,
    val fiatConversion: FiatConversion,
)

/** Identity of the rating slot: re-activation is needed only when one of these deal fields changes. */
private data class RatingKey(
    val txExternalId: String,
    val providerName: String,
    val txExternalUrl: String,
)

/**
 * The deal of a refunded DEX-bridge swap; `null` for everything else. Only a DEX-bridge deal is refunded in an
 * intermediate token, so other provider types must not surface the "Refunded in {token}" terminal.
 */
private fun TxHistoryInfo.bridgeRefundTx(): ExchangeTransaction? {
    val tx = (this as? ExpressTx.Swap)?.tx ?: return null
    if (tx.status != ExpressExchangeStatus.Refunded) return null
    if (tx.provider?.type != ExpressProviderType.DEX_BRIDGE) return null
    return tx
}