package com.tangem.features.send.subcomponents.destination.model.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.send.subcomponents.destination.model.transformers.WALLET_DEFAULT_COUNT
import com.tangem.features.send.subcomponents.destination.model.transformers.WALLET_KEY_TAG
import com.tangem.features.send.subcomponents.destination.model.transformers.emptyListState
import com.tangem.features.send.subcomponents.destination.ui.state.DestinationWalletUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendRecipientWalletListConverterTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val coin: CryptoCurrency = currencyFactory.ethereum
    private val token: CryptoCurrency = currencyFactory.createToken(Blockchain.Ethereum)

    private fun converter(
        senderAddress: String? = SENDER,
        isSelfSendAvailable: Boolean = false,
        isAccountsMode: Boolean = false,
    ) = SendRecipientWalletListConverter(
        senderAddress = senderAddress,
        isSelfSendAvailable = isSelfSendAvailable,
        isAccountsMode = isAccountsMode,
    )

    private fun wallet(
        name: String = "Wallet",
        userWalletId: UserWalletId = UserWalletId("a1"),
        address: String = "0xWalletAddress",
        cryptoCurrency: CryptoCurrency = coin,
    ) = DestinationWalletUM(
        name = name,
        userWalletId = userWalletId,
        address = address,
        cryptoCurrency = cryptoCurrency,
        account = null,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Filtering {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN excluded wallet WHEN convert THEN filtered out leaving empty placeholder`(model: ExcludedModel) {
            // Act
            val actual = model.converter.convert(listOf(model.wallet))

            // Assert
            assertThat(actual).isEqualTo(emptyListState(WALLET_KEY_TAG, WALLET_DEFAULT_COUNT))
        }

        private fun provideTestModels() = listOf(
            ExcludedModel("blank address", wallet(address = ""), converter()),
            ExcludedModel("token and not a payment account", wallet(cryptoCurrency = token), converter()),
            ExcludedModel(
                "own address while self-send disabled",
                wallet(address = SENDER),
                converter(senderAddress = SENDER, isSelfSendAvailable = false),
            ),
        )

        @Test
        fun `GIVEN own address while self-send enabled WHEN convert THEN included`() {
            // Act
            val actual = converter(senderAddress = SENDER, isSelfSendAvailable = true)
                .convert(listOf(wallet(address = SENDER)))

            // Assert
            assertThat(actual).hasSize(1)
            assertThat(actual.first().title).isEqualTo(stringReference(SENDER))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Grouping {

        @Test
        fun `GIVEN same name across multiple wallets WHEN convert THEN names disambiguated with index`() {
            // Arrange  (same name, different userWalletId -> group size > 1)
            val wallets = listOf(
                wallet(name = "Main", userWalletId = UserWalletId("a1"), address = "0xA"),
                wallet(name = "Main", userWalletId = UserWalletId("a2"), address = "0xB"),
            )

            // Act
            val actual = converter().convert(wallets)

            // Assert
            assertThat(actual).hasSize(2)
            assertThat(actual[0].id).isEqualTo("${WALLET_KEY_TAG}0")
            assertThat(actual[1].id).isEqualTo("${WALLET_KEY_TAG}1")
            assertThat(actual[0].subtitle).isEqualTo(stringReference("Main 1"))
            assertThat(actual[1].subtitle).isEqualTo(stringReference("Main 2"))
            assertThat(actual[0].title).isEqualTo(stringReference("0xA"))
            assertThat(actual[1].title).isEqualTo(stringReference("0xB"))
        }

        @Test
        fun `GIVEN single wallet for a name WHEN convert THEN name kept without index`() {
            // Act
            val actual = converter().convert(listOf(wallet(name = "Solo", address = "0xA")))

            // Assert
            assertThat(actual).hasSize(1)
            assertThat(actual.first().subtitle).isEqualTo(stringReference("Solo"))
        }
    }

    data class ExcludedModel(
        val case: String,
        val wallet: DestinationWalletUM,
        val converter: SendRecipientWalletListConverter,
    )

    private companion object {
        private const val SENDER = "0xSenderAddress"
    }
}