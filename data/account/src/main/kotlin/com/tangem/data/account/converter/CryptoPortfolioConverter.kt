package com.tangem.data.account.converter

import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.converter.TwoWayConverter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Converts a [WalletAccountDTO] to an [Account.CryptoPortfolio] and vise versa
 *
 * @property userWallet                      the user wallet associated with the account list
 * @property responseCryptoCurrenciesFactory factory to create crypto currencies from response tokens
 *
[REDACTED_AUTHOR]
 */
internal class CryptoPortfolioConverter @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    private val userTokensResponseFactory: UserTokensResponseFactory,
) : TwoWayConverter<WalletAccountDTO, Account.Personal> {

    override fun convert(value: WalletAccountDTO): Account.Personal {
        val tokens = value.tokens ?: error("Tokens should not be null")
        val derivationIndex = value.derivationIndex.toDerivationIndex()

        return Account.Personal(
            accountId = value.id.toAccountId(userWallet.walletId),
            accountName = AccountNameConverter.convertBack(value = value.name),
            icon = value.toIcon(),
            derivationIndex = derivationIndex,
            cryptoCurrencies = responseCryptoCurrenciesFactory.createAccountCurrencies(
                tokens = tokens,
                userWallet = userWallet,
                accountIndex = derivationIndex,
            ),
        )
    }

    override fun convertBack(value: Account.Personal): WalletAccountDTO {
        return WalletAccountDTO(
            id = value.accountId.value,
            name = AccountNameConverter.convert(value = value.accountName),
            derivationIndex = value.derivationIndex.value,
            icon = value.icon.value.name,
            iconColor = value.icon.color.name,
            tokens = userTokensResponseFactory.createAccountTokens(
                currencies = value.cryptoCurrencies,
                accountId = value.accountId,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): CryptoPortfolioConverter
    }
}