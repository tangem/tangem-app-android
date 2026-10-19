package com.tangem.data.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.nft.SolanaNFTTransferExtras
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class DefaultTransactionRepositoryNFTTest {

    private val userWalletId = UserWalletId(stringValue = "1234567890ABCDEF")
    private val network = buildNetwork(rawNetworkId = "solana")

    private val walletManager: WalletManager = mockk {
        every { wallet.address } returns "solanaOwner"
    }

    private val walletManagersFacade: WalletManagersFacade = mockk {
        coEvery {
            getOrCreateWalletManager(userWalletId, Blockchain.Solana, network.derivationPath.value)
        } returns walletManager
    }

    private val repository = DefaultTransactionRepository(
        tangemTechApi = mockk<TangemTechApi>(),
        walletManagersFacade = walletManagersFacade,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `GIVEN solana nft WHEN createNFTTransferTransaction THEN extras is SolanaNFTTransferExtras`() = runTest {
        // Arrange
        val nftAsset = buildSolanaNftAsset()

        // Act
        val transaction = repository.createNFTTransferTransaction(
            ownerAddress = "solanaOwner",
            nftAsset = nftAsset,
            fee = null,
            memo = null,
            destinationAddress = "solanaDestination",
            userWalletId = userWalletId,
            network = network,
        )

        // Assert
        assertThat(transaction.extras).isEqualTo(SolanaNFTTransferExtras)
    }

    private fun buildSolanaNftAsset(): NFTAsset = NFTAsset(
        identifier = NFTAsset.Identifier.Solana(
            tokenAddress = "mintAddress",
            tokenStandard = "ProgrammableNonFungible",
        ),
        collectionIdentifier = mockk(),
        blockchainId = "solana",
        contractType = "ProgrammableNonFungible",
        owner = "solanaOwner",
        name = "Name",
        description = null,
        amount = BigInteger.ONE,
        decimals = 0,
        salePrice = null,
        rarity = null,
        media = null,
        traits = emptyList(),
    )

    private fun buildNetwork(rawNetworkId: String): Network {
        val derivationPath = Network.DerivationPath.None
        return Network(
            id = Network.ID(value = rawNetworkId, derivationPath = derivationPath),
            name = rawNetworkId.replaceFirstChar { it.uppercase() },
            currencySymbol = "SOL",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("Solana"),
            hasFiatFeeRate = false,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }
}