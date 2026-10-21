package com.tangem.data.staking

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonEncodingException
import com.tangem.blockchain.blockchains.solana.SolanaTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionData.Compiled.Data.Bytes
import com.tangem.blockchain.common.TransactionData.Compiled.Data.RawString
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.staking.StakeKitRepositoryFixtures.apiError
import com.tangem.data.staking.StakeKitRepositoryFixtures.stakingTransaction
import com.tangem.data.staking.StakeKitRepositoryFixtures.transactionDTO
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.grow.datasource.stakekit.StakeKitApi
import com.tangem.grow.datasource.stakekit.models.request.ConstructTransactionRequestBody
import com.tangem.grow.datasource.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.store.datasource.txhistory.db.dao.StakingValidatorDao
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultStakeKitRepositoryConstructTransactionTest {

    private val stakeKitApi: StakeKitApi = mockk()
    private val stakingYieldsStore: StakingYieldsStore = mockk()
    private val stakingValidatorDao: StakingValidatorDao = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()

    private val repository = DefaultStakeKitRepository(
        stakeKitApi = stakeKitApi,
        stakingYieldsStore = stakingYieldsStore,
        stakingValidatorDao = stakingValidatorDao,
        dispatchers = TestingCoroutineDispatcherProvider(),
        walletManagersFacade = walletManagersFacade,
        moshi = StakeKitRepositoryFixtures.moshi,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitApi, stakingYieldsStore, stakingValidatorDao, walletManagersFacade)
    }

    private fun rawId(value: String) = Network.RawID(value)

    private fun stubTransaction(unsigned: String?, network: NetworkTypeDTO = NetworkTypeDTO.SOLANA) {
        coEvery { stakeKitApi.constructTransaction("tx-1", ConstructTransactionRequestBody()) } returns
            ApiResponse.Success(transactionDTO(unsignedTransaction = unsigned, network = network))
    }

    private suspend fun construct(networkId: Network.RawID) = repository.constructTransaction(
        networkId = networkId,
        fee = FEE,
        amount = AMOUNT,
        transactionId = "tx-1",
    )

    private fun compiled(value: TransactionData.Compiled.Data, extras: TransactionExtras? = null) =
        TransactionData.Compiled(
            value = value,
            fee = FEE,
            amount = AMOUNT,
            status = TransactionStatus.Unconfirmed,
            extras = extras,
        )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Solana {

        @Test
        fun `GIVEN solana hex unsigned tx WHEN constructTransaction THEN compiled bytes decoded from hex`() = runTest {
            // Arrange
            stubTransaction("0a0bff")

            // Act
            val actual = construct(rawId("solana"))

            // Assert
            assertThat(actual.first).isEqualTo(stakingTransaction("0a0bff"))
            assertThat((actual.second.value as Bytes).data).isEqualTo(byteArrayOf(0x0a, 0x0b, 0xff.toByte()))
            assertThat(actual.second.copy(value = RawString(""))).isEqualTo(
                compiled(RawString(""), extras = SolanaTransactionExtras(shouldRefreshRecentBlockhash = true)),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Cosmos {

        @Test
        fun `GIVEN cosmos hex unsigned tx WHEN constructTransaction THEN compiled bytes decoded from hex`() = runTest {
            // Arrange
            stubTransaction("0a0b", NetworkTypeDTO.COSMOS)

            // Act
            val actual = construct(rawId("cosmos"))

            // Assert
            assertThat(actual.first).isEqualTo(stakingTransaction("0a0b", NetworkType.COSMOS))
            assertThat((actual.second.value as Bytes).data).isEqualTo(byteArrayOf(0x0a, 0x0b))
            assertThat(actual.second.copy(value = RawString(""))).isEqualTo(compiled(RawString("")))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RawStringChains {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN raw string chain WHEN constructTransaction THEN compiled raw string equals unsigned tx`(
            model: ChainModel,
        ) = runTest {
            // Arrange
            stubTransaction("raw-payload")

            // Act
            val actual = construct(rawId(model.rawId))

            // Assert
            assertThat(actual).isEqualTo(stakingTransaction("raw-payload") to compiled(RawString("raw-payload")))
        }

        private fun provideTestModels() = listOf(
            ChainModel("the-open-network"),
            ChainModel("cardano"),
            ChainModel("binance-smart-chain"),
            ChainModel("ethereum"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Tron {

        @Test
        fun `GIVEN tron json with raw_data_hex WHEN constructTransaction THEN compiled raw string is raw_data_hex`() =
            runTest {
                // Arrange
                val json = """{"raw_data_hex":"abcdef","txID":"1"}"""
                stubTransaction(json, NetworkTypeDTO.TRON)

                // Act
                val actual = construct(rawId("tron"))

                // Assert
                assertThat(actual).isEqualTo(stakingTransaction(json, NetworkType.TRON) to compiled(RawString("abcdef")))
            }

        @Test
        fun `GIVEN malformed tron json WHEN constructTransaction THEN throws moshi encoding error`() = runTest {
            // Arrange
            stubTransaction("not-json", NetworkTypeDTO.TRON)

            // Act
            val error = runCatching { construct(rawId("tron")) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(JsonEncodingException::class.java)
        }

        @Test
        fun `GIVEN tron json null literal WHEN constructTransaction THEN throws failed to parse`() = runTest {
            // Arrange
            stubTransaction("null", NetworkTypeDTO.TRON)

            // Act
            val error = runCatching { construct(rawId("tron")) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("Failed to parse Tron StakeKit transaction")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Unsupported {

        // Pins current behaviour: Polygon and every testnet hit error("Unsupported")
        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN unsupported blockchain WHEN constructTransaction THEN throws unsupported blockchain`(
            model: UnsupportedModel,
        ) = runTest {
            // Arrange
            stubTransaction("0a0b")

            // Act
            val error = runCatching { construct(Network.RawID(model.rawId)) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("Unsupported blockchain: ${model.blockchain}")
        }

        private fun provideTestModels() = listOf(
            UnsupportedModel("polygon-pos", Blockchain.Polygon),
            UnsupportedModel("solana/test", Blockchain.SolanaTestnet),
            UnsupportedModel("tron/test", Blockchain.TronTestnet),
            UnsupportedModel("cosmos/test", Blockchain.CosmosTestnet),
            UnsupportedModel("unknown-net", Blockchain.Unknown),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Errors {

        @Test
        fun `GIVEN unsigned tx missing WHEN constructTransaction THEN throws no unsigned transaction`() = runTest {
            // Arrange
            stubTransaction(unsigned = null)

            // Act
            val error = runCatching { construct(rawId("solana")) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("No unsigned transaction available")
        }

        @Test
        fun `GIVEN api error WHEN constructTransaction THEN throws api cause`() = runTest {
            // Arrange
            val cause = ApiResponseError.NetworkException()
            coEvery { stakeKitApi.constructTransaction("tx-1", ConstructTransactionRequestBody()) } returns
                apiError(cause)

            // Act
            val error = runCatching { construct(rawId("solana")) }.exceptionOrNull()

            // Assert
            assertThat(error).isSameInstanceAs(cause)
        }
    }

    internal data class ChainModel(val rawId: String)

    internal data class UnsupportedModel(val rawId: String, val blockchain: Blockchain)

    private companion object {

        val FEE = Fee.Common(Amount(currencySymbol = "SOL", value = BigDecimal("0.001"), decimals = 9))
        val AMOUNT = Amount(currencySymbol = "SOL", value = BigDecimal("1.5"), decimals = 9)
    }
}