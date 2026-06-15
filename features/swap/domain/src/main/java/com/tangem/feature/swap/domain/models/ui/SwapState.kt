package com.tangem.feature.swap.domain.models.ui

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExpressTxType
import com.tangem.feature.swap.domain.models.domain.PreparedSwapConfigState
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import java.math.BigDecimal

sealed interface SwapState {

    data class QuotesLoadedState(
        // Quote info
        val fromTokenInfo: TokenSwapInfo,
        val toTokenInfo: TokenSwapInfo,
        val swapProvider: SwapProvider,
        // Quote UI state
        val priceImpact: PriceImpact,
        val preparedSwapConfigState: PreparedSwapConfigState = PreparedSwapConfigState(
            balanceStatus = SwapBalanceStatus.Pending,
            hasOutgoingTransaction = false,
        ),
        val permissionState: PermissionDataState = PermissionDataState.Empty,
        // Quote tx
        val swapDataModel: SwapDataModel? = null,
        val integratedApprovalData: IntegratedApprovalData? = null,
        // Quote validation & checking
        val currencyCheck: CryptoCurrencyCheck? = null,
        val validationResult: Throwable? = null,
        val minAdaValue: BigDecimal?,
        val txType: ExpressTxType? = null,
    ) : SwapState

    data class Transfer(
        val userWallet: UserWallet,
        val fromTokenInfo: TokenSwapInfo,
        val toTokenInfo: TokenSwapInfo,
        val cryptoCurrencyWarning: CryptoCurrencyWarning?,
        val isInsufficientBalance: Boolean,
        val appCurrency: AppCurrency,
        val isBalanceHidden: Boolean,
        val isAccountsMode: Boolean,
        val isFeeCoverage: Boolean,
        val sendingAmount: BigDecimal,
        val currencyCheck: CryptoCurrencyCheck? = null,
        val validationResult: Throwable? = null,
        val minAdaValue: BigDecimal? = null,
    ) : SwapState

    data class EmptyAmountState(
        val zeroAmountEquivalent: TextReference,
        val isTransferMode: Boolean = false,
    ) : SwapState

    /**
     * Express data failure. Carries [balanceStatus] so the error-state notifications can decide
     * whether to surface a fee-coverage warning (only when status is [SwapBalanceStatus.FeeAdjustedAmount]).
     */
    data class SwapError(
        val fromTokenInfo: TokenSwapInfo,
        val error: ExpressDataError,
        val balanceStatus: SwapBalanceStatus,
    ) : SwapState
}

@Immutable
data class PriceImpact(
    val value: BigDecimal,
    val amountSignificance: AmountSignificance,
    val type: Type,
) {

    enum class Type {
        NONE, LOW, MEDIUM, HIGH
    }

    enum class AmountSignificance {
        LOW, MEDIUM, HIGH
    }

    fun shouldDisableButton(): Boolean {
        return type == Type.HIGH && amountSignificance == AmountSignificance.HIGH
    }

    fun shouldShowWarning(): Boolean {
        return type.ordinal > Type.LOW.ordinal || amountSignificance.ordinal > AmountSignificance.LOW.ordinal
    }

    companion object {
        val Empty = PriceImpact(
            value = BigDecimal.ZERO,
            amountSignificance = AmountSignificance.LOW,
            type = Type.NONE,
        )
    }
}

sealed class PermissionDataState {

    data class PermissionRequired(
        val isResetApproval: Boolean,
        val spenderAddress: String,
    ) : PermissionDataState()

    data class PermissionSettings(
        val type: ApproveType,
        val spenderAddress: String,
    ) : PermissionDataState()

    object PermissionLoading : PermissionDataState()

    object Empty : PermissionDataState()
}

data class TokenSwapInfo(
    val tokenAmount: SwapAmount,
    val amountFiat: BigDecimal,
    val swapCurrencyStatus: SwapCurrencyStatus,
)

/**
 * Combined approval + swap data attached to a [SwapState.QuotesLoadedState] when the user must approve a token
 * spend before swapping. Carries both the prepared approval transaction (built off the current
 * `permissionState.type` / spender) and the approval fee [TransactionFee] so the user-selected
 * fee bucket can be applied at submission time.
 *
 * The swap-tx data is not stored here — it is rebuilt fresh from `swapDataModel` at submission
 * time so any provider-side payload changes are picked up.
 *
 * @property approvalTransaction the unsigned ERC-20 approve transaction body, fee unset.
 * @property approvalFee the loaded fee envelope (Choosable or Single) for the approval tx; used
 *   to pick min/normal/priority based on the user's [FeeBucket] selection.
 * @property approveType the user-selected approval type (LIMITED vs UNLIMITED) the
 *   [approvalTransaction] was built for. Tracked so the model can detect a recalc-needed change.
 */
data class IntegratedApprovalData(
    val approvalTransaction: TransactionData.Uncompiled,
    val approvalFee: TransactionFee,
    val approveType: ApproveType,
)