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
     * Marker for accounts that hold a portfolio of crypto currencies: [CryptoPortfolio] and [Joint].
     * [Payment], [Virtual] and [Prediction] accounts hold no portfolio and are deliberately excluded.
     */
    @Serializable
    sealed interface Portfolio : Account {

        /** Icon representing the account */
        val icon: CryptoPortfolioIcon

        /** Index used for derivation of the account */
        val derivationIndex: DerivationIndex

        /** Set of tokens associated with the account */
        val cryptoCurrencies: List<CryptoCurrency>
    }

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
        override val icon: CryptoPortfolioIcon,
        override val derivationIndex: DerivationIndex,
        override val cryptoCurrencies: List<CryptoCurrency>,
    ) : Portfolio {

        /** Indicates if the account is the main account */
        val isMainAccount: Boolean
            get() = derivationIndex.isMain

        /** Number of tokens in the account */
        val tokensCount: Int
            get() = cryptoCurrencies.size

        /** Number of distinct networks in the account */
        val networksCount: Int
            get() = cryptoCurrencies.map(CryptoCurrency::network).distinct().size

        fun copy(
            accountName: AccountName = this.accountName,
            icon: CryptoPortfolioIcon = this.icon,
            cryptoCurrencies: List<CryptoCurrency> = this.cryptoCurrencies,
        ): CryptoPortfolio {
            return CryptoPortfolio(
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
                cryptoCurrencies: List<CryptoCurrency> = emptyList(),
            ): Either<Error, CryptoPortfolio> {
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
                cryptoCurrencies: List<CryptoCurrency> = emptyList(),
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
                cryptoCurrencies: List<CryptoCurrency> = emptyList(),
            ): CryptoPortfolio {
                val derivationIndex = DerivationIndex.Main

                return CryptoPortfolio(
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

    @Serializable
    data class Payment private constructor(
        override val accountId: AccountId,
    ) : Account {
        override val accountName: AccountName.Custom = AccountName.Custom("Payment").getOrElse {
            error("Can not create account name for Payment account with userWalletId = ${accountId.userWalletId}")
        }

        companion object {
            operator fun invoke(userWalletId: UserWalletId): Payment {
                return Payment(accountId = AccountId.forPaymentAccount(userWalletId = userWalletId))
            }
        }
    }

    @Serializable
    data class Virtual private constructor(
        override val accountId: AccountId,
    ) : Account {
        override val accountName: AccountName.Custom = AccountName.Custom("Virtual").getOrElse {
            error("Can not create account name for Virtual account with userWalletId = ${accountId.userWalletId}")
        }

        companion object {
            operator fun invoke(userWalletId: UserWalletId): Virtual {
                return Virtual(accountId = AccountId.forVirtualAccount(userWalletId = userWalletId))
            }
        }
    }

    @Serializable
    data class Prediction private constructor(
        override val accountId: AccountId,
    ) : Account {
        override val accountName: AccountName.Custom = AccountName.Custom("Prediction").getOrElse {
            error("Can not create account name for Prediction account with userWalletId = ${accountId.userWalletId}")
        }

        companion object {
            operator fun invoke(userWalletId: UserWalletId): Prediction {
                return Prediction(accountId = AccountId.forPredictionAccount(userWalletId = userWalletId))
            }
        }
    }

    /**
     * Represents the wallet's own row of a joint (Safe multisig) account
     *
     * @property accountId        unique identifier of the account, computed and returned by the backend
     * @property accountName      name of the account, shared by all participants and fixed at creation
     * @property icon             icon representing the account, shared and fixed at creation
     * @property derivationIndex  index of the owner key derivation; an index space independent from
     * [CryptoPortfolio] accounts
     * @property cryptoCurrencies tokens associated with the account. They are never spendable through the regular
     * send/swap flows: a joint account is a Safe contract, not an EOA — see `AccountList.flattenCurrencies`
     */
    @Serializable
    data class Joint private constructor(
        override val accountId: AccountId,
        override val accountName: AccountName,
        override val icon: CryptoPortfolioIcon,
        override val derivationIndex: DerivationIndex,
        override val cryptoCurrencies: List<CryptoCurrency>,
    ) : Portfolio {

        /**
         * Represents possible errors when creating a joint account
         */
        @Serializable
        sealed interface Error {

            /** Error indicating that the account name is invalid */
            @Serializable
            data class AccountNameError(val cause: AccountName.Error) : Error

            /** Error indicating that the derivation index is invalid */
            @Serializable
            data class DerivationIndexError(val cause: DerivationIndex.Error) : Error
        }

        companion object {

            /**
             * Constructor for creating a [Joint] instance from raw backend values
             *
             * @param accountId        unique identifier of the account
             * @param name             name of the account
             * @param icon             icon representing the account
             * @param derivationIndex  index of the owner key derivation
             * @param cryptoCurrencies tokens associated with the account
             */
            operator fun invoke(
                accountId: AccountId,
                name: String,
                icon: CryptoPortfolioIcon,
                derivationIndex: Int,
                cryptoCurrencies: List<CryptoCurrency> = emptyList(),
            ): Either<Error, Joint> = either {
                val accountName = AccountName(value = name).getOrElse {
                    raise(Error.AccountNameError(cause = it))
                }

                val index = DerivationIndex(value = derivationIndex).getOrElse {
                    raise(Error.DerivationIndexError(cause = it))
                }

                invoke(
                    accountId = accountId,
                    accountName = accountName,
                    icon = icon,
                    derivationIndex = index,
                    cryptoCurrencies = cryptoCurrencies,
                )
            }

            /**
             * Constructor for creating a [Joint] instance from already validated values
             *
             * @param accountId        unique identifier of the account
             * @param accountName      name of the account
             * @param icon             icon representing the account
             * @param derivationIndex  index of the owner key derivation
             * @param cryptoCurrencies tokens associated with the account
             */
            @Suppress("LongParameterList")
            operator fun invoke(
                accountId: AccountId,
                accountName: AccountName,
                icon: CryptoPortfolioIcon,
                derivationIndex: DerivationIndex,
                cryptoCurrencies: List<CryptoCurrency> = emptyList(),
            ): Joint {
                return Joint(
                    accountId = accountId,
                    accountName = accountName,
                    icon = icon,
                    derivationIndex = derivationIndex,
                    cryptoCurrencies = cryptoCurrencies,
                )
            }
        }
    }
}

val Account.derivationIndex: DerivationIndex?
    get() = (this as? Account.Portfolio)?.derivationIndex