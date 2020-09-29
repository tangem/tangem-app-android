package com.tangem.tap.domain

import androidx.annotation.StringRes
import com.tangem.TangemError
import com.tangem.wallet.R

interface TapErrors

interface ArgError{
    val args: List<Any>?
}

interface MultiMessageError : TapErrors {
    val errorList: List<TapError>
    val builder: (List<String>) -> String
}

sealed class TapError(
        @StringRes val localizedMessage: Int,
        override val args: List<Any>? = null
) : Throwable(), TapErrors, ArgError {

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
    data class DustAmount(override val args: List<Any>) : TapError(R.string.dust_amount)
    object DustChange : TapError(R.string.dust_change)
    data class CreateAccountUnderfunded(override val args: List<Any>) : TapError(R.string.create_account_underfunded)

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


fun TapErrors.assembleErrors(): MutableList<Pair<Int, List<Any>?>> {
    val idList = mutableListOf<Pair<Int, List<Any>?>>()
    when (this) {
        is MultiMessageError -> this.errorList.forEach { idList.addAll(it.assembleErrors()) }
        is TapError -> idList.add(Pair(this.localizedMessage, this.args))
    }
    return idList
}