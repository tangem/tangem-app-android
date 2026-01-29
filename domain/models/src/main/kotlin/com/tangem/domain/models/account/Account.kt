package com.tangem.domain.models.account

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.tangem.domain.models.account.Account.Crypto.Portfolio.Error.AccountNameError
import com.tangem.domain.models.account.Account.Crypto.Portfolio.Error.DerivationIndexError
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

    /** Index used for derivation of the account */
    val derivationIndex: DerivationIndex

    /** The identifier of the user wallet associated with the account */
    val userWalletId: UserWalletId
        get() = accountId.userWalletId

    @Serializable
    sealed interface Crypto : Account {

        /** Icon representing the account */
        val icon: CryptoPortfolioIcon

        /** Set of tokens associated with the account */
        val cryptoCurrencies: Set<CryptoCurrency>

        /** Indicates if the account is the main account */
        val isMainAccount: Boolean
            get() = derivationIndex.isMain

        /** Number of tokens in the account */
        val tokensCount: Int
            get() = cryptoCurrencies.size

        /** Number of distinct networks in the account */
        val networksCount: Int
            get() = cryptoCurrencies.map(CryptoCurrency::network).distinct().size

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
        data class Portfolio private constructor(
            override val accountId: AccountId,
            override val accountName: AccountName,
            override val icon: CryptoPortfolioIcon,
            override val derivationIndex: DerivationIndex,
            override val cryptoCurrencies: Set<CryptoCurrency>,
        ) : Crypto {

            fun copy(
                accountName: AccountName = this.accountName,
                icon: CryptoPortfolioIcon = this.icon,
                cryptoCurrencies: Set<CryptoCurrency> = this.cryptoCurrencies,
            ): Portfolio {
                return Portfolio(
                    accountId = this.accountId,
                    accountName = accountName,
                    icon = icon,
                    derivationIndex = this.derivationIndex,
                    cryptoCurrencies = cryptoCurrencies,
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
                ): Either<Error, Portfolio> {
                    return either {
                        val accountName = AccountName(value = name).getOrElse {
                            raise(AccountNameError(cause = it))
                        }

                        val index = DerivationIndex(value = derivationIndex).getOrElse {
                            raise(DerivationIndexError(cause = it))
                        }

                        invoke(
                            accountId = accountId,
                            accountName = accountName,
                            icon = icon,
                            derivationIndex = index,
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
                ): Portfolio {
                    return Portfolio(
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
                ): Portfolio {
                    val derivationIndex = DerivationIndex.Main

                    return Portfolio(
                        accountId = AccountId.forCryptoPortfolio(
                            userWalletId = userWalletId,
                            derivationIndex = derivationIndex,
                        ),
                        accountName = AccountName.DefaultMain,
                        icon = CryptoPortfolioIcon.ofMainAccount(userWalletId),
                        derivationIndex = derivationIndex,
                        cryptoCurrencies = cryptoCurrencies,
                    )
                }
            }
        }
    }

    @Serializable
    data class Payment(
        override val accountId: AccountId,
        override val accountName: AccountName,
        val cryptoCurrencies: Set<CryptoCurrency>,
    ) : Account {

        override val derivationIndex: DerivationIndex = DerivationIndex.Payment

        init {
            error("Not yet implemented")
        }
    }
}