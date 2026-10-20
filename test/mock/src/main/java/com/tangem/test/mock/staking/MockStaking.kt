package com.tangem.test.mock.staking

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.staking.P2PEthPoolExitQueue
import com.tangem.domain.models.staking.P2PEthPoolExitRequest
import com.tangem.domain.models.staking.P2PEthPoolStake
import com.tangem.domain.models.staking.P2PEthPoolStakingAccount
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.ethpool.VaultLimitInfo
import com.tangem.domain.staking.model.stakekit.AddressArgument
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import kotlinx.datetime.Instant
import org.joda.time.DateTime
import java.math.BigDecimal

/**
 * Staking fixtures (StakeKit yields, P2P vaults, balances, actions), every argument defaulted so a test states only
 * what it is about.
 *
 * The defaults describe a Solana yield with one active preferred validator and a staked balance of one coin.
 */
@Suppress("TooManyFunctions")
object MockStaking {

    const val SOLANA_ID = "solana-sol-native-multivalidator-staking"
    const val COSMOS_ID = "cosmos-atom-native-staking"
    const val TRON_ID = "tron-trx-native-staking"
    const val TON_ID = "ton-ton-chorus-one-pools-staking"
    const val BSC_ID = "bsc-bnb-native-staking"
    const val CARDANO_ID = "cardano-ada-native-staking"
    const val POLYGON_ID = "ethereum-matic-native-staking"
    const val P2P_ID = "p2p-ethereum-pooled"

    const val VALIDATOR_ADDRESS = "validator-1"
    const val SECOND_VALIDATOR_ADDRESS = "validator-2"
    const val VAULT_ADDRESS = "0xvault1"
    const val DELEGATOR_ADDRESS = "0xdelegator"
    const val POLYGON_CONTRACT = "0x455e53CBB86018Ac2B8092FdCd39d8444aFFC3F6"

    val solToken: YieldToken = yieldToken()
    val atomToken: YieldToken = yieldToken(
        network = NetworkType.COSMOS,
        symbol = "ATOM",
        decimals = 6,
        coinGeckoId = "cosmos",
    )
    val trxToken: YieldToken = yieldToken(
        network = NetworkType.TRON,
        symbol = "TRX",
        decimals = 6,
        coinGeckoId = "tron",
    )
    val tonToken: YieldToken = yieldToken(
        network = NetworkType.TON,
        symbol = "TON",
        decimals = 9,
        coinGeckoId = "the-open-network",
    )
    val bnbToken: YieldToken = yieldToken(
        network = NetworkType.BINANCE,
        symbol = "BNB",
        decimals = 18,
        coinGeckoId = "binancecoin",
    )
    val adaToken: YieldToken = yieldToken(
        network = NetworkType.CARDANO,
        symbol = "ADA",
        decimals = 6,
        coinGeckoId = "cardano",
    )
    val polToken: YieldToken = yieldToken(
        network = NetworkType.ETHEREUM,
        symbol = "POL",
        decimals = 18,
        coinGeckoId = "matic-network",
        address = POLYGON_CONTRACT,
    )

    val solanaYield: Yield = yield(id = SOLANA_ID, token = solToken, metadata = metadata(solToken))
    val cosmosYield: Yield = yield(id = COSMOS_ID, token = atomToken, metadata = metadata(atomToken))
    val tronYield: Yield = yield(id = TRON_ID, token = trxToken, metadata = metadata(trxToken))
    val tonYield: Yield = yield(id = TON_ID, token = tonToken, metadata = metadata(tonToken))
    val bscYield: Yield = yield(id = BSC_ID, token = bnbToken, metadata = metadata(bnbToken))
    val cardanoYield: Yield = yield(id = CARDANO_ID, token = adaToken, metadata = metadata(adaToken))
    val polygonYield: Yield = yield(id = POLYGON_ID, token = polToken, metadata = metadata(polToken))

    @Suppress("LongParameterList")
    fun yieldToken(
        network: NetworkType = NetworkType.SOLANA,
        symbol: String = "SOL",
        decimals: Int = 9,
        coinGeckoId: String? = "solana",
        address: String? = null,
        name: String = symbol,
    ): YieldToken = YieldToken(
        name = name,
        network = network,
        symbol = symbol,
        decimals = decimals,
        address = address,
        coinGeckoId = coinGeckoId,
        logoURI = null,
        isPoints = false,
    )

    @Suppress("LongParameterList")
    fun validator(
        address: String = VALIDATOR_ADDRESS,
        name: String = "Validator 1",
        status: Yield.Validator.ValidatorStatus = Yield.Validator.ValidatorStatus.ACTIVE,
        preferred: Boolean = true,
        isStrategicPartner: Boolean = false,
        rewardInfo: RewardInfo? = RewardInfo(BigDecimal("0.05"), RewardType.APR),
        image: String? = null,
        website: String? = null,
        commission: Double? = null,
    ): Yield.Validator = Yield.Validator(
        address = address,
        status = status,
        name = name,
        image = image,
        website = website,
        rewardInfo = rewardInfo,
        commission = commission,
        isPreferred = preferred,
        isStrategicPartner = isStrategicPartner,
    )

    fun amountArgument(
        minimum: BigDecimal? = BigDecimal.ONE,
        maximum: BigDecimal? = null,
        required: Boolean = true,
    ): AddressArgument = AddressArgument(isRequired = required, minimum = minimum, maximum = maximum)

    fun enterArgs(minimum: BigDecimal? = BigDecimal.ONE, maximum: BigDecimal? = null): Yield.Args.Enter {
        return Yield.Args.Enter(
            addresses = Yield.Args.Enter.Addresses(address = AddressArgument(isRequired = true)),
            args = mapOf(Yield.Args.ArgType.AMOUNT to amountArgument(minimum, maximum)),
        )
    }

    @Suppress("LongParameterList")
    fun metadata(
        token: YieldToken = solToken,
        rewardSchedule: Yield.Metadata.RewardSchedule = Yield.Metadata.RewardSchedule.DAY,
        rewardClaiming: Yield.Metadata.RewardClaiming = Yield.Metadata.RewardClaiming.AUTO,
        cooldownPeriod: Yield.Metadata.Period? = Yield.Metadata.Period(days = 3, seconds = null),
        warmupPeriod: Yield.Metadata.Period = Yield.Metadata.Period(days = 0, seconds = null),
        minimumStake: Int? = null,
    ): Yield.Metadata = Yield.Metadata(
        name = "${token.symbol} staking",
        logoUri = "",
        description = "",
        documentation = null,
        gasFeeToken = token,
        token = token,
        tokens = listOf(token),
        type = "staking",
        rewardSchedule = rewardSchedule,
        cooldownPeriod = cooldownPeriod,
        warmupPeriod = warmupPeriod,
        rewardClaiming = rewardClaiming,
        defaultValidator = null,
        minimumStake = minimumStake,
        isMultipleValidatorsSupported = true,
        revshare = Yield.Metadata.Enabled(false),
        fee = Yield.Metadata.Enabled(false),
    )

    @Suppress("LongParameterList")
    fun yield(
        id: String = SOLANA_ID,
        token: YieldToken = solToken,
        validators: List<Yield.Validator> = listOf(validator()),
        allValidators: List<Yield.Validator> = validators,
        enter: Yield.Args.Enter = enterArgs(),
        exit: Yield.Args.Enter? = enterArgs(),
        metadata: Yield.Metadata = metadata(token),
        apy: BigDecimal = BigDecimal("0.07"),
        rewardType: RewardType = RewardType.APR,
        isAvailable: Boolean = true,
    ): Yield = Yield(
        id = id,
        token = token,
        tokens = listOf(token),
        args = Yield.Args(enter = enter, exit = exit),
        status = Yield.Status(canEnter = true, canExit = true),
        apy = apy,
        rewardRate = apy.toDouble(),
        rewardType = rewardType,
        metadata = metadata,
        validators = validators,
        isAvailable = isAvailable,
        allValidators = allValidators,
    )

    fun yieldFor(integrationId: String): Yield = when (integrationId) {
        SOLANA_ID -> solanaYield
        COSMOS_ID -> cosmosYield
        TRON_ID -> tronYield
        TON_ID -> tonYield
        BSC_ID -> bscYield
        CARDANO_ID -> cardanoYield
        POLYGON_ID -> polygonYield
        else -> error("No staking yield fixture for integration id: $integrationId")
    }

    fun vault(
        vaultAddress: String = VAULT_ADDRESS,
        displayName: String = "Vault 1",
        apy: BigDecimal = BigDecimal("0.03"),
        capacity: BigDecimal = BigDecimal("1000"),
        totalAssets: BigDecimal = BigDecimal("100"),
    ): P2PEthPoolVault = P2PEthPoolVault(
        vaultAddress = vaultAddress,
        displayName = displayName,
        apy = apy,
        baseApy = apy,
        capacity = capacity,
        totalAssets = totalAssets,
        feePercent = BigDecimal("10"),
        isPrivate = false,
        isGenesis = false,
        isSmoothingPool = false,
        isErc20 = false,
        tokenName = null,
        tokenSymbol = null,
        createdAt = 0L,
    )

    fun vaultLimit(limit: BigDecimal = BigDecimal("200"), coefficient: BigDecimal? = null): VaultLimitInfo {
        return VaultLimitInfo(limit = limit, coefficient = coefficient)
    }

    fun pendingAction(
        type: StakingActionType = StakingActionType.CLAIM_REWARDS,
        passthrough: String = "passthrough-1",
        args: PendingAction.PendingActionArgs? = null,
    ): PendingAction = PendingAction(type = type, passthrough = passthrough, args = args)

    @Suppress("LongParameterList")
    fun balanceItem(
        type: BalanceType = BalanceType.STAKED,
        amount: BigDecimal = BigDecimal.ONE,
        validatorAddress: String? = VALIDATOR_ADDRESS,
        token: YieldToken = solToken,
        pendingActions: List<PendingAction> = emptyList(),
        isPending: Boolean = false,
        groupId: String = "group-1",
        date: Instant? = null,
        rawCurrencyId: String? = null,
    ): BalanceItem = BalanceItem(
        groupId = groupId,
        token = token,
        type = type,
        amount = amount,
        rawCurrencyId = rawCurrencyId,
        validatorAddress = validatorAddress,
        date = date,
        pendingActions = pendingActions,
        pendingActionsConstraints = emptyList(),
        isPending = isPending,
    )

    fun stakingId(integrationId: String = SOLANA_ID, address: String = DELEGATOR_ADDRESS): StakingID {
        return StakingID(integrationId = integrationId, address = address)
    }

    fun stakeKitBalance(
        items: List<BalanceItem> = listOf(balanceItem()),
        integrationId: String = SOLANA_ID,
        address: String = DELEGATOR_ADDRESS,
        source: StatusSource = StatusSource.ACTUAL,
    ): StakingBalance.Data.StakeKit = StakingBalance.Data.StakeKit(
        stakingId = stakingId(integrationId, address),
        source = source,
        balance = YieldBalanceItem(items = items, integrationId = integrationId),
    )

    @Suppress("LongParameterList")
    fun p2pAccount(
        vaultAddress: String = VAULT_ADDRESS,
        delegatorAddress: String = DELEGATOR_ADDRESS,
        staked: BigDecimal = BigDecimal.ONE,
        earned: BigDecimal = BigDecimal.ZERO,
        availableToUnstake: BigDecimal = staked,
        availableToWithdraw: BigDecimal = BigDecimal.ZERO,
        exitQueueTotal: BigDecimal = BigDecimal.ZERO,
        exitRequests: List<P2PEthPoolExitRequest> = emptyList(),
    ): P2PEthPoolStakingAccount = P2PEthPoolStakingAccount(
        delegatorAddress = delegatorAddress,
        vaultAddress = vaultAddress,
        stake = P2PEthPoolStake(assets = staked, totalEarnedAssets = earned),
        availableToUnstake = availableToUnstake,
        availableToWithdraw = availableToWithdraw,
        exitQueue = P2PEthPoolExitQueue(total = exitQueueTotal, requests = exitRequests),
    )

    fun p2pBalance(
        accounts: List<P2PEthPoolStakingAccount> = listOf(p2pAccount()),
        address: String = DELEGATOR_ADDRESS,
        source: StatusSource = StatusSource.ACTUAL,
    ): StakingBalance.Data.P2PEthPool = StakingBalance.Data.P2PEthPool(
        stakingId = stakingId(P2P_ID, address),
        source = source,
        accounts = accounts,
    )

    @Suppress("LongParameterList")
    fun stakingAction(
        type: StakingActionType = StakingActionType.STAKE,
        status: StakingActionStatus = StakingActionStatus.PROCESSING,
        amount: BigDecimal = BigDecimal.ONE,
        validatorAddress: String? = VALIDATOR_ADDRESS,
        validatorAddresses: List<String>? = null,
        integrationId: String = SOLANA_ID,
        id: String = "action-1",
        transactions: List<StakingTransaction>? = null,
    ): StakingAction = StakingAction(
        id = id,
        integrationId = integrationId,
        status = status,
        type = type,
        currentStepIndex = 0,
        amount = amount,
        validatorAddress = validatorAddress,
        validatorAddresses = validatorAddresses,
        transactions = transactions,
        createdAt = DateTime(0L),
    )
}