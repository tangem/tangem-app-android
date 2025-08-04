package com.tangem.domain.models.account

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account.CryptoPortfolio.Error.AccountNameError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

/**
 * Represents an account
 *
[REDACTED_AUTHOR]
 */
@Serializable
sealed interface Account {

    /** Unique identifier of the account */
    val accountId: AccountId

    /** Name of the account */
    val name: AccountName

    /** The identifier of the user wallet associated with the account */
    val userWalletId: UserWalletId
        get() = accountId.userWalletId

    /**
     * Represents a crypto portfolio account
     *
     * @property accountId          unique identifier of the account
     * @property name               name of the account
     * @property icon               icon representing the account
     * @property derivationIndex    index used for derivation of the account
     * @property isArchived         indicates whether the account is archived
     * @property cryptoCurrencyList list of tokens associated with the account
     */
    @Serializable
    data class CryptoPortfolio private constructor(
        override val accountId: AccountId,
        override val name: AccountName,
        val icon: CryptoPortfolioIcon,
        val derivationIndex: Int,
        val isArchived: Boolean,
        val cryptoCurrencyList: CryptoCurrencyList,
    ) : Account {

        /** Indicates if the account is the main account */
        val isMainAccount: Boolean
            get() = derivationIndex == 0

        /** Number of tokens in the account */
        val tokensCount: Int
            get() = cryptoCurrencyList.currencies.size

        /** Number of distinct networks in the account */
        val networksCount: Int
            get() = cryptoCurrencyList.currencies.map(CryptoCurrency::network).distinct().size

        /**
         * Represents a list of tokens in the account
         *
         * @property currencies set of cryptocurrencies in the account
         * @property sortType   sorting type for the tokens
         * @property groupType  grouping type for the tokens
         */
        @Serializable
        data class CryptoCurrencyList(
            val currencies: Set<CryptoCurrency>,
            val sortType: TokensSortType,
            val groupType: TokensGroupType,
        )

        /**
         * Represents possible errors when creating a crypto portfolio account
         */
        @Serializable
        sealed interface Error {

            /** Error indicating that the account name is blank */
            @Serializable
            data class AccountNameError(val cause: AccountName.Error) : Error {
                override fun toString(): String = cause.toString()
            }

            /** Error indicating that the derivation index is negative */
            @Serializable
            data object NegativeDerivationIndex : Error {
                override fun toString(): String = "${this::class.simpleName}: Derivation index must be non-negative"
            }
        }

        companion object {

            /**
             * Constructor for creating a [CryptoPortfolio] instance
             *
             * @param accountId          unique identifier of the account
             * @param name               name of the account
             * @param accountIcon        icon representing the account
             * @param derivationIndex    index used for derivation of the account
             * @param isArchived         indicates whether the account is archived
             * @param cryptoCurrencyList list of tokens associated with the account
             */
            @Suppress("LongParameterList")
            operator fun invoke(
                accountId: AccountId,
                name: String,
                accountIcon: CryptoPortfolioIcon,
                derivationIndex: Int,
                isArchived: Boolean,
                cryptoCurrencyList: CryptoCurrencyList,
            ): Either<Error, CryptoPortfolio> {
                return either {
                    val accountName = AccountName(name).mapLeft(::AccountNameError).bind()

                    ensure(derivationIndex >= 0) { Error.NegativeDerivationIndex }

                    CryptoPortfolio(
                        accountId = accountId,
                        name = accountName,
                        icon = accountIcon,
                        derivationIndex = derivationIndex,
                        isArchived = isArchived,
                        cryptoCurrencyList = cryptoCurrencyList,
                    )
                }
            }
        }
    }
}