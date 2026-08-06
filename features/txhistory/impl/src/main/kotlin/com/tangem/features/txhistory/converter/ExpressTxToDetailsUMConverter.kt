package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.swap.SwapRateFormatter
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.plus
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledResourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.domain.express.models.ExchangeTransaction
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressTransactionAsset
import com.tangem.domain.express.models.OnrampTransaction
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.StatusBannerUM.Style
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.ResolvedOwner
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.resolveOwner
import com.tangem.utils.StringsSigns
import com.tangem.utils.toBriefAddressFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Converts an [ExpressTx] (swap / onramp) to the [TxHistoryDetailsUM.TwoAssets] details card. The `from`/`to` legs come
 * from the express deal ([ExchangeTransaction] asset pair / [OnrampTransaction] fiat→asset), and the network-fee row
 * comes from the matched on-chain leg ([ExpressTx.txInfo]).
 */
internal class ExpressTxToDetailsUMConverter(
    private val onGoToProvider: (String) -> Unit,
    private val lookup: TxHistoryLookupContext,
    private val menu: ImmutableList<TxHistoryDetailsUM.MenuItemUM>,
    private val refundCurrency: CryptoCurrency? = null,
    private val onLearnMoreAboutRefundsClick: () -> Unit = {},
    private val onGoToRefundedTokenClick: (CryptoCurrency) -> Unit = {},
) {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()
    private val exchangeStatusConverter = ExpressExchangeStatusToUiStatusConverter()
    private val onrampStatusConverter = ExpressOnrampStatusToUiStatusConverter()

    fun convert(value: ExpressTx): TxHistoryDetailsUM.TwoAssets = when (value) {
        is ExpressTx.Swap -> convertExpressSwap(value)
        is ExpressTx.Onramp -> convertExpressOnramp(value)
    }

    /**
     * The two-asset block always renders the deal's `fromAsset`→`toAsset` regardless of [ExpressTx.Swap.isOutgoing] —
     * `isOutgoing` only selects which leg is the *viewed* one in the history row, it does not reorder the detail legs.
     */
    private fun convertExpressSwap(swap: ExpressTx.Swap): TxHistoryDetailsUM.TwoAssets {
        val status = exchangeStatusConverter.convert(swap.tx.status)
        val (fromOwner, toOwner) = swap.resolveLegOwners()
        val refundToken = refundCurrency.takeIf { swap.tx.status == ExpressExchangeStatus.Refunded }
        return TxHistoryDetailsUM.TwoAssets(
            header = TxHistoryDetailsUM.HeaderUM(
                icon = TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20),
                status = status,
                title = status.statusAwareTitle(R.string.common_swapping, R.string.common_swapped),
                subtitle = headerSubtitle(swap.timestampMillis),
                menu = menu,
            ),
            from = swap.tx.fromAsset.toAssetUM(
                label = ownerLabel(fromOwner, fallback = R.string.swapping_from_title_v2, owned = R.string.common_from),
                owner = fromOwner,
                sign = OUTGOING_SIGN,
                // The spent leg always stands as sent — on a failed/refunded deal only the never-received leg fades.
                isFaded = false,
            ),
            to = swap.tx.toAsset.toAssetUM(
                label = ownerLabel(toOwner, fallback = R.string.swapping_to_title, owned = R.string.common_to),
                owner = toOwner,
                sign = status.incomingSign(),
                isFaded = status is Status.Failed,
            ),
            statusBanner = refundToken?.let(::refundedInBanner) ?: swap.tx.status.toStatusBannerUM(),
            rows = swap.toInfoRows(
                onProviderClick = swap.providerClick(),
                rateRow = swap.tx.swapRateRow(),
                showProviderType = true,
            ),
            providerButton = refundToken?.let(::goToRefundedTokenButton)
                ?: providerButton(swap.externalTxUrl, swap.tx.status.providerButtonLabel()),
        )
    }

    /**
     * Refunded terminal with a resolved refund token: the red "Refunded in {symbol}" plaque with the token/network
     * explanation and the underlined "Learn more" link appended to the subtitle.
     */
    private fun refundedInBanner(refundToken: CryptoCurrency) = TxHistoryDetailsUM.StatusBannerUM(
        style = Style.Refunded,
        title = resourceReference(
            id = R.string.express_exchange_notification_refunded_in_title,
            formatArgs = wrappedList(refundToken.symbol),
        ),
        subtitle = resourceReference(
            id = R.string.express_exchange_notification_refunded_in_text,
            formatArgs = wrappedList(refundToken.symbol, refundToken.network.name),
        ) + stringReference(" ") + styledResourceReference(
            id = R.string.common_learn_more,
            spanStyleReference = { SpanStyle(textDecoration = TextDecoration.Underline) },
            onClick = onLearnMoreAboutRefundsClick,
        ),
        isLoading = false,
    )

    /** Bottom "Go to token" CTA of the refunded terminal — opens the refund token's details. */
    private fun goToRefundedTokenButton(refundToken: CryptoCurrency) = TxHistoryDetailsUM.ProviderButtonUM(
        text = resourceReference(R.string.common_go_to_token),
        onClick = { onGoToRefundedTokenClick(refundToken) },
    )

    private fun convertExpressOnramp(onramp: ExpressTx.Onramp): TxHistoryDetailsUM.TwoAssets {
        val status = onrampStatusConverter.convert(onramp.tx.status)
        val toOwner = resolveLeg(onramp.tx.payoutAddress, onramp.tx.toAsset.cryptoCurrency)?.toOwnerUM()
        return TxHistoryDetailsUM.TwoAssets(
            header = TxHistoryDetailsUM.HeaderUM(
                icon = TxIcon.Vector(Icons.ic_card_20),
                status = status,
                title = status.statusAwareTitle(
                    R.string.tx_history_onramp_top_up,
                    R.string.tx_history_onramp_topped_up,
                ),
                subtitle = headerSubtitle(onramp.timestampMillis),
                menu = menu,
            ),
            from = onramp.tx.fromFiat.toFiatAssetUM(
                // The fiat side was paid from a card, not a portfolio address — no owner to resolve.
                label = resourceReference(R.string.tx_history_you_paid),
                currencyIcon = onramp.tx.country?.image?.let { flagUrl ->
                    CurrencyIconState.FiatIcon(url = flagUrl, fallbackResId = R.drawable.ic_currency_24)
                },
            ),
            to = onramp.tx.toAsset.toAssetUM(
                label = ownerLabel(toOwner, fallback = R.string.swapping_to_title, owned = R.string.common_to),
                owner = toOwner,
                sign = status.onrampIncomingSign(),
                isFaded = status is Status.Failed,
            ),
            statusBanner = onramp.tx.status.toStatusBannerUM(),
            rows = onramp.toInfoRows(onProviderClick = onramp.providerClick(), rateRow = onramp.tx.onrampRateRow()),
            providerButton = providerButton(onramp.externalTxUrl, onramp.tx.status.providerButtonLabel()),
        )
    }

    /** The owners shown under a swap's two legs; either may be `null` (no owner card → "You send" / "You receive"). */
    private data class LegOwners(
        val from: TxHistoryDetailsUM.AssetOwnerUM?,
        val to: TxHistoryDetailsUM.AssetOwnerUM?,
    )

    /**
     * The (from, to) owners shown under the swap legs. A swap settled entirely within one own portfolio names no owner —
     * both legs read "You send" / "You receive". Otherwise each leg shows its owner via [toOwnerUM], which still drops an
     * own-wallet leg when the user has a single wallet (nothing to disambiguate).
     */
    private fun ExpressTx.Swap.resolveLegOwners(): LegOwners {
        val from = resolveLeg(tx.fromAddress, tx.fromAsset.cryptoCurrency)
        val to = resolveLeg(tx.payoutAddress, tx.toAsset.cryptoCurrency)
        return if (isSameOwnPortfolio(from, to)) {
            LegOwners(from = null, to = null)
        } else {
            LegOwners(from = from?.toOwnerUM(), to = to?.toOwnerUM())
        }
    }

    /**
     * Resolves a swap/onramp leg's [address] (on the leg currency's network) to its owner: the user's own account /
     * wallet, or an external counterparty. `null` when there is no address to resolve (e.g. the very-old-version missing
     * `fromAddress`, onramp fiat).
     */
    private fun resolveLeg(address: String?, legCurrency: CryptoCurrency?): ResolvedOwner? {
        if (address == null) return null
        return lookup.resolveOwner(address, legCurrency?.network?.id?.rawId)
    }

    /**
     * True when both swap legs settle in the same own portfolio — the same account, or (in wallet mode) the same wallet.
     * Such a swap has no counterparty to name, so its legs read "You send" / "You receive" with no owner card; legs that
     * differ (cross-account, cross-wallet, or a send-and-swap to an external address) keep their owner.
     */
    private fun isSameOwnPortfolio(from: ResolvedOwner?, to: ResolvedOwner?): Boolean = when {
        from is ResolvedOwner.OwnAccount && to is ResolvedOwner.OwnAccount ->
            from.account.accountId == to.account.accountId
        from is ResolvedOwner.OwnPaymentAccount && to is ResolvedOwner.OwnPaymentAccount ->
            from.account.accountId == to.account.accountId
        from is ResolvedOwner.OwnWallet && to is ResolvedOwner.OwnWallet ->
            from.userWalletId == to.userWalletId
        else -> false
    }

    /**
     * The owner card for a leg, or `null` when it names nothing worth disambiguating: an own-wallet leg is dropped when
     * the user has a single wallet (there is no other wallet to tell it apart from), so it reads "You send" / "You
     * receive". An own account (accounts mode) and an external address are always shown.
     */
    private fun ResolvedOwner.toOwnerUM(): TxHistoryDetailsUM.AssetOwnerUM? = when {
        this is ResolvedOwner.OwnWallet && lookup.walletInfoById.size <= 1 -> null
        else -> toAssetOwnerUM()
    }

    /** Maps a resolved leg owner to the model shown under the amount (own account / own wallet / external address). */
    private fun ResolvedOwner.toAssetOwnerUM(): TxHistoryDetailsUM.AssetOwnerUM = when (this) {
        is ResolvedOwner.OwnAccount -> TxHistoryDetailsUM.AssetOwnerUM.Account(
            name = account.accountName.toUM().value,
            iconResId = account.icon.value.getResId(),
            backgroundColor = account.icon.color.getUiColor(),
        )
        is ResolvedOwner.OwnPaymentAccount -> TxHistoryDetailsUM.AssetOwnerUM.PaymentAccount(
            name = account.accountName.toUM().value,
        )
        is ResolvedOwner.OwnWallet -> TxHistoryDetailsUM.AssetOwnerUM.Wallet(
            name = stringReference(walletInfo.name),
            deviceIconUM = walletInfo.deviceIconUM,
        )
        is ResolvedOwner.External -> TxHistoryDetailsUM.AssetOwnerUM.Address(
            name = stringReference(address.toBriefAddressFormat()),
            rawAddress = address,
        )
    }

    /** Leg caption: the direction-only [fallback] ("You send" / "You receive") without an owner, "From" / "To" with one. */
    private fun ownerLabel(
        owner: TxHistoryDetailsUM.AssetOwnerUM?,
        @StringRes fallback: Int,
        @StringRes owned: Int,
    ): TextReference = resourceReference(if (owner != null) owned else fallback)

    /** Opens the deal's provider page on tap; `null` when the deal has no provider link. */
    private fun ExpressTx.providerClick(): (() -> Unit)? = externalTxUrl?.let { url -> { onGoToProvider(url) } }

    private fun providerButton(url: String?, @StringRes label: Int?): TxHistoryDetailsUM.ProviderButtonUM? {
        if (url == null || label == null) return null
        return TxHistoryDetailsUM.ProviderButtonUM(
            text = resourceReference(label),
            onClick = { onGoToProvider(url) },
        )
    }

    /**
     * Builds one crypto leg of the two-asset block. The ticker symbol and icon come from the resolved
     * [ExpressTransactionAsset.cryptoCurrency]; when it is unresolved the symbol falls back to the network id and the
     * icon slot is left empty ([TxHistoryDetailsUM.AssetUM.currencyIcon] = `null`).
     */
    private fun ExpressTransactionAsset.toAssetUM(
        label: TextReference,
        owner: TxHistoryDetailsUM.AssetOwnerUM?,
        sign: String,
        isFaded: Boolean,
    ): TxHistoryDetailsUM.AssetUM {
        val symbol = displaySymbol
        val formatted = amount.format { crypto(
            symbol = symbol,
            decimals = decimals,
            ignoreSymbolPosition = true,
        ) }.trim()
        return TxHistoryDetailsUM.AssetUM(
            label = label,
            owner = owner,
            amount = stringReference((sign + formatted).trim()),
            currencyIcon = cryptoCurrency?.let(iconStateConverter::convert),
            isFaded = isFaded,
        )
    }

    /**
     * Builds the fiat ("You paid") leg of an onramp. The paid fiat amount is exact and carries no sign — neither `+`/`−`
     * nor the `~` estimate — so only the value is shown. [currencyIcon] is the paid-from country flag, or `null` when the
     * onramp carries no country. It never fades: the paid fiat stands as spent even on a failed onramp, where only the
     * never-received crypto leg is struck.
     */
    private fun Amount.toFiatAssetUM(
        label: TextReference,
        currencyIcon: CurrencyIconState?,
    ): TxHistoryDetailsUM.AssetUM {
        val code = fiatCode
        val formatted = (value ?: BigDecimal.ZERO)
            .format { fiat(fiatCurrencyCode = code, fiatCurrencySymbol = currencySymbol, ignoreSymbolPosition = true) }
        return TxHistoryDetailsUM.AssetUM(
            label = label,
            owner = null,
            amount = stringReference(formatted.trim()),
            currencyIcon = currencyIcon,
            isFaded = false,
        )
    }
}

// region Status banners

/**
 * Express swap status → the status plaque under the two-asset block.
 *
 * In-flight stages render as [Style.Info] with the rotating loader; [Verifying][ExpressExchangeStatus.Verifying]
 * (KYC) and the paused terminal as [Style.Warning]; the failure terminals as [Style.Error], the refunded terminal as
 * [Style.Refunded] and the [Expired][ExpressExchangeStatus.Expired] one as the grey [Style.Expired] clock; the
 * [Finished][ExpressExchangeStatus.Finished] success as [Style.Success] (the plaque then auto-collapses — see
 * `TxHistoryDetailsStatusBanner`). [Unknown][ExpressExchangeStatus.Unknown] carries nothing to show, so it hides the
 * plaque (`null`). The [Refunded][ExpressExchangeStatus.Refunded] mapping here is the fallback for an unresolved
 * refund token — with a resolved one the converter builds the richer "Refunded in {symbol}" plaque.
 */
private fun ExpressExchangeStatus.toStatusBannerUM(): TxHistoryDetailsUM.StatusBannerUM? = when (this) {
    ExpressExchangeStatus.Preview,
    ExpressExchangeStatus.Created,
    ExpressExchangeStatus.ExchangeTxSent,
    ExpressExchangeStatus.Waiting,
    -> loadingBanner(R.string.express_exchange_status_receiving_active)
    ExpressExchangeStatus.WaitingTxHash -> loadingBanner(R.string.express_exchange_status_waiting_tx_hash)
    ExpressExchangeStatus.Confirming -> loadingBanner(R.string.express_exchange_status_confirming_active)
    ExpressExchangeStatus.Exchanging -> loadingBanner(R.string.express_exchange_status_exchanging_active)
    ExpressExchangeStatus.Sending -> loadingBanner(R.string.express_exchange_status_sending_active)
    ExpressExchangeStatus.Verifying -> verificationBanner()
    ExpressExchangeStatus.Refunded -> refundedBanner(R.string.express_exchange_status_refunded)
    ExpressExchangeStatus.Paused -> warningBanner(R.string.express_exchange_status_paused)
    ExpressExchangeStatus.Failed,
    ExpressExchangeStatus.TxFailed,
    -> failedBanner()
    ExpressExchangeStatus.Expired -> expiredBanner(R.string.tx_history_details_status_expired)
    ExpressExchangeStatus.Finished -> successBanner(R.string.express_exchange_status_exchanged)
    ExpressExchangeStatus.Unknown -> null
}

/**
 * Express onramp status → the status plaque under the two-asset block, mapped per the onramp status spec:
 * the in-flight stages collapse to a blue "In progress" loader — [WaitingForPayment][ExpressOnrampStatus.WaitingForPayment]
 * to "Awaiting funds"; [Verifying][ExpressOnrampStatus.Verifying] (KYC) and [Paused][ExpressOnrampStatus.Paused] are amber;
 * [RefundInProgress][ExpressOnrampStatus.RefundInProgress] is an amber "Refunding" loader;
 * [Failed][ExpressOnrampStatus.Failed] is a red [Style.Error] terminal and [Refunded][ExpressOnrampStatus.Refunded] a
 * red [Style.Refunded] one (with the refund glyph); [Expired][ExpressOnrampStatus.Expired] is a grey [Style.Expired]
 * clock terminal; and the [Finished][ExpressOnrampStatus.Finished] success is the only [Style.Success] (auto-collapsed).
 * [Unknown][ExpressOnrampStatus.Unknown] is a terminal client fallback with nothing to show, so it hides the plaque
 * (`null`) — same as the swap variant — rather than a loader that would spin forever once polling stops.
 */
private fun ExpressOnrampStatus.toStatusBannerUM(): TxHistoryDetailsUM.StatusBannerUM? = when (this) {
    ExpressOnrampStatus.Created,
    ExpressOnrampStatus.PaymentProcessing,
    ExpressOnrampStatus.Paid,
    ExpressOnrampStatus.Sending,
    -> loadingBanner(R.string.common_in_progress)
    ExpressOnrampStatus.WaitingForPayment -> loadingBanner(R.string.tx_history_onramp_status_awaiting_funds)
    ExpressOnrampStatus.Verifying -> verificationBanner()
    ExpressOnrampStatus.Paused -> warningBanner(R.string.tx_history_onramp_status_paused)
    ExpressOnrampStatus.RefundInProgress ->
        loadingBanner(R.string.tx_history_onramp_status_refunding, style = Style.Warning)
    ExpressOnrampStatus.Failed -> failedBanner(R.string.tx_history_onramp_status_failed)
    ExpressOnrampStatus.Expired -> expiredBanner(R.string.tx_history_details_status_expired)
    ExpressOnrampStatus.Refunded -> refundedBanner(R.string.tx_history_onramp_status_refunded)
    ExpressOnrampStatus.Finished -> successBanner(R.string.express_exchange_status_bought)
    ExpressOnrampStatus.Unknown -> null
}

/**
 * Label of the bottom CTA for an express swap, or `null` for statuses that need no provider action. The KYC
 * [Verifying][ExpressExchangeStatus.Verifying] state sends the user to verification; the failure terminals send them
 * to the provider (to track / refund). Mirrors the failed/verification banners (the existing express block uses the
 * same per-tx link for both).
 */
@StringRes
private fun ExpressExchangeStatus.providerButtonLabel(): Int? = when (this) {
    ExpressExchangeStatus.Verifying -> R.string.common_go_to_verification
    ExpressExchangeStatus.Failed,
    ExpressExchangeStatus.TxFailed,
    ExpressExchangeStatus.Expired,
    -> R.string.common_go_to_provider
    else -> null
}

/** Label of the bottom CTA for an express onramp, or `null` for statuses that need no provider action. */
@StringRes
private fun ExpressOnrampStatus.providerButtonLabel(): Int? = when (this) {
    ExpressOnrampStatus.Verifying -> R.string.common_go_to_verification
    ExpressOnrampStatus.Failed,
    ExpressOnrampStatus.Expired,
    ExpressOnrampStatus.Refunded,
    -> R.string.common_go_to_provider
    else -> null
}

/** In-progress plaque with the rotating loader; [style] is [Info] (blue) by default, [Warning] for a running refund. */
private fun loadingBanner(@StringRes title: Int, style: Style = Style.Info) = TxHistoryDetailsUM.StatusBannerUM(
    style = style,
    title = resourceReference(title),
    isLoading = true,
)

private fun successBanner(@StringRes title: Int) = TxHistoryDetailsUM.StatusBannerUM(
    style = Style.Success,
    title = resourceReference(title),
    isLoading = false,
)

private fun warningBanner(@StringRes title: Int) = TxHistoryDetailsUM.StatusBannerUM(
    style = Style.Warning,
    title = resourceReference(title),
    isLoading = false,
)

/** Refunded (red) terminal with the refund-arrow glyph. */
private fun refundedBanner(@StringRes title: Int) = TxHistoryDetailsUM.StatusBannerUM(
    style = Style.Refunded,
    title = resourceReference(title),
    isLoading = false,
)

/** Expired (grey) terminal with the clock glyph. */
private fun expiredBanner(@StringRes title: Int) = TxHistoryDetailsUM.StatusBannerUM(
    style = Style.Expired,
    title = resourceReference(title),
    isLoading = false,
)

/** Failure terminal: red plaque with the shared "visit provider to refund" hint. */
private fun failedBanner(@StringRes title: Int = R.string.express_exchange_status_failed) =
    TxHistoryDetailsUM.StatusBannerUM(
        style = Style.Error,
        title = resourceReference(title),
        subtitle = resourceReference(R.string.express_exchange_notification_failed_text),
        isLoading = false,
    )

/** KYC verification: amber plaque with the "visit provider for verification" hint. */
private fun verificationBanner() = TxHistoryDetailsUM.StatusBannerUM(
    style = Style.Warning,
    title = resourceReference(R.string.express_exchange_status_verifying),
    subtitle = resourceReference(R.string.express_exchange_notification_verification_text),
    isLoading = false,
)

// endregion

// region Info rows (provider / rate / network fee)

/**
 * Detail rows of an express op, in order: the [provider] row (its name), the effective-[rateRow] row, then the
 * network-fee row from the matched on-chain leg. Each is dropped when its data is absent — the provider while it is
 * unresolved, the rate while an amount is missing / non-positive (see [swapRateRow] / [onrampRateRow]), the fee while
 * no on-chain leg / fee is present.
 */
private fun ExpressTx.toInfoRows(
    onProviderClick: (() -> Unit)?,
    rateRow: TxHistoryDetailsUM.InfoRowUM?,
    showProviderType: Boolean = false,
): ImmutableList<TxHistoryDetailsUM.InfoRowUM> = buildList {
    provider?.let { add(it.providerRow(onProviderClick, showType = showProviderType)) }
    rateRow?.let { add(it) }
    addAll(txInfo.toInfoRows())
}.toImmutableList()

private fun ExpressProvider.providerRow(onClick: (() -> Unit)?, showType: Boolean): TxHistoryDetailsUM.InfoRowUM =
    TxHistoryDetailsUM.InfoRowUM(
        label = resourceReference(R.string.express_provider),
        value = stringReference(if (showType) "$name ${StringsSigns.DOT} ${type.typeName}" else name),
        // The arrow link affordance is shown only when the row opens the provider page.
        trailingIconRes = onClick?.let { R.drawable.ic_arrow_top_right_24 },
        onClick = onClick,
    )

/** Detail rows pulled from the matched on-chain leg of an express op; empty while the leg has not loaded. */
private fun OnChainTx?.toInfoRows(): ImmutableList<TxHistoryDetailsUM.InfoRowUM> =
    (this as? OnChainTx.BSDK)?.txInfo?.toInfoRows() ?: persistentListOf()

// endregion

// region Rate row

private const val RATE_MAX_DECIMALS = 8

/**
 * Effective swap rate row `1 {base} ≈ {x} {quote}`. The base/quote direction and formatting follow the app-wide
 * [SwapRateFormatter] rules ([REDACTED_TASK_KEY]) so the pair reads the same as on the swap screen. Hidden (`null`) when a leg
 * has no resolved [CryptoCurrency] (the direction rules need the currency type — no way to pick a canonical base) or
 * when an amount is missing or non-positive.
 */
private fun ExchangeTransaction.swapRateRow(): TxHistoryDetailsUM.InfoRowUM? {
    val fromCurrency = fromAsset.cryptoCurrency ?: return null
    val toCurrency = toAsset.cryptoCurrency ?: return null
    val fromAmount = fromAsset.amount.takeIfPositive() ?: return null
    val toAmount = toAsset.amount.takeIfPositive() ?: return null

    val value = SwapRateFormatter.formatRate(
        from = fromCurrency,
        to = toCurrency,
        fromAmount = fromAmount,
        toAmount = toAmount,
    )
    return rateRowUM(value)
}

/**
 * Effective onramp rate row `1 {crypto} ≈ {x} {fiat}`, computed on the fly as `x = fiatPaid / cryptoReceived`. The API's
 * nominal `rate` / `rate_usd` are intentionally ignored to avoid UI drift from hidden fees. Hidden (`null`) when an
 * amount is missing or non-positive.
 */
private fun OnrampTransaction.onrampRateRow(): TxHistoryDetailsUM.InfoRowUM? {
    val fiatPaid = fromFiat.value.takeIfPositive() ?: return null
    val cryptoReceived = toAsset.amount.takeIfPositive() ?: return null
    // Divide at full precision; the fiat formatter then rounds the rate to the currency's display scale.
    val rate = fiatPaid.divide(cryptoReceived, RATE_MAX_DECIMALS, RoundingMode.HALF_UP)
    val cryptoSymbol = toAsset.displaySymbol
    val fiatCode = fromFiat.fiatCode
    val value = rateText(
        base = oneOf(cryptoSymbol),
        quote = rate.format {
            fiat(fiatCurrencyCode = fiatCode, fiatCurrencySymbol = fromFiat.currencySymbol, ignoreSymbolPosition = true)
        },
    )
    return rateRowUM(value)
}

private fun rateRowUM(value: String): TxHistoryDetailsUM.InfoRowUM = TxHistoryDetailsUM.InfoRowUM(
    label = resourceReference(R.string.common_rate),
    value = stringReference(value),
)

/**
 * Leading `1 {symbol}` of the rate, e.g. `1 POL` — number-first, matching the amount legs (the crypto formatter forces a
 * two-decimal minimum, so the literal `1` is built directly rather than via [crypto]).
 */
private fun oneOf(symbol: String): String = "1${StringsSigns.NON_BREAKING_SPACE}$symbol"

private fun rateText(base: String, quote: String): String {
    return "${base.trim()} ${StringsSigns.APPROXIMATE} ${quote.trim()}"
}

private fun BigDecimal?.takeIfPositive(): BigDecimal? = this?.takeIf { it > BigDecimal.ZERO }

// endregion

// region Amount signs

/** Leading sign of the pay-in / "You send" leg: always `−` — the funds left regardless of how the deal ended. */
private const val OUTGOING_SIGN = "${StringsSigns.MINUS} "

/**
 * Leading sign of the payout / "You receive" leg: `~` while in flight (the final received amount is still an estimate),
 * `+` once the funds have settled, and dropped on a failed deal (the amount is then only struck through).
 */
private fun Status.incomingSign(): String = when (this) {
    is Status.Unconfirmed -> "${StringsSigns.TILDE_SIGN} "
    is Status.Confirmed -> "${StringsSigns.PLUS} "
    is Status.Failed -> ""
}

/**
 * Leading sign of an onramp payout leg: `~` while in flight (the received amount is still an estimate), and nothing
 * once it has settled or failed — an onramp buy never carries a `+`/`−` sign (unlike a swap's two-way legs).
 */
private fun Status.onrampIncomingSign(): String = when (this) {
    is Status.Unconfirmed -> "${StringsSigns.TILDE_SIGN} "
    is Status.Confirmed,
    is Status.Failed,
    -> ""
}

// endregion