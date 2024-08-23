package com.tangem.data.managetokens

import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

internal class DefaultCustomTokensRepository : CustomTokensRepository {

    override suspend fun validateContractAddress(contractAddress: String, networkId: Network.ID): Boolean {
        TODO("Should be implemented in https://tangem.atlassian.net/browse/AND-8117")
    }

    override suspend fun isCurrencyNotAdded(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        contractAddress: String?,
    ): Boolean {
        TODO("Should be implemented in https://tangem.atlassian.net/browse/AND-8117")
    }

    override suspend fun findToken(
        userWalletId: UserWalletId,
        contractAddress: String,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Token? {
        TODO("Should be implemented in https://tangem.atlassian.net/browse/AND-8117")
    }

    override suspend fun createCoin(
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Coin {
        TODO("Should be implemented in https://tangem.atlassian.net/browse/AND-8117")
    }

    override suspend fun createCustomToken(
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        formValues: AddCustomTokenForm.Validated.All,
    ): CryptoCurrency.Token {
        TODO("Should be implemented in https://tangem.atlassian.net/browse/AND-8117")
    }
}
