package com.tangem.data.common.currency

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserTokensResponseAccountIdEnricherTest {

    private val userWalletId = UserWalletId("011")
    private val mockCryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val userTokensResponseFactory = UserTokensResponseFactory()

    @Test
    fun `enriches tokens with missing account ids`() {
        // Arrange
        val response = mockCryptoCurrencyFactory.ethereumAndStellar
            .mapIndexed { index, currency ->
                currency.toResponseToken(
                    accountId = null,
                    derivationPath = "m/44'/60'/$index'/0/0",
                )
            }
            .toResponse()

        // Act
        val actual = UserTokensResponseAccountIdEnricher(userWalletId, response)

        // Assert
        val expected = response.tokens
            .mapIndexed { index, currency ->
                currency.enrichWithAccountId(accountIndex = index)
            }
            .toResponse()

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `does not modify tokens with existing account ids`() {
        // Arrange
        val response = mockCryptoCurrencyFactory.ethereumAndStellar
            .mapIndexed { index, currency ->
                currency.toResponseToken(derivationPath = "m/44'/60'/$index'/0/0")
                    .enrichWithAccountId(accountIndex = index)
            }
            .toResponse()

        // Act
        val actual = UserTokensResponseAccountIdEnricher(userWalletId, response)

        // Assert
        val expected = response
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `skips tokens with invalid derivation paths`() {
        // Arrange
        val validDerivationPath = "m/44'/60'/0'/0/0"
        val invalidDerivationPath = "invalid/path"

        val tokenWithInvalidPath = mockCryptoCurrencyFactory.ethereum.toResponseToken(
            accountId = null,
            derivationPath = invalidDerivationPath,
        )

        val tokenWithValidPath = mockCryptoCurrencyFactory.stellar.toResponseToken(
            accountId = null,
            derivationPath = validDerivationPath,
        )

        val response = listOf(tokenWithInvalidPath, tokenWithValidPath).toResponse()

        // Act
        val actual = UserTokensResponseAccountIdEnricher(userWalletId, response)

        // Assert
        val expected = listOf(
            tokenWithInvalidPath,
            tokenWithValidPath.enrichWithAccountId(accountIndex = 0),
        ).toResponse()

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `skips tokens with unknown network id`() {
        // Arrange
        val unknownNetworkId = "unknown"
        val validNetworkId = mockCryptoCurrencyFactory.ethereum.network.rawId

        val tokenWithUnknownNetworkId = mockCryptoCurrencyFactory.ethereum.toResponseToken(
            networkId = unknownNetworkId,
            derivationPath = "m/44'/60'/0'/0/0",
            accountId = null,
        )

        val tokenWithValidNetworkId = mockCryptoCurrencyFactory.ethereum.toResponseToken(
            accountId = null,
            networkId = validNetworkId,
            derivationPath = "m/44'/60'/0'/0/0",
        )

        val response = listOf(tokenWithUnknownNetworkId, tokenWithValidNetworkId).toResponse()

        // Act
        val actual = UserTokensResponseAccountIdEnricher(userWalletId, response)

        // Assert
        val expected = listOf(
            tokenWithUnknownNetworkId,
            tokenWithValidNetworkId.enrichWithAccountId(accountIndex = 0),
        ).toResponse()

        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun CryptoCurrency.toResponseToken(
        accountId: AccountId? = null,
        networkId: String? = null,
        derivationPath: String,
    ): UserTokensResponse.Token {
        return userTokensResponseFactory.createResponseToken(this).copy(
            networkId = networkId ?: network.rawId,
            derivationPath = derivationPath,
            accountId = accountId?.value,
        )
    }

    private fun List<UserTokensResponse.Token>.toResponse(): UserTokensResponse {
        return UserTokensResponse(
            group = UserTokensResponse.GroupType.NONE,
            sort = UserTokensResponse.SortType.MANUAL,
            tokens = this,
        )
    }

    private fun UserTokensResponse.Token.enrichWithAccountId(accountIndex: Int): UserTokensResponse.Token {
        val derivationIndex = DerivationIndex(value = accountIndex).getOrNull()!!
        val accountId = AccountId.forCryptoPortfolio(userWalletId, derivationIndex)

        return copy(accountId = accountId.value)
    }
}