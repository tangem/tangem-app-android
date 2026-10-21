package com.tangem.data.staking

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.Wallet
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.staking.StakeKitRepositoryFixtures.actionDTO
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.grow.datasource.stakekit.StakeKitApi
import com.tangem.grow.datasource.stakekit.models.request.ActionRequestBody
import com.tangem.grow.datasource.stakekit.models.request.Address
import com.tangem.store.datasource.txhistory.db.dao.StakingValidatorDao
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultStakeKitRepositoryAdditionalAddressesTest {

    private val stakeKitApi: StakeKitApi = mockk()
    private val stakingYieldsStore: StakingYieldsStore = mockk()
    private val stakingValidatorDao: StakingValidatorDao = mockk()
    private val txHistoryFeatureToggle: TxHistoryFeatureToggles = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()

    private val repository = DefaultStakeKitRepository(
        stakeKitApi = stakeKitApi,
        stakingYieldsStore = stakingYieldsStore,
        stakingValidatorDao = stakingValidatorDao,
        txHistoryFeatureToggle = txHistoryFeatureToggle,
        dispatchers = TestingCoroutineDispatcherProvider(),
        walletManagersFacade = walletManagersFacade,
        moshi = StakeKitRepositoryFixtures.moshi,
    )

    private val factory = MockCryptoCurrencyFactory()
    private val solanaNetwork = factory.createCoin(Blockchain.Solana).network
    private val cosmosNetwork = factory.createCoin(Blockchain.Cosmos).network

    private val walletManager: WalletManager = mockk {
        every { wallet.publicKey } returns Wallet.PublicKey(seedKey = COSMOS_KEY, derivationType = null)
    }

    private val body = slot<ActionRequestBody>()

    @BeforeEach
    fun setUp() {
        clearMocks(stakeKitApi, walletManagersFacade)
        body.clear()
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any<ByteArray>(), any<Int>()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
        }
        coEvery { stakeKitApi.createEnterAction(capture(body)) } returns ApiResponse.Success(actionDTO())
        coEvery { walletManagersFacade.getOrCreateWalletManager(WALLET_ID, any<Network>()) } returns walletManager
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    private fun actionParams(
        type: StakingActionCommonType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
        token: YieldToken = MockStaking.solToken,
    ) = ActionParams(
        actionCommonType = type,
        integrationId = MockStaking.SOLANA_ID,
        amount = BigDecimal("1.5"),
        address = "addr-1",
        validatorAddress = MockStaking.VALIDATOR_ADDRESS,
        token = token,
    )

    private val expectedKey: String
        get() = java.util.Base64.getEncoder().encodeToString(COSMOS_COMPRESSED_KEY)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateAction {

        @Test
        fun `GIVEN cosmos token WHEN createAction enter THEN cosmosPubKey is base64 of compressed blockchain key`() =
            runTest {
                // Arrange
                val params = actionParams(token = MockStaking.atomToken)

                // Act
                repository.createAction(WALLET_ID, cosmosNetwork, params)

                // Assert
                val expected = Address(
                    address = "addr-1",
                    additionalAddresses = Address.AdditionalAddresses(cosmosPubKey = expectedKey),
                )
                assertThat(body.captured.addresses).isEqualTo(expected)
                verify { Base64.encodeToString(COSMOS_COMPRESSED_KEY, Base64.NO_WRAP) }
            }

        // Pins current behaviour: branch is chosen by params.token.network, not by the network argument
        @Test
        fun `GIVEN cosmos token on non cosmos network WHEN createAction THEN cosmosPubKey still added`() = runTest {
            // Arrange
            val params = actionParams(token = MockStaking.atomToken)

            // Act
            repository.createAction(WALLET_ID, solanaNetwork, params)

            // Assert
            assertThat(body.captured.addresses.additionalAddresses?.cosmosPubKey).isEqualTo(expectedKey)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN non cosmos token WHEN createAction THEN additional addresses null`(model: TokenModel) = runTest {
            // Arrange
            val params = actionParams(token = model.token)

            // Act
            repository.createAction(WALLET_ID, solanaNetwork, params)

            // Assert
            assertThat(body.captured.addresses.additionalAddresses).isNull()
            verify(exactly = 0) { Base64.encodeToString(any<ByteArray>(), any<Int>()) }
        }

        private fun provideTestModels() = listOf(
            TokenModel(MockStaking.solToken),
            TokenModel(MockStaking.trxToken),
            TokenModel(MockStaking.tonToken),
            TokenModel(MockStaking.bnbToken),
            TokenModel(MockStaking.adaToken),
            TokenModel(MockStaking.polToken),
        )
    }

    internal data class TokenModel(val token: YieldToken)

    private companion object {

        val WALLET_ID = UserWalletId("011")
        const val G_X = "79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798"
        const val G_Y = "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8"

        // Uncompressed secp256k1 generator point G (04 || X || Y); Y is even, so its compressed form is 02 || X
        val COSMOS_KEY = "04$G_X$G_Y".hexToBytes()
        val COSMOS_COMPRESSED_KEY = "02$G_X".hexToBytes()
    }
}