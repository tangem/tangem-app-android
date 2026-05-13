package com.tangem.feature.swap.domain.models.ui

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import java.math.BigDecimal

sealed interface SwapState {

    data class QuotesLoadedState(
        val fromTokenInfo: TokenSwapInfo,
        val toTokenInfo: TokenSwapInfo,
        val priceImpact: PriceImpact,
        val preparedSwapConfigState: PreparedSwapConfigState = PreparedSwapConfigState(
            isBalanceEnough = false,
            feeState = SwapFeeState.NotEnough(),
            hasOutgoingTransaction = false,
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
        ),
        val permissionState: PermissionDataState = PermissionDataState.Empty,
        val swapDataModel: SwapDataModel? = null,
        val currencyCheck: CryptoCurrencyCheck? = null,
        val validationResult: Throwable? = null,
        val minAdaValue: BigDecimal?,
        val swapProvider: SwapProvider,
    ) : SwapState

    data class Transfer(
        val userWallet: UserWallet,
        val fromTokenInfo: TokenSwapInfo,
        val toTokenInfo: TokenSwapInfo,
        val appCurrency: AppCurrency,
        val isBalanceHidden: Boolean,
        val isAccountsMode: Boolean,
    ) : SwapState

    data class EmptyAmountState(
        val zeroAmountEquivalent: TextReference,
        val isTransferMode: Boolean = false,
    ) : SwapState

    data class SwapError(
        val fromTokenInfo: TokenSwapInfo,
        val error: ExpressDataError,
        val includeFeeInAmount: IncludeFeeInAmount,
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

    object PermissionLoading : PermissionDataState()

    object Empty : PermissionDataState()
}

data class TokenSwapInfo(
    val tokenAmount: SwapAmount,
    val amountFiat: BigDecimal,
    val swapCurrencyStatus: SwapCurrencyStatus,
)