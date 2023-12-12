package com.tangem.feature.swap.domain.models.ui

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.IncludeFeeInAmount
import com.tangem.feature.swap.domain.models.domain.PreparedSwapConfigState
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import java.math.BigDecimal

sealed interface SwapState {

    data class QuotesLoadedState(
        val fromTokenInfo: TokenSwapInfo,
        val toTokenInfo: TokenSwapInfo,
        val priceImpact: Float,
        val networkCurrency: String,
        val preparedSwapConfigState: PreparedSwapConfigState = PreparedSwapConfigState(
            isAllowedToSpend = false,
            isBalanceEnough = false,
            isFeeEnough = false,
            hasOutgoingTransaction = false,
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
        ),
        val permissionState: PermissionDataState = PermissionDataState.Empty,
        val swapDataModel: SwapDataModel? = null,
        val txFee: TxFeeState,
        val tangemFee: Double,
    ) : SwapState

    data class EmptyAmountState(
        val fromTokenWalletBalance: String,
        val toTokenWalletBalance: String,
        val zeroAmountEquivalent: String,
    ) : SwapState

    data class SwapError(
        val fromTokenInfo: TokenSwapInfo,
        val error: DataError,
    ) : SwapState
}

sealed class PermissionDataState {

    data class PermissionReadyForRequest(
        val currency: String,
        val amount: String,
        val walletAddress: String,
        val spenderAddress: String,
        val requestApproveData: RequestApproveStateData,
    ) : PermissionDataState()

    object PermissionFailed : PermissionDataState()

    object PermissionLoading : PermissionDataState()

    object Empty : PermissionDataState()
}

data class TokenSwapInfo(
    val tokenAmount: SwapAmount,
    val amountFiat: BigDecimal,
    val cryptoCurrencyStatus: CryptoCurrencyStatus,
)

data class RequestApproveStateData(
    val fee: TxFeeState,
    val approveData: String,
    val fromTokenAmount: SwapAmount,
    val spenderAddress: String,
)

// data class SwapStateData(
//     val fee: TxFeeState,
//     val swapModel: SwapDataModel,
// )

sealed class TxFeeState {
    data class MultipleFeeState(
        val normalFee: TxFee,
        val priorityFee: TxFee,
    ) : TxFeeState()

    data class SingleFeeState(
        val fee: TxFee,
    ) : TxFeeState()

    object Empty : TxFeeState()
}

data class TxFee(
    val feeValue: BigDecimal,
    val gasLimit: Int,
    val feeFiatFormatted: String,
    val feeCryptoFormatted: String,
    val decimals: Int,
    val cryptoSymbol: String,
    val feeType: FeeType,
)

enum class FeeType {
    NORMAL, PRIORITY
}

fun FeeType.getNameForAnalytics(): String {
    return when (this) {
        FeeType.NORMAL -> "Normal"
        FeeType.PRIORITY -> "Max"
    }
}