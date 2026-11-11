package com.tangem.domain.tangempay.model.bankofframp

import com.tangem.domain.tangempay.model.AccountFee
import java.math.BigDecimal
import java.math.RoundingMode

/** `GET /account/bank-offramp/options`: token sources and bank rails with their fees and ETA. */
data class BankOfframpOptions(
    val sources: List<OfframpSource>,
    val destinations: List<OfframpDestination>,
)

data class OfframpSource(
    val networkName: String,
    val isTestnet: Boolean,
    val chainId: Long,
    val tokens: List<OfframpToken>,
)

data class OfframpToken(
    val symbol: String,
    val contractAddress: String,
)

data class OfframpDestination(
    val rail: BankRail,
    val currency: String?,
    val fee: OfframpFee,
    val eta: OfframpEta,
)

/** [name] is how the backend labels the rail ("ACH", "FedWire"); the app names it itself when it is absent. */
data class OfframpFee(
    val name: String?,
    val calculationType: AccountFee.CalculationType,
    val amount: BigDecimal,
    val currency: String,
    val description: String?,
) {

    fun amountFor(transferAmount: BigDecimal): BigDecimal = when (calculationType) {
        AccountFee.CalculationType.FIXED -> amount
        AccountFee.CalculationType.PERCENTAGE -> transferAmount.multiply(amount)
            .movePointLeft(PERCENT_SCALE)
            .setScale(CENT_SCALE, RoundingMode.HALF_UP)
    }

    private companion object {
        const val PERCENT_SCALE = 2
        const val CENT_SCALE = 2
    }
}

data class OfframpEta(
    val minDays: Int,
    val maxDays: Int,
)