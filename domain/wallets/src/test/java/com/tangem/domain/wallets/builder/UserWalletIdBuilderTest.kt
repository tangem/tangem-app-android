package com.tangem.domain.wallets.builder

import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UserWalletIdBuilderTest {

    private val twinsScanResponse = createScanResponse(
        cardId = FIRST_TWIN_CARD_ID,
        publicKey = FIRST_TWIN_PUBLIC_KEY,
        productType = ProductType.Twins,
        secondTwinPublicKey = SECOND_TWIN_PUBLIC_KEY.toHexString(),
    )

    private val twinsWalletId = UserWalletIdBuilder.scanResponse(twinsScanResponse).build()

    @Test
    fun `GIVEN twin card WHEN card THEN throws IllegalArgumentException`() {
        // Act
        val actual = runCatching { UserWalletIdBuilder.card(card = twinsScanResponse.card) }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN read card and wallet scan response WHEN card THEN builds expected id`(model: CardOfWalletModel) {
        // Act
        val actual = UserWalletIdBuilder
            .card(card = model.readCard, walletScanResponse = model.walletScanResponse)
            .build()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        CardOfWalletModel(
            readCard = twinsScanResponse.card,
            walletScanResponse = twinsScanResponse,
            expected = twinsWalletId,
        ),
        CardOfWalletModel(
            readCard = createCard(cardId = SECOND_TWIN_CARD_ID, publicKey = SECOND_TWIN_PUBLIC_KEY),
            walletScanResponse = twinsScanResponse,
            expected = twinsWalletId,
        ),
        CardOfWalletModel(
            readCard = createCard(cardId = SECOND_TWIN_CARD_ID, publicKey = FOREIGN_TWIN_PUBLIC_KEY),
            walletScanResponse = twinsScanResponse,
            expected = UserWalletIdBuilder.scanResponse(
                createScanResponse(
                    cardId = SECOND_TWIN_CARD_ID,
                    publicKey = FOREIGN_TWIN_PUBLIC_KEY,
                    productType = ProductType.Twins,
                    secondTwinPublicKey = FIRST_TWIN_PUBLIC_KEY.toHexString(),
                ),
            ).build(),
        ),
        CardOfWalletModel(
            readCard = createCard(cardId = FIRST_TWIN_CARD_ID, publicKey = FIRST_TWIN_PUBLIC_KEY),
            walletScanResponse = createScanResponse(
                cardId = FIRST_TWIN_CARD_ID,
                publicKey = FIRST_TWIN_PUBLIC_KEY,
                productType = ProductType.Twins,
                secondTwinPublicKey = null,
            ),
            expected = UserWalletIdBuilder.walletPublicKey(FIRST_TWIN_PUBLIC_KEY),
        ),
        CardOfWalletModel(
            readCard = createCard(cardId = WALLET_CARD_ID, publicKey = FIRST_TWIN_PUBLIC_KEY),
            walletScanResponse = createScanResponse(
                cardId = WALLET_CARD_ID,
                publicKey = FIRST_TWIN_PUBLIC_KEY,
                productType = ProductType.Wallet,
                secondTwinPublicKey = null,
            ),
            expected = UserWalletIdBuilder.walletPublicKey(FIRST_TWIN_PUBLIC_KEY),
        ),
    )

    data class CardOfWalletModel(
        val readCard: CardDTO,
        val walletScanResponse: ScanResponse,
        val expected: UserWalletId?,
    )

    private fun createScanResponse(
        cardId: String,
        publicKey: ByteArray,
        productType: ProductType,
        secondTwinPublicKey: String?,
    ): ScanResponse {
        return createMockScanResponse().copy(
            card = createCard(cardId = cardId, publicKey = publicKey),
            productType = productType,
            secondTwinPublicKey = secondTwinPublicKey,
        )
    }

    private fun createCard(cardId: String, publicKey: ByteArray): CardDTO {
        val card = createMockScanResponse().card

        return card.copy(
            cardId = cardId,
            wallets = listOf(card.wallets.first().copy(publicKey = publicKey)),
        )
    }

    private fun createMockScanResponse(): ScanResponse {
        return MockScanResponseFactory.create(
            cardConfig = GenericCardConfig(maxWalletCount = 1),
            derivedKeys = emptyMap(),
        )
    }

    private companion object {
        const val FIRST_TWIN_CARD_ID = "CB64000000015006"
        const val SECOND_TWIN_CARD_ID = "CB65000000015005"
        const val WALLET_CARD_ID = "AC01000000000001"

        val FIRST_TWIN_PUBLIC_KEY = (
            "042AF9EF10F0AB0846248E122D8265A7166035953D938A537464B1A6777BBD17B4" +
                "CF84473DCEA5EE9CDE5DD024AEE42D7DFD380844D828DE90BDD5235496143EB1"
            ).hexToBytes()
        val SECOND_TWIN_PUBLIC_KEY = (
            "041535EF77F57A7E7C1541858E5BD3C096A81CEB39FCC72FB9354BBC55FEF86B3C" +
                "2D5A44750FABE59D268BEC0CB1BAC854A7678F48A26BD355CE39FE197457E2FF"
            ).hexToBytes()
        val FOREIGN_TWIN_PUBLIC_KEY = (
            "0479BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798" +
                "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8"
            ).hexToBytes()
    }
}