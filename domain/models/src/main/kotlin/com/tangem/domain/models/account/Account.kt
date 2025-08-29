package com.tangem.domain.models.account

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.tangem.domain.models.account.Account.CryptoPortfolio.Error.AccountNameError
import com.tangem.domain.models.account.Account.CryptoPortfolio.Error.DerivationIndexError
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
    val accountName: AccountName

    /** The identifier of the user wallet associated with the account */
    val userWalletId: UserWalletId
        get() = accountId.userWalletId

    /**
     * Represents a crypto portfolio account
     *
     * @property accountId        unique identifier of the account
     * @property accountName      name of the account
     * @property icon             icon representing the account
     * @property derivationIndex  index used for derivation of the account
     * @property cryptoCurrencies set of tokens associated with the account
     */
    @Serializable
    data class CryptoPortfolio private constructor(
        override val accountId: AccountId,
        override val accountName: AccountName,
        val icon: CryptoPortfolioIcon,
        val derivationIndex: DerivationIndex,
        val cryptoCurrencies: Set<CryptoCurrency>,
    ) : Account {

        /** Indicates if the account is the main account */
        val isMainAccount: Boolean
            get() = derivationIndex.isMain

        /** Number of tokens in the account */
        val tokensCount: Int
            get() = cryptoCurrencies.size

        /** Number of distinct networks in the account */
        val networksCount: Int
            get() = cryptoCurrencies.map(CryptoCurrency::network).distinct().size

        fun copy(accountName: AccountName = this.accountName, icon: CryptoPortfolioIcon = this.icon): CryptoPortfolio {
            return CryptoPortfolio(
                accountId = this.accountId,
                accountName = accountName,
                icon = icon,
                derivationIndex = this.derivationIndex,
                cryptoCurrencies = this.cryptoCurrencies,
            )
        }

        /**
         * Represents possible errors when creating a crypto portfolio account
         */
        @Serializable
        sealed interface Error {

            /** Error indicating that the account name is blank */
            @Serializable
            data class AccountNameError(val cause: AccountName.Error) : Error

            /** Error indicating that the derivation index is negative */
            @Serializable
            data class DerivationIndexError(val cause: DerivationIndex.Error) : Error
        }

        companion object {

            /**
             * Constructor for creating a [CryptoPortfolio] instance
             *
             * @param accountId        unique identifier of the account
             * @param name      name of the account
             * @param icon             icon representing the account
             * @param derivationIndex  index used for derivation of the account
             * @param cryptoCurrencies set of tokens associated with the account
             */
            operator fun invoke(
                accountId: AccountId,
                name: String,
                icon: CryptoPortfolioIcon,
                derivationIndex: Int,
                cryptoCurrencies: Set<CryptoCurrency> = emptySet(),
            ): Either<Error, CryptoPortfolio> {
                return either {
                    val accountName = AccountName(value = name).getOrElse {
                        raise(AccountNameError(cause = it))
                    }

                    val derivationIndex = DerivationIndex(value = derivationIndex).getOrElse {
                        raise(DerivationIndexError(cause = it))
                    }

                    invoke(
                        accountId = accountId,
                        accountName = accountName,
                        icon = icon,
                        derivationIndex = derivationIndex,
                        cryptoCurrencies = cryptoCurrencies,
                    )
                }
            }

            /**
             * Constructor for creating a [CryptoPortfolio] instance
             *
             * @param accountId        unique identifier of the account
             * @param accountName      name of the account
             * @param icon             icon representing the account
             * @param derivationIndex  index used for derivation of the account
             * @param cryptoCurrencies set of tokens associated with the account
             */
            @Suppress("LongParameterList")
            operator fun invoke(
                accountId: AccountId,
                accountName: AccountName,
                icon: CryptoPortfolioIcon,
                derivationIndex: DerivationIndex,
                cryptoCurrencies: Set<CryptoCurrency> = emptySet(),
            ): CryptoPortfolio {
                return CryptoPortfolio(
                    accountId = accountId,
                    accountName = accountName,
                    icon = icon,
                    derivationIndex = derivationIndex,
                    cryptoCurrencies = cryptoCurrencies,
                )
            }

            /**
             * Creates a main account for the given user wallet ID
             *
             * @param userWalletId     the ID of the user wallet
             * @param cryptoCurrencies set of tokens associated with the account
             */
            fun createMainAccount(
                userWalletId: UserWalletId,
                cryptoCurrencies: Set<CryptoCurrency> = emptySet(),
            ): CryptoPortfolio {
                val derivationIndex = DerivationIndex.Main

                return CryptoPortfolio(
                    accountId = AccountId.forCryptoPortfolio(
                        userWalletId = userWalletId,
                        derivationIndex = derivationIndex,
                    ),
                    accountName = AccountName.Main,
                    icon = CryptoPortfolioIcon.ofMainAccount(userWalletId),
                    derivationIndex = derivationIndex,
                    cryptoCurrencies = cryptoCurrencies,
                )
            }
        }
    }
}