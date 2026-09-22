package com.tangem.domain.pay.model.bankofframp

import java.math.BigDecimal

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
    val fee: OfframpFee,
    val eta: OfframpEta,
)

/** [name] is how the backend labels the rail ("ACH", "FedWire"); the app names it itself when it is absent. */
data class OfframpFee(
    val name: String?,
    val amount: BigDecimal,
    val currency: String,
    val description: String?,
)

data class OfframpEta(
    val minDays: Int,
    val maxDays: Int,
)