package com.tangem.tap.domain

import androidx.annotation.StringRes
import com.tangem.wallet.R

sealed class TapError(@StringRes val localizedMessage: Int): Throwable() {
    object PayIdAlreadyCreated: TapError(R.string.error_payid_already_created)
    object PayIdCreatingError: TapError(R.string.error_creating_payid)
    object PayIdEmptyField: TapError(R.string.wallet_create_payid_empty)
    object UnknownBlockchain: TapError(R.string.wallet_unknown_blockchain)
    object NoInternetConnection: TapError(R.string.notification_no_internet)
    object InsufficientBalance: TapError(R.string.error_insufficient_balance)
    object BlockchainInternalError: TapError(R.string.error_blockchain_internal)
    object UnknownError: TapError(R.string.error_unknown)
}