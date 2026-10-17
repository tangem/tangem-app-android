package com.tangem.domain.jointaccount.gas

import arrow.core.Either
import com.tangem.domain.jointaccount.model.SafeGasToken
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network

/**
 * Prices one given gas token — the one the user picked on send, or the one a co-signed operation names — and is not
 * the list the user picks from. The creation and the co-signing paths price it from the same source, so both apply
 * the same tolerance.
 */
fun interface JointGasTokenResolver {

    /**
     * `null` when [address] is neither the coin nor a Smart Gas token of [network].
     *
     * @param rawCurrencyId the quote id of the account's own token with [address]: the Smart Gas list carries
     * contracts only, so the rate is read for the token the account holds
     */
    suspend fun resolve(
        network: Network,
        address: String,
        rawCurrencyId: CryptoCurrency.RawID?,
    ): Either<Throwable, SafeGasToken?>
}