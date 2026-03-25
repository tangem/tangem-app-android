package com.tangem.features.approval.api

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId

interface GiveApprovalComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        val amount: String,
        val spenderAddress: String,
        val subtitle: TextReference,
        val callback: Callback,
    )

    interface Callback {
        fun onApproveClick()
        fun onApproveDone()
        fun onApproveFailed()
        fun onCancelClick()
    }

    interface Factory {
        fun create(context: AppComponentContext, params: Params): GiveApprovalComponent
    }
}