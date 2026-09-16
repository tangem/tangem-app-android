package com.tangem.data.common.currency

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.network.Network
import org.junit.jupiter.api.Test

internal class ResponseCryptoCurrenciesFactoryTest {

    private val factory = ResponseCryptoCurrenciesFactory(
        networkFactory = NetworkFactory(excludedBlockchains = ExcludedBlockchains()),
    )
    private val userWallet = MockUserWalletFactory.create()
    private val ownerKeyPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/3")

    // Both tokens name the main account's path: for a joint row it must be ignored
    private val tokens = listOf(
        UserTokensResponse.Token(
            networkId = "ethereum",
            derivationPath = "m/44'/60'/0'/0/0",
            name = "Ethereum",
            symbol = "ETH",
            decimals = 18,
            contractAddress = null,
        ),
        UserTokensResponse.Token(
            id = "tether",
            networkId = "ethereum",
            derivationPath = "m/44'/60'/0'/0/0",
            name = "Tether",
            symbol = "USDT",
            decimals = 6,
            contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
        ),
    )

    @Test
    fun `GIVEN joint path WHEN createCurrencies THEN every currency sits on the joint network whatever the token names`() {
        // Act
        val actual = factory.createCurrencies(tokens = tokens, userWallet = userWallet, derivationPath = ownerKeyPath)

        // Assert
        assertThat(actual).hasSize(2)
        assertThat(actual.map { it.network.id }.distinct())
            .containsExactly(Network.ID(value = "ethereum", derivationPath = ownerKeyPath))
    }

    @Test
    fun `GIVEN same tokens WHEN built for a joint and for the main account THEN currency ids differ`() {
        // Act
        val joint = factory.createCurrencies(tokens = tokens, userWallet = userWallet, derivationPath = ownerKeyPath)
        val personal = factory.createCurrencies(tokens = tokens, userWallet = userWallet, accountIndex = DerivationIndex.Main)

        // Assert
        assertThat(joint.map { it.id }).containsNoneIn(personal.map { it.id })
    }
}