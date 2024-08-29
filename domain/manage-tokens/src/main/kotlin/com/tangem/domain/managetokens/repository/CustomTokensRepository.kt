package com.tangem.domain.managetokens.repository

import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

interface CustomTokensRepository {

    suspend fun validateContractAddress(contractAddress: String, networkId: Network.ID): Boolean

    suspend fun isCurrencyNotAdded(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        contractAddress: String?,
    ): Boolean

    suspend fun findToken(
        userWalletId: UserWalletId,
        contractAddress: String,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Token?

    fun createCoin(networkId: Network.ID, derivationPath: Network.DerivationPath): CryptoCurrency.Coin

    suspend fun createCustomToken(
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        formValues: AddCustomTokenForm.Validated.All,
    ): CryptoCurrency.Token

    suspend fun removeCurrency(userWalletId: UserWalletId, currency: ManagedCryptoCurrency.Custom)
}