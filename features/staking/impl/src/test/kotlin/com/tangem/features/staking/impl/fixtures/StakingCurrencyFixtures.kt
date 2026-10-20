package com.tangem.features.staking.impl.fixtures

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingBalanceEntry
import com.tangem.domain.models.staking.StakingEntryActions
import com.tangem.domain.models.staking.StakingEntryType
import com.tangem.domain.models.staking.ValidatorInfo
import com.tangem.test.mock.staking.MockStaking
import kotlinx.datetime.Instant
import java.math.BigDecimal

internal val currencyFactory: MockCryptoCurrencyFactory = MockCryptoCurrencyFactory()

internal fun coin(blockchain: Blockchain): CryptoCurrency.Coin = currencyFactory.createCoin(blockchain)

internal fun polygonToken(): CryptoCurrency.Token = currencyFactory.createToken(
    blockchain = Blockchain.Ethereum,
    id = "polygon-ecosystem-token",
    contractAddress = MockStaking.POLYGON_CONTRACT,
    symbol = "POL",
    decimals = 18,
)

internal fun currencyStatus(
    currency: CryptoCurrency = coin(Blockchain.Solana),
    amount: BigDecimal? = BigDecimal("10"),
    fiatRate: BigDecimal? = BigDecimal("2"),
    stakingBalance: StakingBalance? = null,
    networkSource: StatusSource = StatusSource.ACTUAL,
): CryptoCurrencyStatus {
    val networkAddress = NetworkAddress.Single(
        NetworkAddress.Address("address", NetworkAddress.Address.Type.Primary),
    )
    val sources = CryptoCurrencyStatus.Sources(networkSource = networkSource)
    val value = when {
        amount == null -> CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = fiatRate)
        fiatRate != null -> CryptoCurrencyStatus.Loaded(
            amount = amount,
            fiatAmount = amount * fiatRate,
            fiatRate = fiatRate,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = networkAddress,
            sources = sources,
            contributions = listOfNotNull(stakingBalance),
        )
        else -> CryptoCurrencyStatus.NoQuote(
            amount = amount,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = networkAddress,
            sources = sources,
            contributions = listOfNotNull(stakingBalance),
        )
    }
    return CryptoCurrencyStatus(currency = currency, value = value)
}

@Suppress("LongParameterList")
internal fun stakeKitEntry(
    type: StakingEntryType = StakingEntryType.STAKED,
    amount: BigDecimal = BigDecimal.ONE,
    validatorAddress: String? = MockStaking.VALIDATOR_ADDRESS,
    validatorName: String? = null,
    date: Instant? = null,
    pendingActions: List<PendingAction> = emptyList(),
    isPending: Boolean = false,
    id: String = "group-1",
    rawCurrencyId: String? = null,
): StakingBalanceEntry = StakingBalanceEntry(
    id = id,
    type = type,
    amount = amount,
    validator = validatorAddress?.let { ValidatorInfo(it, validatorName) },
    date = date,
    actions = StakingEntryActions.StakeKit(pendingActions, pendingActionsConstraints = emptyList()),
    isPending = isPending,
    rawCurrencyId = rawCurrencyId,
)

internal fun p2pEntry(
    type: StakingEntryType = StakingEntryType.STAKED,
    amount: BigDecimal = BigDecimal.ONE,
    vaultAddress: String? = MockStaking.VAULT_ADDRESS,
    vaultName: String? = "Vault 1",
    isPending: Boolean = false,
    id: String = "p2p-1",
): StakingBalanceEntry = StakingBalanceEntry(
    id = id,
    type = type,
    amount = amount,
    validator = vaultAddress?.let { ValidatorInfo(it, vaultName) },
    date = null,
    actions = StakingEntryActions.P2PEthPool(ticket = null, estimatedWithdrawalDate = null, isClaimable = false),
    isPending = isPending,
    rawCurrencyId = null,
)

internal fun ethFee(value: BigDecimal?, symbol: String = "SOL", decimals: Int = 9): Fee =
    Fee.Common(amount = Amount(currencySymbol = symbol, value = value, decimals = decimals))