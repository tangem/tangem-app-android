package com.tangem.feature.swap.domain.models.ui

import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ApproveModel
import com.tangem.feature.swap.domain.models.domain.PreparedSwapConfigState
import com.tangem.feature.swap.domain.models.domain.SwapDataModel

sealed interface SwapState {

    data class QuotesLoadedState(
        val fromTokenInfo: TokenSwapInfo,
        val toTokenInfo: TokenSwapInfo,
        val fee: String,
        val networkCurrency: String,
        val preparedSwapConfigState: PreparedSwapConfigState,
        val permissionState: PermissionDataState = PermissionDataState.Empty,
        val swapDataModel: SwapDataModel? = null,
    ) : SwapState

    data class EmptyAmountState(
        val fromTokenWalletBalance: String,
        val toTokenWalletBalance: String,
    ) : SwapState

    data class SwapError(val error: DataError) : SwapState
}

sealed class PermissionDataState {

    data class PermissionReadyForRequest(
        val currency: String,
        val amount: String,
        val walletAddress: String,
        val spenderAddress: String,
        val fee: String,
        val requestApproveData: RequestApproveStateData,
    ) : PermissionDataState()

    object PermissionFailed : PermissionDataState()

    object PermissionLoading : PermissionDataState()

    object Empty : PermissionDataState()
}

data class TokenSwapInfo(
    val tokenAmount: SwapAmount,
    val coinId: String,
    val tokenWalletBalance: String,
    val tokenFiatBalance: String,
)

data class RequestApproveStateData(
    val estimatedGas: Int,
    val approveModel: ApproveModel,
)
