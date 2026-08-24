package com.tangem.data.account.converter

import arrow.core.getOrElse
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.converter.TwoWayConverter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Converts a joint row of `GET /accounts` to an [Account.Joint] and back.
 *
 * @property userWallet the user wallet the account belongs to
 */
internal class JointAccountConverter @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    private val userTokensResponseFactory: UserTokensResponseFactory,
) : TwoWayConverter<WalletAccountDTO, Account.Joint> {

    override fun convert(value: WalletAccountDTO): Account.Joint {
        val derivationIndex = value.derivationIndex.toDerivationIndex()

        return Account.Joint(
            accountId = value.id.toJointAccountId(userWallet.walletId),
            accountName = AccountNameConverter.convertBack(value = value.name),
            icon = value.toIcon(),
            derivationIndex = derivationIndex,
            // A joint account may legitimately carry no tokens at all: it is created empty
            cryptoCurrencies = responseCryptoCurrenciesFactory.createAccountCurrencies(
                tokens = value.tokens,
                userWallet = userWallet,
                accountIndex = derivationIndex,
            ),
        )
    }

    override fun convertBack(value: Account.Joint): WalletAccountDTO {
        return WalletAccountDTO(
            id = value.accountId.value,
            name = AccountNameConverter.convert(value = value.accountName),
            derivationIndex = value.derivationIndex.value,
            icon = value.icon.value.name,
            iconColor = value.icon.color.name,
            type = WalletAccountDTO.Type.JOINT.value,
            tokens = userTokensResponseFactory.createAccountTokens(
                currencies = value.cryptoCurrencies,
                accountId = value.accountId,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): JointAccountConverter
    }
}

private fun String.toJointAccountId(userWalletId: UserWalletId): AccountId {
    return AccountId.forJointAccount(userWalletId = userWalletId, value = this).getOrElse {
        error("Unable to create joint AccountId from value: $this. Cause: $it")
    }
}