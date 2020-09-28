package com.tangem.tap.domain

import androidx.annotation.StringRes
import com.tangem.TangemError
import com.tangem.wallet.R

interface TapErrors

interface TapArgError : TapErrors {
    val args: List<Any>
}

interface MultiMessageError : TapErrors {
    val errorList: List<TapError>
    val builder: (List<String>) -> String
}

sealed class TapError(@StringRes val localizedMessage: Int) : Throwable(), TapErrors {
    object UnknownError : TapError(R.string.error_unknown)
    object PayIdAlreadyCreated : TapError(R.string.error_payid_already_created)
    object PayIdCreatingError : TapError(R.string.error_creating_payid)
    object PayIdEmptyField : TapError(R.string.wallet_create_payid_empty)
    object UnknownBlockchain : TapError(R.string.wallet_unknown_blockchain)
    object NoInternetConnection : TapError(R.string.notification_no_internet)
    object InsufficientBalance : TapError(R.string.error_insufficient_balance)
    object BlockchainInternalError : TapError(R.string.error_blockchain_internal)
    object AmountExceedsBalance : TapError(R.string.amount_exceeds_balance)
    object FeeExceedsBalance : TapError(R.string.fee_exceeds_balance)
    object TotalExceedsBalance : TapError(R.string.total_exceeds_balance)
    object InvalidAmountValue : TapError(R.string.invalid_amount_value)
    object InvalidFeeValue : TapError(R.string.invalid_fee_value)
    data class DustAmount(override val args: List<Any>) : TapError(R.string.dust_amount), TapArgError
    object DustChange : TapError(R.string.dust_change)
    data class CreateAccountUnderfunded(override val args: List<Any>) : TapError(R.string.create_account_underfunded), TapArgError

    data class ValidateTransactionErrors(
            override val errorList: List<TapError>,
            override val builder: (List<String>) -> String
    ) : TapError(-1), MultiMessageError
}

sealed class TapSdkError(override val messageResId: Int?) : Throwable(), TangemError {
    final override val code: Int = 1
    override var customMessage: String = code.toString()

    object CardForDifferentApp : TapSdkError(R.string.error_card_for_different_app)
}


fun TapErrors.assembleErrorIds(): MutableList<Int> {
    val idList = mutableListOf<Int>()
    when (this) {
        is MultiMessageError -> this.errorList.forEach { idList.addAll(it.assembleErrorIds()) }
        is TapError -> idList.add(this.localizedMessage)
    }
    return idList
}