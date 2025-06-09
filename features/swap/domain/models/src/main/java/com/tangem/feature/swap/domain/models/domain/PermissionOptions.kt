package com.tangem.feature.swap.domain.models.domain

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.domain.models.ui.RequestApproveStateData
import com.tangem.feature.swap.domain.models.ui.TxFee

/**
 * Permission options
 *
 * @param approveData tx data to give approve, it loaded from 1inch in findBestQuote if needed
 * @param forTokenContractAddress token contract address for which needs permission
 * @param fromToken which token will be swapping
 * @param approveType unlimited or tx amount approve
 * @param txFee fee for tx
 */
data class PermissionOptions(
    val approveData: RequestApproveStateData,
    val forTokenContractAddress: String,
    val fromToken: CryptoCurrency,
    val spenderAddress: String,
    val approveType: SwapApproveType,
    val txFee: TxFee,
)