package com.tangem.features.tangempay.withdraw.bank.preview.state

import androidx.annotation.StringRes
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.default
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.simple
import com.tangem.domain.tangempay.model.bankofframp.BankAccountDetails
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.Beneficiary
import com.tangem.domain.tangempay.model.bankofframp.OfframpDestination
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.amount.state.feeText
import com.tangem.features.tangempay.withdraw.bank.amount.state.formatFiat
import com.tangem.features.tangempay.withdraw.bank.amount.state.toEtaReference
import com.tangem.features.tangempay.withdraw.bank.amount.state.toTitleReference
import com.tangem.features.tangempay.withdraw.bank.amount.state.totalFee
import com.tangem.features.tangempay.withdraw.bank.amount.state.totalFeeText
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormFields
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawFromFactory
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderFactory
import com.tangem.features.tangempay.withdraw.bank.model.isRailLocked
import com.tangem.features.tangempay.withdraw.bank.model.recipientDetails
import com.tangem.features.tangempay.withdraw.bank.model.toFormValues
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.preview.model.LockedQuote
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewState
import com.tangem.features.tangempay.withdraw.bank.preview.model.toOrderRequest
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.AmountCardUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.ConversionUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.DetailRowUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.FeeRowUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.RateLockUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.RecipientCardUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.RouteUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM.TransferUM
import com.tangem.utils.StringsSigns.TILDE_SIGN
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

private const val RATE_DECIMALS = 2
private const val SECONDS_PER_MINUTE = 60

private val ACCOUNT_FIELDS = setOf(
    BankWithdrawFormField.AccountNumber,
    BankWithdrawFormField.RoutingNumber,
    BankWithdrawFormField.Clabe,
)

internal class TangemPayBankWithdrawPreviewUMFactory(
    sourceAccount: BankWithdrawSourceAccount,
    private val callbacks: Callbacks,
) {

    private val iconConverter = CryptoCurrencyToIconStateConverter(shouldGrayscaleTestnet = false)

    private val from = BankWithdrawFromFactory.create(sourceAccount)

    private val isNetworkFeeCharged = when (sourceAccount) {
        is BankWithdrawSourceAccount.Payment -> false
        is BankWithdrawSourceAccount.Money -> true
    }

    fun create(draft: BankWithdrawDraft, state: PreviewState): TangemPayBankWithdrawPreviewUM {
        val failure = state.failure
        if (failure != null) return createFailed(failure)
        return TangemPayBankWithdrawPreviewUM.Summary(
            header = BankWithdrawHeaderFactory.create(
                draft = draft,
                step = TangemPayBankWithdrawInnerRoute.Preview,
                onCloseClick = callbacks.onCloseClick,
            ),
            transfer = createTransfer(draft = draft, state = state),
            recipientCards = draft.recipientDetails()?.let { details ->
                createRecipientCards(
                    details = details,
                    displayNames = draft.newRecipient.displayNames,
                    isEditable = draft.recipient == null && !state.isSending,
                )
            } ?: persistentListOf(),
            isSending = state.isSending,
            isSendEnabled = draft.toOrderRequest() != null && (!draft.isConverting() || state.quote != null),
            sendText = createSendText(draft),
            isColdWallet = state.isColdWallet,
            onBackClick = callbacks.onBackClick,
            onCloseClick = callbacks.onCloseClick,
            onSendClick = callbacks.onSendClick,
        )
    }

    private fun createTransfer(draft: BankWithdrawDraft, state: PreviewState): TransferUM? {
        val amount = draft.amount ?: return null
        val source = draft.source ?: return null
        val destination = draft.destination ?: return null
        val total = draft.totalText() ?: return null
        val currency = Currency.getInstance(destination.fee.currency)
        val railTitle = destination.toTitleReference()
        val fee = destination.totalFeeText(draft.networkFee)
        val isSending = state.isSending
        val isConverting = draft.isConverting()
        val conversionFee = if (isConverting) ConversionFee.of(destination, amount, state.quote) else null
        return TransferUM(
            amount = AmountCardUM(
                from = from,
                balance = source.fiatBalance.formatFiat(currency).orMaskWithStars(state.isBalanceHidden),
                amount = amount.formatFiat(currency),
                icon = iconConverter.convert(source.token),
                symbol = source.token.symbol,
                onClick = if (isSending) {
                    null
                } else {
                    { callbacks.onEditClick(BankWithdrawEditTarget.Amount) }
                },
            ),
            route = RouteUM(
                rail = destination.rail,
                title = railTitle,
                eta = destination.toEtaReference(),
                fee = if (isConverting) {
                    conversionFee?.feesTotalText(networkFee = draft.networkFee, currency = currency)
                } else {
                    destination.feeText(amount = amount, networkFee = draft.networkFee)
                },
                onClick = if (isSending || draft.isRailLocked) null else callbacks.onRailClick,
            ),
            conversion = if (isConverting) {
                createConversion(
                    destination = destination,
                    networkFee = draft.networkFee,
                    networkName = source.network.name,
                    railTitle = railTitle,
                    conversionFee = conversionFee,
                    lockedQuote = state.quote,
                )
            } else {
                null
            },
            sendingSummary = if (isNetworkFeeCharged) {
                resourceReference(R.string.money_account_send_fees_footer, wrappedList(railTitle))
            } else {
                resourceReference(
                    R.string.tangempay_withdraw_bank_sending_summary,
                    wrappedList(total, railTitle, fee),
                )
            },
        )
    }

    @Suppress("LongParameterList")
    private fun createConversion(
        destination: OfframpDestination,
        networkFee: BigDecimal,
        networkName: String,
        railTitle: TextReference,
        conversionFee: ConversionFee?,
        lockedQuote: LockedQuote?,
    ): ConversionUM {
        val currency = Currency.getInstance(destination.fee.currency)
        val quote = lockedQuote?.quote
        return ConversionUM(
            rate = quote?.let { terms ->
                val rate = terms.exchangeRate.format { simple(decimals = RATE_DECIMALS).default() }
                "1 ${currency.currencyCode} = $rate ${terms.destinationCurrency}"
            },
            recipientGets = quote?.let { it.destinationAmount.formatIn(it.destinationCurrency) },
            rateLock = lockedQuote.toRateLock(),
            fees = persistentListOf(
                FeeRowUM(
                    label = combinedReference(
                        railTitle,
                        stringReference(" ${destination.totalFeeText(networkFee = BigDecimal.ZERO)}"),
                    ),
                    value = conversionFee?.let { it.railFee.formatFiat(currency).approximateUnless(it.isQuoted) },
                ),
                FeeRowUM(
                    label = resourceReference(R.string.money_account_send_fees_network, wrappedList(networkName)),
                    value = networkFee.formatFiat(currency).approximateUnless(isExact = false),
                ),
            ),
            total = conversionFee?.feesTotalText(networkFee = networkFee, currency = currency),
        )
    }

    private fun LockedQuote?.toRateLock(): RateLockUM = when {
        this == null -> RateLockUM.Loading
        secondsLeft == null -> RateLockUM.Hidden
        else -> RateLockUM.Countdown(
            text = resourceReference(
                id = R.string.money_account_send_rate_lock,
                formatArgs = wrappedList(secondsLeft.toMinutesAndSeconds()),
            ),
        )
    }

    private fun createSendText(draft: BankWithdrawDraft): TextReference {
        val total = draft.totalText()
        return if (isNetworkFeeCharged && total != null) {
            resourceReference(R.string.money_account_send_pay_button, wrappedList(total))
        } else {
            resourceReference(R.string.common_send)
        }
    }

    private fun BankWithdrawDraft.isConverting(): Boolean = isNetworkFeeCharged && destination?.rail == BankRail.SPEI

    private fun BankWithdrawDraft.totalText(): String? {
        val amount = amount ?: return null
        val destination = destination ?: return null
        return (amount + destination.totalFee(networkFee)).formatFiat(Currency.getInstance(destination.fee.currency))
    }

    private fun createRecipientCards(
        details: BankAccountDetails,
        displayNames: Map<BankWithdrawFormField, String>,
        isEditable: Boolean,
    ): ImmutableList<RecipientCardUM> {
        val values = details.toFormValues()
        val recipientFields = BankWithdrawFormFields.recipientDetails(
            isBusiness = details.beneficiary is Beneficiary.Business,
            rail = details.rail,
        )
        // A CLABE already names the bank, so a SPEI recipient gets no bank card even if the backend keeps a bank name.
        val bankCard = (BankWithdrawFormFields.bankDetails to BankWithdrawEditTarget.BankDetails)
            .takeIf { details.rail != BankRail.SPEI }
        val cards = listOfNotNull(
            recipientFields.filter { it in ACCOUNT_FIELDS } to BankWithdrawEditTarget.RecipientDetails,
            recipientFields.filterNot { it in ACCOUNT_FIELDS } to BankWithdrawEditTarget.RecipientDetails,
            bankCard,
        )
        return cards.mapNotNull { (fields, target) ->
            val rows = fields.mapNotNull { field ->
                // Only a recipient added here carries names; a saved one comes back with codes and keeps them.
                val shown = displayNames[field]?.takeIf(String::isNotBlank) ?: values[field]
                // A saved recipient can come back missing a value, and an empty row reads as missing data.
                shown
                    ?.takeIf(String::isNotBlank)
                    ?.let { value -> DetailRowUM(label = field.label, value = value) }
            }
            // A saved recipient can miss every value of a card, and a rowless card only adds a gap to the screen.
            if (rows.isEmpty()) return@mapNotNull null
            RecipientCardUM(
                rows = rows.toImmutableList(),
                onClick = if (isEditable) {
                    { callbacks.onEditClick(target) }
                } else {
                    null
                },
            )
        }.toImmutableList()
    }

    private fun createFailed(failure: PreviewFailure): TangemPayBankWithdrawPreviewUM.Failed = when (failure) {
        PreviewFailure.RecipientRegion -> unavailable(R.string.tangempay_withdraw_bank_error_recipient_region)
        PreviewFailure.SenderRegion -> unavailable(R.string.tangempay_withdraw_bank_error_sender_region)
        PreviewFailure.Business -> unavailable(R.string.tangempay_withdraw_bank_error_business)
        PreviewFailure.UnsupportedConfiguration -> unavailable(R.string.tangempay_withdraw_bank_error_region_title)
        PreviewFailure.General -> general(onRetryClick = callbacks.onRetryClick)
        PreviewFailure.TransferOutcomeUnknown -> general(onRetryClick = null)
    }

    private fun general(onRetryClick: (() -> Unit)?) = TangemPayBankWithdrawPreviewUM.Failed(
        title = resourceReference(R.string.tangempay_withdraw_bank_error_general_title),
        icon = TangemStatusIcon.Kind.Warning,
        onCloseClick = callbacks.onCloseClick,
        onRetryClick = onRetryClick,
    )

    private fun unavailable(@StringRes titleRes: Int) = TangemPayBankWithdrawPreviewUM.Failed(
        title = resourceReference(titleRes),
        icon = TangemStatusIcon.Kind.HeartBroken,
        onCloseClick = callbacks.onCloseClick,
        onRetryClick = null,
    )

    private data class ConversionFee(val railFee: BigDecimal, val isQuoted: Boolean) {

        fun feesTotalText(networkFee: BigDecimal, currency: Currency): String =
            (railFee + networkFee).formatFiat(currency).approximateUnless(isQuoted)

        companion object {
            fun of(destination: OfframpDestination, amount: BigDecimal, quote: LockedQuote?): ConversionFee? {
                val terms = quote?.quote ?: return null
                val quotedFee = terms.feeAmount
                return ConversionFee(
                    railFee = quotedFee ?: destination.fee.amountFor(amount),
                    isQuoted = quotedFee != null,
                )
            }
        }
    }

    data class Callbacks(
        val onBackClick: () -> Unit,
        val onCloseClick: () -> Unit,
        val onSendClick: () -> Unit,
        val onRetryClick: () -> Unit,
        val onEditClick: (BankWithdrawEditTarget) -> Unit,
        val onRailClick: () -> Unit,
    )
}

private fun String.approximateUnless(isExact: Boolean): String = if (isExact) this else TILDE_SIGN + this

private fun BigDecimal.formatIn(currencyCode: String): String =
    runCatching { Currency.getInstance(currencyCode) }.getOrNull()?.let { formatFiat(it) }
        ?: "${format { simple(decimals = RATE_DECIMALS).default() }} $currencyCode"

private fun Long.toMinutesAndSeconds(): String =
    String.format(Locale.ROOT, "%02d:%02d", this / SECONDS_PER_MINUTE, this % SECONDS_PER_MINUTE)