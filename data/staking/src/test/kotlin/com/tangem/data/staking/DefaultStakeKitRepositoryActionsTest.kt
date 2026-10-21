package com.tangem.data.staking

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.staking.StakeKitRepositoryFixtures.actionDTO
import com.tangem.data.staking.StakeKitRepositoryFixtures.apiError
import com.tangem.data.staking.StakeKitRepositoryFixtures.gasEstimateDTO
import com.tangem.data.staking.StakeKitRepositoryFixtures.stakingAction
import com.tangem.data.staking.StakeKitRepositoryFixtures.tokenDTO
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.datasource.local.token.converter.YieldTokenConverter
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.grow.datasource.stakekit.StakeKitApi
import com.tangem.grow.datasource.stakekit.models.request.ActionRequestBody
import com.tangem.grow.datasource.stakekit.models.request.ActionRequestBodyArgs
import com.tangem.grow.datasource.stakekit.models.request.Address
import com.tangem.grow.datasource.stakekit.models.request.PendingActionRequestBody
import com.tangem.grow.datasource.stakekit.models.request.TronResource
import com.tangem.grow.datasource.stakekit.models.response.GetActionsResponse
import com.tangem.store.datasource.txhistory.db.dao.StakingValidatorDao
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultStakeKitRepositoryActionsTest {

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
    private val solana = factory.createCoin(Blockchain.Solana)
    private val solanaNetwork = solana.network
    private val tronNetwork = factory.createCoin(Blockchain.Tron).network
    private val tronTestnetNetwork = tronNetwork.copy(
        id = Network.ID(value = "tron/test", derivationPath = tronNetwork.derivationPath),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitApi, stakingYieldsStore, stakingValidatorDao, txHistoryFeatureToggle, walletManagersFacade)
        coEvery { walletManagersFacade.getOrCreateWalletManager(any(), any<Network>()) } returns null
    }

    private fun actionParams(
        type: StakingActionCommonType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
        token: YieldToken = MockStaking.solToken,
        amount: BigDecimal = BigDecimal("1.5"),
        pendingType: StakingActionType? = null,
        passthrough: String? = null,
    ) = ActionParams(
        actionCommonType = type,
        integrationId = MockStaking.SOLANA_ID,
        amount = amount,
        address = "addr-1",
        validatorAddress = MockStaking.VALIDATOR_ADDRESS,
        token = token,
        passthrough = passthrough,
        type = pendingType,
    )

    private fun expectedEnterBody() = ActionRequestBody(
        integrationId = MockStaking.SOLANA_ID,
        addresses = Address(address = "addr-1", additionalAddresses = null),
        args = ActionRequestBodyArgs(
            amount = "1.5",
            validatorAddress = MockStaking.VALIDATOR_ADDRESS,
            validatorAddresses = listOf(MockStaking.VALIDATOR_ADDRESS),
            tronResource = null,
            inputToken = YieldTokenConverter.convertBack(MockStaking.solToken),
        ),
    )

    private fun expectedPendingBody() = PendingActionRequestBody(
        type = StakingActionType.CLAIM_REWARDS,
        integrationId = MockStaking.SOLANA_ID,
        passthrough = "pass-1",
        args = ActionRequestBodyArgs(
            amount = "1.5",
            validatorAddress = MockStaking.VALIDATOR_ADDRESS,
            validatorAddresses = listOf(MockStaking.VALIDATOR_ADDRESS),
        ),
    )

    private fun stubAllCreateEndpoints() {
        coEvery { stakeKitApi.createEnterAction(any()) } returns ApiResponse.Success(actionDTO())
        coEvery { stakeKitApi.createExitAction(any()) } returns ApiResponse.Success(actionDTO())
        coEvery { stakeKitApi.createPendingAction(any()) } returns ApiResponse.Success(actionDTO())
    }

    private fun stubAllEstimateEndpoints() {
        coEvery { stakeKitApi.estimateGasOnEnter(any()) } returns ApiResponse.Success(gasEstimateDTO())
        coEvery { stakeKitApi.estimateGasOnExit(any()) } returns ApiResponse.Success(gasEstimateDTO())
        coEvery { stakeKitApi.estimateGasOnPending(any()) } returns ApiResponse.Success(gasEstimateDTO())
    }

    private fun verifyCreateEndpoint(endpoint: ActionEndpoint) {
        coVerify(exactly = if (endpoint == ActionEndpoint.ENTER) 1 else 0) { stakeKitApi.createEnterAction(any()) }
        coVerify(exactly = if (endpoint == ActionEndpoint.EXIT) 1 else 0) { stakeKitApi.createExitAction(any()) }
        coVerify(exactly = if (endpoint == ActionEndpoint.PENDING) 1 else 0) {
            stakeKitApi.createPendingAction(any())
        }
    }

    private fun verifyEstimateEndpoint(endpoint: ActionEndpoint) {
        coVerify(exactly = if (endpoint == ActionEndpoint.ENTER) 1 else 0) { stakeKitApi.estimateGasOnEnter(any()) }
        coVerify(exactly = if (endpoint == ActionEndpoint.EXIT) 1 else 0) { stakeKitApi.estimateGasOnExit(any()) }
        coVerify(exactly = if (endpoint == ActionEndpoint.PENDING) 1 else 0) {
            stakeKitApi.estimateGasOnPending(any())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetActions {

        @Test
        fun `GIVEN default address WHEN getActions THEN api called with json names and actions converted`() = runTest {
            // Arrange
            coEvery { walletManagersFacade.getDefaultAddress(WALLET_ID, solanaNetwork) } returns "addr-1"
            coEvery { stakeKitApi.getActions(any(), any(), any(), any(), any()) } returns ApiResponse.Success(
                GetActionsResponse(data = listOf(actionDTO()), hasNextPage = false, limit = 50, page = 0),
            )

            // Act
            val actual = repository.getActions(WALLET_ID, solana, NetworkType.SOLANA, StakingActionStatus.PROCESSING)

            // Assert
            assertThat(actual).isEqualTo(listOf(stakingAction()))
            coVerify { stakeKitApi.getActions("addr-1", "solana", "PROCESSING", "createdAtDesc", 50) }
        }

        @Test
        fun `GIVEN binance network type WHEN getActions THEN network json name is binance`() = runTest {
            // Arrange
            coEvery { walletManagersFacade.getDefaultAddress(WALLET_ID, solanaNetwork) } returns "addr-1"
            coEvery { stakeKitApi.getActions(any(), any(), any(), any(), any()) } returns ApiResponse.Success(
                GetActionsResponse(data = emptyList(), hasNextPage = false, limit = 50, page = 0),
            )

            // Act
            repository.getActions(WALLET_ID, solana, NetworkType.BINANCE, StakingActionStatus.SUCCESS)

            // Assert
            coVerify { stakeKitApi.getActions("addr-1", "binance", "SUCCESS", "createdAtDesc", 50) }
        }

        @Test
        fun `GIVEN no default address WHEN getActions THEN empty wallet address sent`() = runTest {
            // Arrange
            coEvery { walletManagersFacade.getDefaultAddress(WALLET_ID, solanaNetwork) } returns null
            coEvery { stakeKitApi.getActions(any(), any(), any(), any(), any()) } returns ApiResponse.Success(
                GetActionsResponse(data = emptyList(), hasNextPage = false, limit = 50, page = 0),
            )

            // Act
            repository.getActions(WALLET_ID, solana, NetworkType.SOLANA, StakingActionStatus.PROCESSING)

            // Assert
            coVerify { stakeKitApi.getActions("", "solana", "PROCESSING", any(), any()) }
        }

        @Test
        fun `GIVEN unknown action status WHEN getActions THEN status UNKNOWN sent`() = runTest {
            // Arrange
            coEvery { walletManagersFacade.getDefaultAddress(WALLET_ID, solanaNetwork) } returns "addr-1"
            coEvery { stakeKitApi.getActions(any(), any(), any(), any(), any()) } returns ApiResponse.Success(
                GetActionsResponse(data = emptyList(), hasNextPage = false, limit = 50, page = 0),
            )

            // Act
            repository.getActions(WALLET_ID, solana, NetworkType.SOLANA, StakingActionStatus.UNKNOWN)

            // Assert
            coVerify { stakeKitApi.getActions("addr-1", "solana", "UNKNOWN", any(), any()) }
        }

        @Test
        fun `GIVEN api error WHEN getActions THEN throws api cause`() = runTest {
            // Arrange
            val cause = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.BAD_REQUEST,
                message = "bad",
                errorBody = "{}",
            )
            coEvery { walletManagersFacade.getDefaultAddress(WALLET_ID, solanaNetwork) } returns "addr-1"
            coEvery { stakeKitApi.getActions(any(), any(), any(), any(), any()) } returns apiError(cause)

            // Act
            val error = runCatching {
                repository.getActions(WALLET_ID, solana, NetworkType.SOLANA, StakingActionStatus.PROCESSING)
            }.exceptionOrNull()

            // Assert
            assertThat(error).isSameInstanceAs(cause)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateAction {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN action common type WHEN createAction THEN routed to matching endpoint`(model: RouteModel) = runTest {
            // Arrange
            stubAllCreateEndpoints()

            // Act
            val actual = repository.createAction(WALLET_ID, solanaNetwork, actionParams(type = model.type))

            // Assert
            assertThat(actual).isEqualTo(stakingAction())
            verifyCreateEndpoint(model.endpoint)
        }

        @Test
        fun `GIVEN enter params WHEN createAction THEN request body built from params`() = runTest {
            // Arrange
            val body = slot<ActionRequestBody>()
            coEvery { stakeKitApi.createEnterAction(capture(body)) } returns ApiResponse.Success(actionDTO())

            // Act
            repository.createAction(WALLET_ID, solanaNetwork, actionParams())

            // Assert
            assertThat(body.captured).isEqualTo(expectedEnterBody())
        }

        @Test
        fun `GIVEN amount in scientific notation WHEN createAction THEN plain string amount sent`() = runTest {
            // Arrange
            val body = slot<ActionRequestBody>()
            coEvery { stakeKitApi.createEnterAction(capture(body)) } returns ApiResponse.Success(actionDTO())

            // Act
            repository.createAction(WALLET_ID, solanaNetwork, actionParams(amount = BigDecimal("1E+3")))

            // Assert
            assertThat(body.captured.args.amount).isEqualTo("1000")
        }

        @Test
        fun `GIVEN pending params WHEN createAction THEN pending body built without token and tron resource`() =
            runTest {
                // Arrange
                val body = slot<PendingActionRequestBody>()
                coEvery { stakeKitApi.createPendingAction(capture(body)) } returns ApiResponse.Success(actionDTO())
                val params = actionParams(
                    type = StakingActionCommonType.Pending.Rewards,
                    pendingType = StakingActionType.CLAIM_REWARDS,
                    passthrough = "pass-1",
                )

                // Act
                repository.createAction(WALLET_ID, tronNetwork, params)

                // Assert
                assertThat(body.captured).isEqualTo(expectedPendingBody())
            }

        @Test
        fun `GIVEN pending params without type and passthrough WHEN createAction THEN UNKNOWN type and empty passthrough`() =
            runTest {
                // Arrange
                val body = slot<PendingActionRequestBody>()
                coEvery { stakeKitApi.createPendingAction(capture(body)) } returns ApiResponse.Success(actionDTO())

                // Act
                repository.createAction(
                    userWalletId = WALLET_ID,
                    network = solanaNetwork,
                    params = actionParams(type = StakingActionCommonType.Pending.Other),
                )

                // Assert
                val expected = expectedPendingBody().copy(type = StakingActionType.UNKNOWN, passthrough = "")
                assertThat(body.captured).isEqualTo(expected)
            }

        @Test
        fun `GIVEN pending params WHEN createAction THEN wallet manager not requested`() = runTest {
            // Arrange
            coEvery { stakeKitApi.createPendingAction(any()) } returns ApiResponse.Success(actionDTO())

            // Act
            repository.createAction(
                userWalletId = WALLET_ID,
                network = solanaNetwork,
                params = actionParams(type = StakingActionCommonType.Pending.Rewards),
            )

            // Assert
            coVerify(exactly = 0) { walletManagersFacade.getOrCreateWalletManager(any(), any<Network>()) }
        }

        @Test
        fun `GIVEN api error WHEN createAction THEN throws api cause`() = runTest {
            // Arrange
            val cause = ApiResponseError.TimeoutException()
            coEvery { stakeKitApi.createEnterAction(any()) } returns apiError(cause)

            // Act
            val error = runCatching { repository.createAction(WALLET_ID, solanaNetwork, actionParams()) }
                .exceptionOrNull()

            // Assert
            assertThat(error).isSameInstanceAs(cause)
        }

        private fun provideTestModels() = routeModels()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EstimateGas {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN action common type WHEN estimateGas THEN routed to matching endpoint`(model: RouteModel) = runTest {
            // Arrange
            stubAllEstimateEndpoints()

            // Act
            val actual = repository.estimateGas(WALLET_ID, solanaNetwork, actionParams(type = model.type))

            // Assert
            val expected = StakingGasEstimate(
                amount = BigDecimal("0.001"),
                token = YieldTokenConverter.convert(tokenDTO()),
                gasLimit = "21000",
            )
            assertThat(actual).isEqualTo(expected)
            verifyEstimateEndpoint(model.endpoint)
        }

        @Test
        fun `GIVEN enter params WHEN estimateGas THEN same request body as createAction`() = runTest {
            // Arrange
            val body = slot<ActionRequestBody>()
            coEvery { stakeKitApi.estimateGasOnEnter(capture(body)) } returns ApiResponse.Success(gasEstimateDTO())

            // Act
            repository.estimateGas(WALLET_ID, solanaNetwork, actionParams())

            // Assert
            assertThat(body.captured).isEqualTo(expectedEnterBody())
        }

        @Test
        fun `GIVEN pending params WHEN estimateGas THEN pending body sent`() = runTest {
            // Arrange
            val body = slot<PendingActionRequestBody>()
            coEvery { stakeKitApi.estimateGasOnPending(capture(body)) } returns ApiResponse.Success(gasEstimateDTO())
            val params = actionParams(
                type = StakingActionCommonType.Pending.Rewards,
                pendingType = StakingActionType.CLAIM_REWARDS,
                passthrough = "pass-1",
            )

            // Act
            repository.estimateGas(WALLET_ID, solanaNetwork, params)

            // Assert
            assertThat(body.captured).isEqualTo(expectedPendingBody())
        }

        @Test
        fun `GIVEN api error WHEN estimateGas THEN throws api cause`() = runTest {
            // Arrange
            val cause = ApiResponseError.TimeoutException()
            coEvery { stakeKitApi.estimateGasOnExit(any()) } returns apiError(cause)
            val params = actionParams(type = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false))

            // Act
            val error = runCatching { repository.estimateGas(WALLET_ID, solanaNetwork, params) }.exceptionOrNull()

            // Assert
            assertThat(error).isSameInstanceAs(cause)
        }

        private fun provideTestModels() = routeModels()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TronResourceRequest {

        @Test
        fun `GIVEN tron network WHEN createAction enter THEN ENERGY resource requested`() = runTest {
            // Arrange
            val body = slot<ActionRequestBody>()
            coEvery { stakeKitApi.createEnterAction(capture(body)) } returns ApiResponse.Success(actionDTO())

            // Act
            repository.createAction(WALLET_ID, tronNetwork, actionParams(token = MockStaking.trxToken))

            // Assert
            assertThat(body.captured.args.tronResource).isEqualTo(TronResource.ENERGY)
        }

        @Test
        fun `GIVEN tron testnet network WHEN createAction enter THEN ENERGY resource requested`() = runTest {
            // Arrange
            val body = slot<ActionRequestBody>()
            coEvery { stakeKitApi.createEnterAction(capture(body)) } returns ApiResponse.Success(actionDTO())

            // Act
            repository.createAction(WALLET_ID, tronTestnetNetwork, actionParams(token = MockStaking.trxToken))

            // Assert
            assertThat(body.captured.args.tronResource).isEqualTo(TronResource.ENERGY)
        }

        @Test
        fun `GIVEN solana network WHEN createAction enter THEN no tron resource`() = runTest {
            // Arrange
            val body = slot<ActionRequestBody>()
            coEvery { stakeKitApi.createEnterAction(capture(body)) } returns ApiResponse.Success(actionDTO())

            // Act
            repository.createAction(WALLET_ID, solanaNetwork, actionParams())

            // Assert
            assertThat(body.captured.args.tronResource).isNull()
        }
    }

    internal enum class ActionEndpoint { ENTER, EXIT, PENDING }

    internal data class RouteModel(val type: StakingActionCommonType, val endpoint: ActionEndpoint)

    private fun routeModels() = listOf(
        RouteModel(StakingActionCommonType.Enter(shouldSkipEnterAmount = false), ActionEndpoint.ENTER),
        RouteModel(StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false), ActionEndpoint.EXIT),
        RouteModel(StakingActionCommonType.Pending.Stake(shouldSkipEnterAmount = false), ActionEndpoint.PENDING),
        RouteModel(StakingActionCommonType.Pending.Restake, ActionEndpoint.PENDING),
        RouteModel(StakingActionCommonType.Pending.Rewards, ActionEndpoint.PENDING),
        RouteModel(StakingActionCommonType.Pending.Other, ActionEndpoint.PENDING),
    )

    private companion object {

        val WALLET_ID = UserWalletId("011")
    }
}