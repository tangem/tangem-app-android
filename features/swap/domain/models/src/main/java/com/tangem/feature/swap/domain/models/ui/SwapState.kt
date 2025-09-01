package com.tangem.feature.swap.domain.models.ui

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import java.math.BigDecimal

sealed interface SwapState {

    /**
     * @param txFee fee state uses for calculation and build transaction
     * @param txFeeIncludeOtherNativeFee fee state uses for display and included otherNativeFee (specific for bridge)
     */
    data class QuotesLoadedState(
        val fromTokenInfo: TokenSwapInfo,
        val toTokenInfo: TokenSwapInfo,
        val priceImpact: PriceImpact,
        val preparedSwapConfigState: PreparedSwapConfigState = PreparedSwapConfigState(
            isAllowedToSpend = false,
            isBalanceEnough = false,
            feeState = SwapFeeState.NotEnough(),
            hasOutgoingTransaction = false,
            includeFeeInAmount = IncludeFeeInAmount.Excluded,
        ),
        val permissionState: PermissionDataState = PermissionDataState.Empty,
        val swapDataModel: SwapDataModel? = null,
        val txFee: TxFeeState,
        val currencyCheck: CryptoCurrencyCheck? = null,
        val validationResult: Throwable? = null,
        val minAdaValue: BigDecimal?,
        val swapProvider: SwapProvider,
    ) : SwapState

    data class EmptyAmountState(val zeroAmountEquivalent: String) : SwapState

    data class SwapError(
        val fromTokenInfo: TokenSwapInfo,
        val error: ExpressDataError,
        val includeFeeInAmount: IncludeFeeInAmount,
    ) : SwapState
}

sealed class PriceImpact {

    abstract val value: Float

    data class Empty(override val value: Float = 0f) : PriceImpact()

    data class Value(override val value: Float) : PriceImpact()

    fun getIntPercentValue() = (value * HUNDRED_PERCENTS).toInt()

    companion object {
        private const val HUNDRED_PERCENTS = 100
    }
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
    val fromTokenAmount: SwapAmount,
    val spenderAddress: String,
)

sealed class TxFeeState {
    data class MultipleFeeState(
        val normalFee: TxFee,
        val priorityFee: TxFee,
    ) : TxFeeState() {

        fun getFeeByType(feeType: FeeType): TxFee {
            return when (feeType) {
                FeeType.NORMAL -> normalFee
                FeeType.PRIORITY -> priorityFee
            }
        }
    }

    data class SingleFeeState(
        val fee: TxFee,
    ) : TxFeeState()

    data object Empty : TxFeeState()
}

data class TxFee(
    val feeValue: BigDecimal,
    val feeFiatFormatted: String,
    val feeCryptoFormatted: String,
    val feeIncludeOtherNativeFee: BigDecimal,
    val feeFiatFormattedWithNative: String,
    val feeCryptoFormattedWithNative: String,
    val cryptoSymbol: String,
    val feeType: FeeType,
    val fee: Fee,
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