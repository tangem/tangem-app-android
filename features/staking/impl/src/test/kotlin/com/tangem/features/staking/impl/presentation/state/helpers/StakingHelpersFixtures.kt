package com.tangem.features.staking.impl.presentation.state.helpers

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.ethpool.P2PEthPoolUnsignedTx
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.toStakingTarget
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.createAssentConfirmationUiState
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmationStatePreviewData
import com.tangem.test.mock.staking.MockStaking
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal const val SOURCE_ADDRESS = "0xsource"

private val currencyFactory = MockCryptoCurrencyFactory()

internal val solanaCoin: CryptoCurrency.Coin = currencyFactory.createCoin(Blockchain.Solana)
internal val tronCoin: CryptoCurrency.Coin = currencyFactory.createCoin(Blockchain.Tron)
internal val ethereumCoin: CryptoCurrency.Coin = currencyFactory.createCoin(Blockchain.Ethereum)
internal val polygonToken: CryptoCurrency.Token = currencyFactory.createToken(
    blockchain = Blockchain.Ethereum,
    id = "matic-network",
    contractAddress = MockStaking.POLYGON_CONTRACT,
    symbol = "POL",
    decimals = 18,
)
internal val realUserWallet = MockUserWalletFactory.create()

internal fun status(
    currency: CryptoCurrency = solanaCoin,
    address: String? = SOURCE_ADDRESS,
    balance: BigDecimal = BigDecimal.TEN,
) = CryptoCurrencyStatus(
    currency = currency,
    value = if (address == null) {
        CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null)
    } else {
        CryptoCurrencyStatus.Loaded(
            amount = balance,
            fiatAmount = BigDecimal.ZERO,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                NetworkAddress.Address(address, NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        )
    },
)

internal fun fee(value: BigDecimal = BigDecimal.ONE, symbol: String = "SOL", decimals: Int = 9) =
    Fee.Common(Amount(currencySymbol = symbol, value = value, decimals = decimals))

internal fun confirmationData(
    fee: Fee? = fee(),
    feeState: FeeState = (ConfirmationStatePreviewData.assentStakingState.feeState as FeeState.Content).copy(fee = fee),
    pendingAction: PendingAction? = null,
    pendingActions: ImmutableList<PendingAction>? = null,
    isApprovalNeeded: Boolean = false,
    allowance: BigDecimal = BigDecimal.ZERO,
    reduceAmountBy: BigDecimal? = null,
) = ConfirmationStatePreviewData.assentStakingState.copy(
    feeState = feeState,
    pendingAction = pendingAction,
    pendingActions = pendingActions,
    isApprovalNeeded = isApprovalNeeded,
    allowance = allowance,
    reduceAmountBy = reduceAmountBy,
)

internal fun amountData(value: BigDecimal?): AmountState.Data = mockk(relaxed = true) {
    every { amountTextField.cryptoAmount.value } returns value
}

internal fun validatorData(target: StakingTarget = MockStaking.validator().toStakingTarget()) =
    StakingStates.ValidatorState.Data(
        isPrimaryButtonEnabled = true,
        isClickable = true,
        isVisibleOnConfirmation = true,
        chosenTarget = target,
        activeTarget = null,
        availableTargets = listOf(target),
    )

internal fun balanceState(targetAddress: String?) = BalanceState(
    groupId = "group-1",
    title = TextReference.EMPTY,
    type = BalanceType.STAKED,
    subtitle = null,
    isClickable = true,
    cryptoValue = "1",
    cryptoAmount = BigDecimal.ONE,
    formattedCryptoAmount = TextReference.EMPTY,
    fiatAmount = null,
    formattedFiatAmount = TextReference.EMPTY,
    rawCurrencyId = null,
    target = null,
    pendingActions = persistentListOf(),
    isPending = false,
    targetAddress = targetAddress,
)

internal fun uiState(
    confirmationState: StakingStates.ConfirmationState = confirmationData(),
    amountState: AmountState = amountData(BigDecimal.TEN),
    validatorState: StakingStates.ValidatorState = validatorData(),
    balanceState: BalanceState? = null,
    actionType: StakingActionCommonType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
): StakingUiState = createAssentConfirmationUiState(actionType).copy(
    confirmationState = confirmationState,
    amountState = amountState,
    validatorState = validatorState,
    balanceState = balanceState,
)

internal fun unsignedTx(
    value: BigDecimal = BigDecimal("1000000000000000000"),
    gasLimit: BigDecimal = BigDecimal("21000"),
    maxFeePerGas: BigDecimal = BigDecimal("30000000000"),
) = P2PEthPoolUnsignedTx(
    serializeTx = "0xserialized",
    to = "0xto",
    data = "0xdeadbeef",
    value = value,
    nonce = 5,
    chainId = 1,
    gasLimit = gasLimit,
    maxFeePerGas = maxFeePerGas,
    maxPriorityFeePerGas = BigDecimal("1500000000"),
)