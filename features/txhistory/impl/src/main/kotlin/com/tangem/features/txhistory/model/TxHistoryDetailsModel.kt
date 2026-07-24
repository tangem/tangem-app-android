package com.tangem.features.txhistory.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.staking.GetYieldUseCase
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.txhistory.model.explorerHash
import com.tangem.domain.txhistory.model.idToCopy
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.rating.RatingComponent
import com.tangem.features.txhistory.component.TxHistoryDetailsComponent
import com.tangem.features.txhistory.converter.TxHistoryInfoToTxHistoryDetailsUMConverter
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    override val dispatchers: CoroutineDispatcherProvider,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val getYieldUseCase: GetYieldUseCase,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    ownerLookupProducer: TxHistoryOwnerLookupProducer,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: TxHistoryDetailsComponent.Params = paramsContainer.require()

    /**
     * Validators of the viewed currency's staking yield, keyed by on-chain address. Used to resolve the validator
     * address carried by a staking [TxInfo] to its display name and page. Empty when the currency has no staking yield
     * (the use case reads the prefetched yields cache and fails gracefully for non-staking / custom tokens).
     *
     * The yield is fetched only when the viewed tx is a staking op ([requiresValidatorLookup]) — a Send / Swap / onramp
     * has no validator to resolve, so it skips the use case entirely and stays on the empty map.
     */
    private val validatorsByAddress: Flow<Map<String, Yield.Validator>> = params.txHistoryInfo
        .map { it.requiresValidatorLookup() }
        .distinctUntilChanged()
        .map { requiresLookup -> if (requiresLookup) loadValidators() else emptyMap() }
        .onStart { emit(emptyMap()) }
        .distinctUntilChanged()
        .flowOn(dispatchers.io)

    /**
     * The portfolio token a refunded DEX-bridge swap was refunded in — drives the "Refunded in {token}" terminal.
     * `null` while unresolved or when the deal carries no refund token.
     */
    private val refundCurrency = MutableStateFlow<CryptoCurrency?>(null)

    /**
     * Provider-rating (CSAT) card slot. Activated once the viewed tx turns out to be an express swap — the card is
     * shown for any swap status, mirroring the legacy express-status sheet. Stays dismissed for onramp / on-chain txs.
     */
    val ratingSlotNavigation = SlotNavigation<RatingComponent.Params>()

    private var isRatingActivationStarted = false

    init {
        // One-shot: the portfolio add must not re-run when the UI resubscribes.
        modelScope.launch(dispatchers.default) {
            val refundAssetId = params.txHistoryInfo
                .mapNotNull { it.bridgeRefundTx()?.refundAssetId }
                .first()
            refundCurrency.value = addRefundTokenToPortfolio(refundAssetId)
        }
    }

    val uiState: StateFlow<TxHistoryDetailsUM?> = combine(
        flow = params.txHistoryInfo,
        flow2 = ownerLookupProducer(),
        flow3 = validatorsByAddress,
        flow4 = refundCurrency,
    ) { txInfo, lookup, validators, refundToken ->
        // No explorer hash (e.g. an express op with no on-chain leg yet, or a blank on-chain hash) → the "Share" and
        // "Explore" rows are dropped; a blank id drops the "Transaction ID" row.
        val explorerHash = txInfo.explorerHash?.ifBlank { null }
        val idToCopy = txInfo.idToCopy.ifBlank { null }
        TxHistoryInfoToTxHistoryDetailsUMConverter(
            currency = params.currency,
            onCopyAddress = ::onCopyAddress,
            onGoToProvider = urlOpener::openUrl,
            onCopyTxId = idToCopy?.let { id -> { onCopyTxId(id) } },
            onShare = explorerHash?.let { hash -> { share(hash) } },
            onExplore = explorerHash?.let { hash -> { explore(hash) } },
            refundCurrency = refundToken,
            onLearnMoreAboutRefundsClick = ::onLearnMoreAboutRefunds,
            onGoToRefundedTokenClick = params.onOpenTokenDetails,
            lookup = lookup,
            validatorsByAddress = validators,
            onOpenValidator = urlOpener::openUrl,
        ).convert(txInfo)
    }
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = null)

    /**
     * Activates the rating slot for an express swap. The rating key is the provider-side deal id when present, the
     * express id otherwise — same as the legacy surface, so ratings stay shared between the old and new UI.

     */
    fun activateRatingForSwap() {
        if (isRatingActivationStarted) return
        isRatingActivationStarted = true
        params.txHistoryInfo
            .mapNotNull { (it as? ExpressTx.Swap)?.tx }
            .map { tx ->
                RatingKey(
                    txExternalId = tx.externalTxId ?: tx.txId,
                    providerName = tx.provider?.name.orEmpty(),
                    txExternalUrl = tx.externalTxUrl.orEmpty(),
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
     * Resolves the viewed currency's staking validators into an address-keyed map. Returns empty when the currency has
     * no yield (non-staking / custom token) — the use case surfaces that as a [Left] which we treat as "no validators".
     */
    private suspend fun loadValidators(): Map<String, Yield.Validator> = getYieldUseCase(
        cryptoCurrencyId = params.currency.id,
        symbol = params.currency.symbol,
    ).fold(
        ifLeft = { emptyMap() },
        ifRight = { yield -> yield.validators.associateBy(Yield.Validator::address) },
    )

    /**
     * Whether the row is an on-chain staking op — the only case whose validator address can resolve to a validator.
     * A non-staking on-chain tx, or any express row, carries no validator, so the yield lookup is skipped.
     */
    private fun TxHistoryInfo.requiresValidatorLookup(): Boolean =
        (this as? OnChainTx.BSDK)?.txInfo?.type is TxInfo.TransactionType.Staking

    /** Copies a counterparty address to the clipboard — wired into the detail card's copy button via the converter. */
    private fun onCopyAddress(address: String) {
        clipboardManager.setText(text = address, isSensitive = false)
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(R.string.wallet_notification_address_copied),
                startIconId = R.drawable.ic_check_24,
            ),
        )
    }

    /** Copies the transaction id to the clipboard — wired into the header menu's "Transaction ID" row. */
    private fun onCopyTxId(id: String) {
        clipboardManager.setText(text = id, isSensitive = false)
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(R.string.express_transaction_id_copied),
                startIconId = R.drawable.ic_check_24,
            ),
        )
    }

    /** Opens the transaction in the blockchain explorer — wired into the header menu's "Explore" row. */
    private fun explore(txHash: String) {
        getExplorerTransactionUrlUseCase(txHash = txHash, currency = params.currency).fold(
            ifLeft = { TangemLogger.e(it.toString()) },
            ifRight = { urlOpener.openUrl(url = it) },
        )
    }

    /** Shares the transaction's explorer URL — wired into the header menu's "Share" row. */
    private fun share(txHash: String) {
        getExplorerTransactionUrlUseCase(txHash = txHash, currency = params.currency).fold(
            ifLeft = { TangemLogger.e(it.toString()) },
            ifRight = { shareManager.shareText(text = it) },
        )
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