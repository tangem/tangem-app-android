package com.tangem.tap.features.wallet.ui

import android.content.Context
import com.tangem.common.module.ModuleMessageConverter
import com.tangem.tap.features.wallet.models.WalletWarning
import com.tangem.tap.features.wallet.models.WalletWarningDescription
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 20/05/2022.
 */
class WalletWarningConverter(
    private val context: Context,
) : ModuleMessageConverter<WalletWarning, WalletWarningDescription> {

    override fun convert(message: WalletWarning): WalletWarningDescription {
        val warningMessage = when (message) {
            is WalletWarning.ExistentialDeposit -> {
                context.getString(
                    R.string.warning_existential_deposit_message,
                    message.currencyName,
                    message.edStringValueWithSymbol,
                )
            }
            is WalletWarning.BalanceNotEnoughForFee -> {
                context.getString(
                    R.string.token_details_send_blocked_fee_format,
                    message.blockchainFullName,
                    message.blockchainFullName,
                )
            }
            is WalletWarning.TransactionInProgress -> {
                context.getString(
                    R.string.token_details_send_blocked_tx_format,
                    message.currencyName
                )
            }
            is WalletWarning.Rent -> {
                context.getString(
                    R.string.solana_rent_warning,
                    message.walletRent.minRentValue,
                    message.walletRent.rentExemptValue,
                )
            }
        }
        return WalletWarningDescription(context.getString(R.string.common_warning), warningMessage)
    }
}
