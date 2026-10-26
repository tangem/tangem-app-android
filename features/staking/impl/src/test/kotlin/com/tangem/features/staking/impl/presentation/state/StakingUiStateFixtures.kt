package com.tangem.features.staking.impl.presentation.state

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.RewardBlockType
import com.tangem.domain.staking.StakingTransactionVerdict
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.toStakingTarget
import com.tangem.domain.tokens.model.Amount
import com.tangem.features.staking.impl.fixtures.ethFee
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmationStatePreviewData
import com.tangem.features.staking.impl.presentation.state.stub.StakingClickIntentsStub
import com.tangem.test.mock.staking.MockStaking
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

/**
 * A **real** [StakingUiState] on the assent Confirmation step. Transformers call `copy()`, which MockK intercepts on
 * a mocked data class, so applying a transformer to a mocked state would read nothing.
 */
internal fun createAssentConfirmationUiState(
    actionType: StakingActionCommonType = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false),
) = StakingUiState(
    title = TextReference.EMPTY,
    subtitle = null,
    clickIntents = StakingClickIntentsStub,
    walletName = "",
    cryptoCurrencyName = "",
    cryptoCurrencySymbol = "",
    cryptoCurrencyBlockchainId = "",
    currentStep = StakingStep.Confirmation,
    initialInfoState = StakingStates.InitialInfoState.Empty(),
    amountState = mockk<AmountState.Data>(relaxed = true),
    validatorState = StakingStates.ValidatorState.Empty(),
    rewardsValidatorsState = StakingStates.RewardsValidatorsState.Empty(),
    confirmationState = ConfirmationStatePreviewData.assentStakingState,
    isBalanceHidden = false,
    bottomSheetConfig = null,
    actionType = actionType,
    buttonsState = NavigationButtonsState.Empty,
    balanceState = null,
    isColdWalletInteractionIconVisible = true,
    shouldShowHoldToConfirmButton = false,
)

@Suppress("LongParameterList")
internal fun stakingUiState(
    currentStep: StakingStep = StakingStep.InitialInfo,
    actionType: StakingActionCommonType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
    cryptoCurrencyName: String = "Solana",
    cryptoCurrencySymbol: String = "SOL",
    cryptoCurrencyBlockchainId: String = Blockchain.Solana.toNetworkId(),
    walletName: String = "Wallet",
    clickIntents: StakingClickIntents = StakingClickIntentsStub,
    initialInfoState: StakingStates.InitialInfoState = StakingStates.InitialInfoState.Empty(),
    amountState: AmountState = AmountState.Empty,
    rewardsValidatorsState: StakingStates.RewardsValidatorsState = StakingStates.RewardsValidatorsState.Empty(),
    confirmationState: StakingStates.ConfirmationState = StakingStates.ConfirmationState.Empty(),
    validatorState: StakingStates.ValidatorState = StakingStates.ValidatorState.Empty(),
    balanceState: BalanceState? = null,
    bottomSheetConfig: TangemBottomSheetConfig? = null,
    isColdWalletInteractionIconVisible: Boolean = true,
    shouldShowHoldToConfirmButton: Boolean = false,
): StakingUiState = StakingUiState(
    title = TextReference.EMPTY,
    subtitle = null,
    clickIntents = clickIntents,
    walletName = walletName,
    cryptoCurrencyName = cryptoCurrencyName,
    cryptoCurrencySymbol = cryptoCurrencySymbol,
    cryptoCurrencyBlockchainId = cryptoCurrencyBlockchainId,
    currentStep = currentStep,
    initialInfoState = initialInfoState,
    amountState = amountState,
    rewardsValidatorsState = rewardsValidatorsState,
    confirmationState = confirmationState,
    validatorState = validatorState,
    isBalanceHidden = false,
    bottomSheetConfig = bottomSheetConfig,
    actionType = actionType,
    buttonsState = NavigationButtonsState.Empty,
    balanceState = balanceState,
    isColdWalletInteractionIconVisible = isColdWalletInteractionIconVisible,
    shouldShowHoldToConfirmButton = shouldShowHoldToConfirmButton,
)

@Suppress("LongParameterList")
internal fun amountData(
    cryptoAmount: BigDecimal? = BigDecimal.ONE,
    value: String = cryptoAmount?.toPlainString().orEmpty(),
    fiatAmount: BigDecimal? = null,
    isFiatValue: Boolean = false,
    isError: Boolean = false,
    isWarning: Boolean = false,
    error: TextReference = TextReference.EMPTY,
    cryptoDecimals: Int = 9,
    fiatDecimals: Int = 2,
    isPrimaryButtonEnabled: Boolean = true,
    reduceAmountBy: BigDecimal = BigDecimal.ZERO,
): AmountState.Data = AmountState.Data(
    isPrimaryButtonEnabled = isPrimaryButtonEnabled,
    accountTitleUM = AccountTitleUM.Text(TextReference.EMPTY),
    availableBalanceCrypto = TextReference.EMPTY,
    availableBalanceFiat = TextReference.EMPTY,
    tokenName = TextReference.EMPTY,
    tokenIconState = mockk(relaxed = true),
    amountTextField = AmountFieldModel(
        value = value,
        onValueChange = {},
        keyboardOptions = KeyboardOptions.Default,
        keyboardActions = KeyboardActions.Default,
        cryptoAmount = Amount(currencySymbol = "SOL", value = cryptoAmount, decimals = cryptoDecimals),
        fiatAmount = Amount(currencySymbol = "USD", value = fiatAmount, decimals = fiatDecimals),
        isFiatValue = isFiatValue,
        fiatValue = fiatAmount?.toPlainString().orEmpty(),
        isFiatUnavailable = false,
        isValuePasted = false,
        onValuePastedTriggerDismiss = {},
        isError = isError,
        isWarning = isWarning,
        error = error,
    ),
    appCurrency = AppCurrency.Default,
    reduceAmountBy = reduceAmountBy,
)

internal fun feeContent(fee: Fee? = ethFee(BigDecimal("0.01")), isFeeApproximate: Boolean = false) = FeeState.Content(
    fee = fee,
    rate = null,
    isFeeConvertibleToFiat = false,
    appCurrency = AppCurrency.Default,
    isFeeApproximate = isFeeApproximate,
)

@Suppress("LongParameterList")
internal fun confirmationData(
    pendingAction: PendingAction? = null,
    pendingActions: ImmutableList<PendingAction>? = null,
    innerState: InnerConfirmationStakingState = InnerConfirmationStakingState.ASSENT,
    feeState: FeeState = FeeState.Loading,
    notifications: ImmutableList<NotificationUM> = persistentListOf(),
    isApprovalNeeded: Boolean = false,
    allowance: BigDecimal = BigDecimal.ZERO,
    reduceAmountBy: BigDecimal? = null,
    transactionDoneState: TransactionDoneState = TransactionDoneState.Empty,
    isPrimaryButtonEnabled: Boolean = true,
    isValidationInProgress: Boolean = false,
    transactionVerdict: StakingTransactionVerdict? = null,
) = StakingStates.ConfirmationState.Data(
    isPrimaryButtonEnabled = isPrimaryButtonEnabled,
    innerState = innerState,
    feeState = feeState,
    pendingAction = pendingAction,
    pendingActions = pendingActions,
    notifications = notifications,
    footerText = TextReference.EMPTY,
    transactionDoneState = transactionDoneState,
    isApprovalNeeded = isApprovalNeeded,
    isAmountEditable = true,
    allowance = allowance,
    reduceAmountBy = reduceAmountBy,
    isValidationInProgress = isValidationInProgress,
    transactionVerdict = transactionVerdict,
)

internal fun target(validator: Yield.Validator = MockStaking.validator()): StakingTarget = validator.toStakingTarget()

@Suppress("LongParameterList")
internal fun balanceState(
    type: BalanceType = BalanceType.STAKED,
    cryptoAmount: BigDecimal = BigDecimal.ONE,
    fiatAmount: BigDecimal? = null,
    target: StakingTarget? = target(),
    pendingActions: List<PendingAction> = emptyList(),
    isPending: Boolean = false,
    groupId: String = "group-1",
) = BalanceState(
    groupId = groupId,
    title = stringReference(target?.name.orEmpty()),
    type = type,
    subtitle = null,
    isClickable = true,
    cryptoValue = cryptoAmount.toPlainString(),
    cryptoAmount = cryptoAmount,
    formattedCryptoAmount = TextReference.EMPTY,
    fiatAmount = fiatAmount,
    formattedFiatAmount = TextReference.EMPTY,
    rawCurrencyId = null,
    target = target,
    pendingActions = pendingActions.toPersistentList(),
    isPending = isPending,
    targetAddress = target?.address,
)

internal fun yieldBalanceData(balances: List<BalanceState> = emptyList()) = InnerYieldBalanceState.Data(
    integrationId = MockStaking.SOLANA_ID,
    reward = YieldReward(
        rewardsCrypto = "",
        rewardsFiat = "",
        rewardBlockType = RewardBlockType.NoRewards,
        rewardConstraints = null,
    ),
    isActionable = false,
    balances = balances.toPersistentList(),
)

internal fun initialInfoData(
    yieldBalance: InnerYieldBalanceState = InnerYieldBalanceState.Empty,
    isPrimaryButtonEnabled: Boolean = true,
    areAllTargetsFull: Boolean = false,
) = StakingStates.InitialInfoState.Data(
    isPrimaryButtonEnabled = isPrimaryButtonEnabled,
    isBannerVisible = false,
    infoItems = persistentListOf(),
    onInfoClick = {},
    yieldBalance = yieldBalance,
    pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
    legalUrls = StakingStates.InitialInfoState.LegalUrls(termsOfServiceUrl = "tos", privacyPolicyUrl = "pp"),
    areAllTargetsFull = areAllTargetsFull,
)

internal fun validatorData(
    chosenTarget: StakingTarget = target(),
    activeTarget: StakingTarget? = null,
    availableTargets: List<StakingTarget> = listOf(chosenTarget),
) = StakingStates.ValidatorState.Data(
    isPrimaryButtonEnabled = true,
    isClickable = true,
    isVisibleOnConfirmation = true,
    chosenTarget = chosenTarget,
    activeTarget = activeTarget,
    availableTargets = availableTargets,
)