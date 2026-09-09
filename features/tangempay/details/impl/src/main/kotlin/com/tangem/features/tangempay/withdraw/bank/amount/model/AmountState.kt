package com.tangem.features.tangempay.withdraw.bank.amount.model

import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.pay.model.bankofframp.AmountLimits
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawAmountResult
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawSource
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal
import java.util.Currency

internal enum class AmountSheet { TOKENS, ROUTES }

/** Raw inputs of the Amount step; [resolve] turns them into the selection the UI and the Continue action work with. */
internal data class AmountState(
    val options: BankOfframpOptions? = null,
    val networks: List<PaymentNetworkStatus>? = null,
    val currencyCode: String? = null,
    val value: String = "",
    val selectedSourceId: String? = null,
    val selectedRail: BankRail? = null,
    val sheet: AmountSheet? = null,
) {

    fun resolve(draft: BankWithdrawDraft): AmountSelection? {
        val options = options ?: return null
        val networks = networks ?: return null
        val destinations = options.destinations
        val preferredRail = selectedRail ?: draft.destination?.rail
        val destination = destinations.firstOrNull { it.rail == preferredRail }
            ?: destinations.firstOrNull()
            ?: return null
        val sources = BankWithdrawSourceResolver.resolve(networks = networks, sources = options.sources)
        val preferredSourceId = selectedSourceId ?: draft.source?.id
        val source = sources.firstOrNull { it.id == preferredSourceId }
            ?: BankWithdrawSourceResolver.default(sources)
        val amount = value.parseBigDecimalOrNull()
        return AmountSelection(
            currency = Currency.getInstance(currencyCode ?: destination.fee.currency),
            limits = options.amountLimits,
            sources = sources,
            source = source,
            destinations = destinations,
            destination = destination,
            amount = amount,
            validation = BankWithdrawAmountValidator.validate(
                amount = amount,
                limits = options.amountLimits,
                balance = source?.fiatBalance.orZero(),
                fee = destination.fee.amount,
            ),
        )
    }
}

internal data class AmountSelection(
    val currency: Currency,
    val limits: AmountLimits,
    val sources: List<BankWithdrawSource>,
    val source: BankWithdrawSource?,
    val destinations: List<OfframpDestination>,
    val destination: OfframpDestination,
    val amount: BigDecimal?,
    val validation: BankWithdrawAmountValidator.Result,
) {

    val isContinueEnabled: Boolean
        get() = validation == BankWithdrawAmountValidator.Result.Valid && source?.isAvailable == true

    fun toResult(): BankWithdrawAmountResult? {
        val amount = amount ?: return null
        val source = source ?: return null
        return if (isContinueEnabled) BankWithdrawAmountResult(amount, source, destination) else null
    }
}