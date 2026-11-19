package com.tangem.data.common.currency

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResponseCryptoCurrenciesFactoryTest {

    private val factory = ResponseCryptoCurrenciesFactory(
        networkFactory = NetworkFactory(excludedBlockchains = ExcludedBlockchains()),
    )
    private val userWallet = MockUserWalletFactory.create()
    private val ownerKeyPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/3")

    private val coin = UserTokensResponse.Token(
        id = "ethereum",
        networkId = "ethereum",
        derivationPath = ownerKeyPath.value,
        name = "Ethereum",
        symbol = "ETH",
        decimals = 18,
        contractAddress = null,
    )
    private val token = UserTokensResponse.Token(
        id = "tether",
        networkId = "ethereum",
        derivationPath = ownerKeyPath.value,
        name = "Tether",
        symbol = "USDT",
        decimals = 6,
        contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
    )
    private val tokens = listOf(coin, token)

    @Test
    fun `GIVEN listed tokens on the owner key path WHEN createCurrencies THEN every currency sits on the joint network`() {
        // Act
        val actual = factory.createCurrencies(tokens = tokens, userWallet = userWallet, derivationPath = ownerKeyPath)

        // Assert
        assertThat(actual).hasSize(2)
        assertThat(actual.map { it.network.id }.distinct())
            .containsExactly(Network.ID(value = "ethereum", derivationPath = ownerKeyPath))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN joint path WHEN createCurrencies THEN custom tokens are left out`(model: JointModel) {
        // Act
        val actual = factory.createCurrencies(
            tokens = listOf(coin, model.token),
            userWallet = userWallet,
            derivationPath = ownerKeyPath,
        )

        // Assert
        assertThat(actual.map { it.symbol }).containsExactlyElementsIn(model.expectedSymbols).inOrder()
    }

    private fun provideTestModels() = listOf(
        JointModel(
            name = "a token without an id",
            token = token.copy(id = null),
            expectedSymbols = listOf("ETH"),
        ),
        JointModel(
            name = "a token on another path",
            token = token.copy(derivationPath = "m/44'/60'/0'/0/0"),
            expectedSymbols = listOf("ETH"),
        ),
        JointModel(
            name = "a token that names no path",
            token = token.copy(derivationPath = null),
            expectedSymbols = listOf("ETH"),
        ),
    )

    data class JointModel(val name: String, val token: UserTokensResponse.Token, val expectedSymbols: List<String>) {
        override fun toString(): String = name
    }

    @Test
    fun `GIVEN same tokens WHEN built for a joint and for the main account THEN currency ids differ`() {
        // Act
        val joint = factory.createCurrencies(tokens = tokens, userWallet = userWallet, derivationPath = ownerKeyPath)
        val personal = factory.createCurrencies(
            tokens = tokens.map { it.copy(derivationPath = "m/44'/60'/0'/0/0") },
            userWallet = userWallet,
            accountIndex = DerivationIndex.Main,
        )

        // Assert
        assertThat(joint.map { it.id }).containsNoneIn(personal.map { it.id })
    }
}