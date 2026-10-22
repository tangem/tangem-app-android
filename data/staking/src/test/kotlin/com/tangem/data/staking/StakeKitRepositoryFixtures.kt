package com.tangem.data.staking

import com.squareup.moshi.Moshi
import com.tangem.common.test.data.staking.MockYieldDTOFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionStatus
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.grow.datasource.stakekit.addStakeKitEnumFallbackAdapters
import com.tangem.grow.datasource.stakekit.models.response.ActionDTO
import com.tangem.grow.datasource.stakekit.models.response.EnabledYieldsResponse
import com.tangem.grow.datasource.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.TokenDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.ValidatorDTO.ValidatorStatusDTO
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionStatusDTO
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingGasEstimateDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionStatusDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionTypeDTO
import com.tangem.test.mock.staking.MockStaking
import org.joda.time.DateTime
import java.math.BigDecimal

@Suppress("TooManyFunctions")
internal object StakeKitRepositoryFixtures {

    val moshi: Moshi = Moshi.Builder().addStakeKitEnumFallbackAdapters().build()

    fun tokenDTO(
        network: NetworkTypeDTO = NetworkTypeDTO.SOLANA,
        symbol: String = "SOL",
        decimals: Int = 9,
        coinGeckoId: String? = "solana",
        address: String? = null,
    ) = TokenDTO(
        name = symbol,
        network = network,
        symbol = symbol,
        decimals = decimals,
        address = address,
        coinGeckoId = coinGeckoId,
        logoURI = null,
        isPoints = null,
    )

    fun validatorDTO(
        address: String? = MockStaking.VALIDATOR_ADDRESS,
        status: YieldDTO.ValidatorDTO.ValidatorStatusDTO? = ValidatorStatusDTO.ACTIVE,
        name: String? = "Validator 1",
        apr: BigDecimal? = BigDecimal("0.05"),
        commission: Double? = null,
        preferred: Boolean? = true,
    ) = YieldDTO.ValidatorDTO(
        address = address,
        status = status,
        name = name,
        image = null,
        website = null,
        apr = apr,
        commission = commission,
        stakedBalance = null,
        votingPower = null,
        preferred = preferred,
    )

    fun yieldDTO(
        id: String? = MockStaking.SOLANA_ID,
        token: TokenDTO = tokenDTO(),
        tokens: List<TokenDTO> = listOf(token),
        validators: List<YieldDTO.ValidatorDTO> = listOf(validatorDTO()),
        isAvailable: Boolean? = true,
    ): YieldDTO = MockYieldDTOFactory.create().copy(
        id = id,
        token = token,
        tokens = tokens,
        validators = validators,
        isAvailable = isAvailable,
    )

    fun enabledYields(vararg yields: YieldDTO) = EnabledYieldsResponse(
        data = yields.toList(),
        hasNextPage = false,
        limit = 100,
        page = 0,
    )

    fun transactionDTO(
        unsignedTransaction: String? = "0a0b",
        network: NetworkTypeDTO = NetworkTypeDTO.SOLANA,
    ) = StakingTransactionDTO(
        id = "tx-1",
        network = network,
        status = StakingTransactionStatusDTO.CREATED,
        type = StakingTransactionTypeDTO.STAKE,
        hash = null,
        signedTransaction = null,
        unsignedTransaction = unsignedTransaction,
        stepIndex = 0,
        error = null,
        gasEstimate = null,
        stakeId = null,
        explorerUrl = null,
        ledgerHwAppId = null,
        isMessage = false,
    )

    fun stakingTransaction(
        unsignedTransaction: String? = "0a0b",
        network: NetworkType = NetworkType.SOLANA,
    ) = StakingTransaction(
        id = "tx-1",
        network = network,
        status = StakingTransactionStatus.CREATED,
        type = StakingTransactionType.STAKE,
        hash = null,
        signedTransaction = null,
        unsignedTransaction = unsignedTransaction,
        stepIndex = 0,
        error = null,
        gasEstimate = null,
        stakeId = null,
        explorerUrl = null,
        ledgerHwAppId = null,
        isMessage = false,
    )

    fun actionDTO(id: String = "action-1", integrationId: String = MockStaking.SOLANA_ID) = ActionDTO(
        id = id,
        integrationId = integrationId,
        status = StakingActionStatusDTO.PROCESSING,
        type = StakingActionTypeDTO.STAKE,
        currentStepIndex = 0,
        amount = BigDecimal.ONE,
        validatorAddress = MockStaking.VALIDATOR_ADDRESS,
        validatorAddresses = null,
        transactions = null,
        createdAt = DateTime(0L),
    )

    fun stakingAction(id: String = "action-1", integrationId: String = MockStaking.SOLANA_ID): StakingAction =
        MockStaking.stakingAction(
            type = StakingActionType.STAKE,
            status = StakingActionStatus.PROCESSING,
            id = id,
            integrationId = integrationId,
        )

    fun gasEstimateDTO(amount: BigDecimal = BigDecimal("0.001"), gasLimit: String? = "21000") = StakingGasEstimateDTO(
        amount = amount,
        token = tokenDTO(),
        gasLimit = gasLimit,
    )

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> apiError(cause: ApiResponseError = ApiResponseError.NetworkException()): ApiResponse<T> =
        ApiResponse.Error(cause) as ApiResponse<T>
}