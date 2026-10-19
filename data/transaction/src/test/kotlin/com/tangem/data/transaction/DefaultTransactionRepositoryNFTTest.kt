package com.tangem.data.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.solana.nft.SolanaNFTTransferExtras
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.smartcontract.SmartContractCallDataProviderFactory
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.blockchain.nft.models.SolanaNFTTokenStandard
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultTransactionRepositoryNFTTest {

    private val userWalletId = UserWalletId(stringValue = "1234567890ABCDEF")

    private val walletManager: WalletManager = mockk {
        every { wallet.address } returns OWNER
    }

    private val walletManagersFacade: WalletManagersFacade = mockk {
        coEvery { getOrCreateWalletManager(userWalletId, any(), any()) } returns walletManager
    }

    private val repository = DefaultTransactionRepository(
        tangemTechApi = mockk<TangemTechApi>(),
        walletManagersFacade = walletManagersFacade,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN nft of a network WHEN createNFTTransferTransaction THEN extras match the network`(model: ExtrasModel) =
        runTest {
            // Act
            val transaction = repository.createNFTTransferTransaction(
                ownerAddress = OWNER,
                nftAsset = model.asset,
                fee = null,
                memo = null,
                destinationAddress = DESTINATION,
                userWalletId = userWalletId,
                network = buildNetwork(model.rawNetworkId),
            )

            // Assert
            assertThat(transaction.extras.snapshot()).isEqualTo(model.expected)
        }

    private fun provideTestModels(): List<ExtrasModel> {
        val evmAsset = buildAsset(
            NFTAsset.Identifier.EVM(
                tokenId = BigInteger.TEN,
                tokenAddress = EVM_CONTRACT,
                contractType = NFTAsset.Identifier.EVM.ContractType.ERC721,
            ),
        )
        val evmCallData = requireNotNull(
            SmartContractCallDataProviderFactory.getNFTTransferCallData(
                nftAsset = evmAsset,
                ownerAddress = OWNER,
                destinationAddress = DESTINATION,
                blockchain = Blockchain.Ethereum,
            ),
        )

        return listOf(
            ExtrasModel(
                rawNetworkId = "solana",
                asset = buildAsset(
                    NFTAsset.Identifier.Solana(
                        tokenAddress = "mintAddress",
                        tokenStandard = SolanaNFTTokenStandard.PROGRAMMABLE_NFT,
                    ),
                ),
                expected = SolanaNFTTransferExtras,
            ),
            ExtrasModel(
                rawNetworkId = "ethereum",
                asset = evmAsset,
                expected = EvmExtrasSnapshot(callDataHex = evmCallData.dataHex, gasLimit = null, nonce = null),
            ),
            ExtrasModel(
                rawNetworkId = "the-open-network",
                asset = buildAsset(NFTAsset.Identifier.TON(tokenAddress = "tonAddress")),
                expected = null,
            ),
            ExtrasModel(
                rawNetworkId = "the-open-network",
                asset = buildAsset(NFTAsset.Identifier.Unknown),
                expected = null,
            ),
        )
    }

    private fun TransactionExtras?.snapshot(): Any? = when (this) {
        is EthereumTransactionExtras -> EvmExtrasSnapshot(callDataHex = callData?.dataHex, gasLimit, nonce)
        else -> this
    }

    private fun buildAsset(identifier: NFTAsset.Identifier): NFTAsset = NFTAsset(
        identifier = identifier,
        collectionIdentifier = NFTCollection.Identifier.Unknown,
        blockchainId = "blockchain",
        contractType = "contractType",
        owner = OWNER,
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
            name = rawNetworkId,
            currencySymbol = "SYMBOL",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified(rawNetworkId),
            hasFiatFeeRate = false,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    data class ExtrasModel(val rawNetworkId: String, val asset: NFTAsset, val expected: Any?)

    private data class EvmExtrasSnapshot(val callDataHex: String?, val gasLimit: BigInteger?, val nonce: BigInteger?)

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DESTINATION = "0x2222222222222222222222222222222222222222"
        const val EVM_CONTRACT = "0x3333333333333333333333333333333333333333"
    }
}