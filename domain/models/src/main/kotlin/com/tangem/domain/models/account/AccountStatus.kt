package com.tangem.domain.models.account

import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.quote.PriceChange
import com.tangem.domain.models.tokenlist.TokenList
import kotlinx.serialization.Serializable

/**
 * Represents the status of an account
 *
[REDACTED_AUTHOR]
 */
@Serializable
sealed interface AccountStatus {

    /** The account associated with this status */
    val account: Account

    /** Unique identifier of the account */
    val accountId: AccountId
        get() = account.accountId

    /**
     * Represents the status of an account that holds a portfolio of crypto currencies — a [Personal] one of the wallet
     * or a [Joint] one, mirroring [Account.CryptoPortfolio]. Both carry a token list; the flows that spend from the
     * wallet's own keys (swap, send via swap, staking, yield) take [Personal] only
     *
     * @property account        the crypto portfolio account
     * @property tokenList      the list of tokens associated with the account
     * @property priceChangeLce the price change information wrapped in a Lce (Loading, Content, Error) state
     */
    @Serializable
    sealed interface CryptoPortfolio : AccountStatus {

        override val account: Account.CryptoPortfolio
        val tokenList: TokenList
        val priceChangeLce: Lce<Unit, PriceChange>

        fun flattenCurrencies(): List<CryptoCurrencyStatus> {
            return tokenList.flattenCurrencies()
        }

        /** Returns a copy of the status with the given fields replaced, whichever kind it is */
        fun copySealed(
            tokenList: TokenList = this.tokenList,
            priceChangeLce: Lce<Unit, PriceChange> = this.priceChangeLce,
        ): CryptoPortfolio {
            return when (this) {
                is Personal -> copy(tokenList = tokenList, priceChangeLce = priceChangeLce)
                is Joint -> copy(tokenList = tokenList, priceChangeLce = priceChangeLce)
            }
        }
    }

    /** The status of a crypto portfolio account of the wallet itself */
    @Serializable
    data class Personal(
        override val account: Account.Personal,
        override val tokenList: TokenList,
        override val priceChangeLce: Lce<Unit, PriceChange>,
    ) : CryptoPortfolio

    /**
     * The status of a joint account: its currencies belong to a Safe contract, read by the Safe address
     *
     * @property value     the lifecycle state of the account
     * @property tokenList the tokens read by the Safe address; [TokenList.Empty] until the account is active
     */
    @Serializable
    data class Joint(
        override val account: Account.Joint,
        val value: JointAccountStatusValue,
        override val tokenList: TokenList,
        override val priceChangeLce: Lce<Unit, PriceChange>,
    ) : CryptoPortfolio

    @Serializable
    data class Payment(
        override val account: Account.Payment,
        val value: PaymentAccountStatusValue,
    ) : AccountStatus

    @Serializable
    data class Money(
        override val account: Account.Money,
        val value: MoneyAccountStatusValue,
    ) : AccountStatus

    @Serializable
    data class Prediction(
        override val account: Account.Prediction,
        val value: PredictionAccountStatusValue,
    ) : AccountStatus

    @Serializable
    data class Collectibles(
        override val account: Account.Collectibles,
        val value: CollectiblesAccountStatusValue,
    ) : AccountStatus
}

fun Iterable<AccountStatus>.filterCryptoPortfolio(): List<AccountStatus.CryptoPortfolio> {
    return filterIsInstance<AccountStatus.CryptoPortfolio>()
}

fun Sequence<AccountStatus>.filterCryptoPortfolio(): Sequence<AccountStatus.CryptoPortfolio> {
    return filterIsInstance<AccountStatus.CryptoPortfolio>()
}