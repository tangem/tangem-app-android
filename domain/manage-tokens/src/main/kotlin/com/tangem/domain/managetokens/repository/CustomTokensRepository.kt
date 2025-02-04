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

    suspend fun createCoin(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Coin

    suspend fun createToken(
        managedCryptoCurrency: ManagedCryptoCurrency.Token,
        sourceNetwork: ManagedCryptoCurrency.SourceNetwork.Default,
        rawId: CryptoCurrency.RawID?,
    ): CryptoCurrency.Token

    suspend fun createCustomToken(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        formValues: AddCustomTokenForm.Validated.All,
    ): CryptoCurrency.Token

    suspend fun removeCurrency(userWalletId: UserWalletId, currency: ManagedCryptoCurrency.Custom)

    suspend fun getSupportedNetworks(userWalletId: UserWalletId): List<Network>

    fun createDerivationPath(rawPath: String): Network.DerivationPath
}