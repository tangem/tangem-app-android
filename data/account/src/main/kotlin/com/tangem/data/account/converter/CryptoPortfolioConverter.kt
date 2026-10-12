package com.tangem.data.account.converter

import arrow.core.getOrElse
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.jointaccount.derivation.toNetworkDerivationPath
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.converter.TwoWayConverter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Converts a row of `GET /accounts` to an [Account.CryptoPortfolio] and back.
 *
 * The account's `type` tells a personal portfolio from a joint one; an absent type is a personal one — the field
 * predates joint accounts.
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
) : TwoWayConverter<WalletAccountDTO, Account.CryptoPortfolio> {

    override fun convert(value: WalletAccountDTO): Account.CryptoPortfolio {
        return when (value.type) {
            WalletAccountDTO.Type.JOINT -> convertJoint(value)
            WalletAccountDTO.Type.CRYPTO,
            null,
            -> convertPersonal(value)
        }
    }

    override fun convertBack(value: Account.CryptoPortfolio): WalletAccountDTO {
        return WalletAccountDTO(
            id = value.accountId.value,
            name = AccountNameConverter.convert(value = value.accountName),
            derivationIndex = when (value) {
                is Account.Joint -> value.ownerKeyIndex.value
                is Account.Personal -> value.derivationIndex.value
            },
            icon = value.icon.value.name,
            iconColor = value.icon.color.name,
            tokens = userTokensResponseFactory.createAccountTokens(
                currencies = value.cryptoCurrencies,
                accountId = value.accountId,
            ),
            type = when (value) {
                is Account.Joint -> WalletAccountDTO.Type.JOINT
                is Account.Personal -> WalletAccountDTO.Type.CRYPTO
            },
        )
    }

    private fun convertPersonal(value: WalletAccountDTO): Account.Personal {
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

    private fun convertJoint(value: WalletAccountDTO): Account.Joint {
        val ownerKeyIndex = OwnerKeyIndex(value.derivationIndex).getOrElse {
            error("Unable to create OwnerKeyIndex from value: ${value.derivationIndex}. Cause: $it")
        }

        return Account.Joint(
            accountId = value.id.toJointAccountId(userWallet.walletId),
            accountName = AccountNameConverter.convertBack(value = value.name),
            icon = value.toIcon(),
            ownerKeyIndex = ownerKeyIndex,
            // A joint account may legitimately carry no tokens at all: it is created empty
            cryptoCurrencies = responseCryptoCurrenciesFactory.createCurrencies(
                tokens = value.tokens.orEmpty(),
                userWallet = userWallet,
                derivationPath = ownerKeyIndex.toNetworkDerivationPath(),
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): CryptoPortfolioConverter
    }
}

private fun String.toJointAccountId(userWalletId: UserWalletId): AccountId {
    return AccountId.forJointAccount(userWalletId = userWalletId, value = this).getOrElse {
        error("Unable to create joint AccountId from value: $this. Cause: $it")
    }
}