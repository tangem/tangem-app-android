package com.tangem.domain.pay

import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import java.math.BigDecimal

/**
 * Factory that builds the [CryptoCurrency.Token] used by Tangem Pay (USDC on Polygon) for a given user wallet.
 *
 * Replaces the deprecated `TangemPayCryptoCurrencyFactory`: callers no longer pass the chain id explicitly —
 * the underlying network is resolved from the wallet.
 */
interface TangemPayCurrencyFactory {

    /**
     * Builds the Tangem Pay token bound to the network of the wallet identified by [userWalletId].
     *
     * @throws IllegalStateException if no wallet with [userWalletId] is currently loaded.
     */
    fun create(userWalletId: UserWalletId): CryptoCurrency.Token
    fun createVirtualAccountToken(userWalletId: UserWalletId): CryptoCurrency.Token

    /**
     * Builds the domain multichain networks from the raw [networks] delivered by `customer/me`.
     * Each [CustomerInfo.NetworkInfo] becomes a [PaymentNetworkStatus] tagged by its status:
     *  - ENABLED -> [PaymentNetworkStatus.Available] (per-token currency + balance + receive address, using [fiatRate]),
     *  - NOT_ISSUED -> [PaymentNetworkStatus.NotIssued] (currencies only),
     *  - DISABLED -> [PaymentNetworkStatus.Disabled] (currencies only).
     *
     * Networks whose blockchain cannot be resolved are skipped. Returns empty when [networks] is empty.
     */
    fun createNetworkStatuses(
        userWalletId: UserWalletId,
        networks: List<CustomerInfo.NetworkInfo>,
        fiatRate: BigDecimal?,
    ): List<PaymentNetworkStatus>

    /** Hardcoded token metadata for the Tangem Pay currency (USDC on Polygon). */
    companion object {
        /** CoinGecko-style raw id used to query quotes for the Tangem Pay token. */
        val TOKEN_ID = CryptoCurrency.RawID("usd-coin")
        const val TOKEN_NAME = "USDC"
        const val TOKEN_CONTRACT_ADDRESS = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"
        const val TOKEN_DECIMALS = 6
    }
}