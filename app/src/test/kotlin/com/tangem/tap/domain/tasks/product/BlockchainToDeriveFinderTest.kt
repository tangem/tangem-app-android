package com.tangem.tap.domain.tasks.product

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.wallets.derivations.BlockchainToDerive
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlockchainToDeriveFinderTest {

    private val walletAccountsFetcher = mockk<WalletAccountsFetcher>()
    private val finder = BlockchainToDeriveFinder(
        walletAccountsFetcher = walletAccountsFetcher,
    )

    @AfterEach
    fun tearDown() {
        clearMocks(walletAccountsFetcher)
    }

    @Test
    fun `GIVEN card is not HD wallet THEN return empty set`() = runTest {
        // Arrange
        val card = mockk<CardDTO> {
            every { this@mockk.settings.isHDWalletAllowed } returns false
        }

        // Act
        val actual = finder.find(card)

        // Assert
        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN card has empty wallets THEN return empty set`() = runTest {
        // Arrange
        val card = mockk<CardDTO> {
            every { this@mockk.settings.isHDWalletAllowed } returns true
            every { this@mockk.wallets } returns emptyList()
        }

        // Act
        val actual = finder.find(card)

        // Assert
        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN saved bitcoin THEN return only bitcoin`() = runTest {
        // Arrange
        val card = createCardDTO()

        val response = createResponse(Blockchain.Bitcoin)
        coEvery { walletAccountsFetcher.getSaved(userWalletId) } returns response

        // Act
        val actual = finder.find(card)

        // Assert
        val expected = setOf(
            createExpected(Blockchain.Bitcoin),
        )

        Truth.assertThat(actual).containsExactlyElementsIn(expected)

        coVerify(exactly = 1) { walletAccountsFetcher.getSaved(userWalletId) }
    }

    @Test
    fun `GIVEN empty store and common demo card THEN return demo blockchains`() = runTest {
        // Arrange
        val demoCardId = "AC01000000045754"
        val card = createCardDTO(cardId = demoCardId)

        coEvery { walletAccountsFetcher.getSaved(userWalletId) } returns null

        // Act
        val actual = finder.find(card)

        // Assert
        val expected = setOf(
            createExpected(Blockchain.Bitcoin),
            createExpected(Blockchain.Ethereum),
            createExpected(Blockchain.Dogecoin),
            createExpected(Blockchain.Solana),
        )

        Truth.assertThat(actual).containsExactlyElementsIn(expected)

        coVerify(exactly = 1) { walletAccountsFetcher.getSaved(userWalletId) }
    }

    @Test
    fun `GIVEN empty store and DE00 demo card THEN return demo blockchains`() = runTest {
        // Arrange
        val demoCardId = "DE00"
        val card = createCardDTO(cardId = demoCardId)

        coEvery { walletAccountsFetcher.getSaved(userWalletId) } returns null

        // Act
        val actual = finder.find(card)

        // Assert
        val expected = setOf(
            createExpected(Blockchain.Bitcoin),
            createExpected(Blockchain.Ethereum),
            createExpected(Blockchain.Dogecoin),
        )

        Truth.assertThat(actual).containsExactlyElementsIn(expected)

        coVerify(exactly = 1) { walletAccountsFetcher.getSaved(userWalletId) }
    }

    @Test
    fun `GIVEN empty store THEN return default blockchains`() = runTest {
        // Arrange
        val card = createCardDTO()

        coEvery { walletAccountsFetcher.getSaved(userWalletId) } returns null

        // Act
        val actual = finder.find(card)

        // Assert
        val expected = setOf(
            createExpected(Blockchain.Bitcoin),
            createExpected(Blockchain.Ethereum),
        )

        Truth.assertThat(actual).containsExactlyElementsIn(expected)

        coVerify(exactly = 1) { walletAccountsFetcher.getSaved(userWalletId) }
    }

    @Test
    fun `GIVEN saved cardano THEN return only cardano`() = runTest {
        // Arrange
        val card = createCardDTO()

        val response = createResponse(Blockchain.Cardano)
        coEvery { walletAccountsFetcher.getSaved(userWalletId) } returns response

        // Act
        val actual = finder.find(card)

        // Assert
        val expected = setOf(
            createExpected(Blockchain.Cardano),
        )

        Truth.assertThat(actual).containsExactlyElementsIn(expected)

        coVerify(exactly = 1) { walletAccountsFetcher.getSaved(userWalletId) }
    }

    @Test
    fun `GIVEN saved eth-like blockchains THEN return all saved blockchains without filtering`() = runTest {
        // Arrange
        val card = createCardDTO()

        val blockchains = listOf(Blockchain.Ethereum, Blockchain.BSC, Blockchain.Polygon)

        val response = createResponse(*blockchains.toTypedArray())

        coEvery { walletAccountsFetcher.getSaved(userWalletId) } returns response

        // Act
        val actual = finder.find(card)

        // Assert
        val expected = blockchains.mapTo(hashSetOf(), ::createExpected)

        Truth.assertThat(actual).containsExactlyElementsIn(expected)

        coVerify(exactly = 1) { walletAccountsFetcher.getSaved(userWalletId) }
    }

    private fun createCardDTO(cardId: String = "0001", batchId: String = "AC10"): CardDTO {
        val wallet = mockk<CardDTO.Wallet> {
            every { this@mockk.publicKey } returns byteArrayOf(0)
        }

        return mockk<CardDTO> {
            every { this@mockk.cardId } returns cardId
            every { this@mockk.batchId } returns batchId
            every { this@mockk.settings.isHDWalletAllowed } returns true
            every { this@mockk.settings.isKeysImportAllowed } returns true
            every { this@mockk.firmwareVersion } returns CardDTO.FirmwareVersion(
                major = 6,
                minor = 33,
                patch = 0,
                type = com.tangem.common.card.FirmwareVersion.FirmwareType.Release,
            )
            every { this@mockk.wallets } returns listOf(wallet)
        }
    }

    private fun createResponse(vararg blockchains: Blockchain): GetWalletAccountsResponse {
        val tokens = blockchains.map { blockchain ->
            mockk<UserTokensResponse.Token> {
                every { this@mockk.networkId } returns blockchain.toNetworkId()
                every { this@mockk.derivationPath } returns blockchain.getDerivationPath().rawPath
                every { this@mockk.contractAddress } returns null
            }
        }

        val account = mockk<WalletAccountDTO> {
            every { this@mockk.tokens } returns tokens
        }

        return mockk {
            every { this@mockk.accounts } returns listOf(account)
        }
    }

    private fun createExpected(
        blockchain: Blockchain,
        derivationPath: DerivationPath = blockchain.getDerivationPath(),
    ): BlockchainToDerive {
        return BlockchainToDerive(blockchain = blockchain, derivationPath = derivationPath)
    }

    private fun Blockchain.getDerivationPath(): DerivationPath {
        return derivationPath(DerivationStyle.V3)!!
    }

    private companion object {

        // for byteArrayOf(0)
        val userWalletId = UserWalletId("41448576B8DA24C7D8F5F0F79863D20D7D8312A7F9E50D3248304136DDB7AAD7")
    }
}
