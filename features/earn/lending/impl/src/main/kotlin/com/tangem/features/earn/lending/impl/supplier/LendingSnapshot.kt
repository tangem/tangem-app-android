package com.tangem.features.earn.lending.impl.supplier

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.lending.impl.provider.LendingMarket
import com.tangem.features.earn.product.api.product.ProductAvailability
import java.math.BigDecimal

/**
 * Everything Lending knows about one wallet.
 *
 * @property assets keyed by the wallet currency (ETH), not by its wrapped token
 * @property source the weakest of the sources
 */
internal data class LendingSnapshot(
    val assets: Map<CryptoCurrency.ID, LendingAssetState>,
    val source: StatusSource,
) {

    companion object {

        /** The wallet holds no currency the provider covers: there is nothing to wait for */
        val NoAssets = LendingSnapshot(assets = emptyMap(), source = StatusSource.ACTUAL)

        /** The producer's fallback: data is missing because of a failure, not because there is no position */
        val Failed = LendingSnapshot(assets = emptyMap(), source = StatusSource.ONLY_CACHE)
    }
}

/**
 * @property address                the network address of the currency, the source of transactions
 * @property balance                network balance without contributions, read from the network status rather than the
 *                                  status list, which already includes the position
 * @property hasPendingTx           an outgoing transaction of the currency is not confirmed yet
 * @property isWrappedTokenInWallet the wrapped token is also in the portfolio, so its balance may count the position twice
 * @property source                 the weakest of the network and the provider sources
 */
internal data class LendingAssetState(
    val asset: LendingAsset,
    val address: String?,
    val balance: BigDecimal?,
    val fiatRate: BigDecimal?,
    val market: LendingMarket,
    val hasPendingTx: Boolean,
    val isWrappedTokenInWallet: Boolean,
    val source: StatusSource,
) {

    /** New money is accepted only by an open market that the provider does not block; the position stays visible */
    val availability: ProductAvailability
        get() = if (market.isOpen && !market.deposit.isDepositBlocked) {
            ProductAvailability.Available
        } else {
            ProductAvailability.Unavailable
        }
}