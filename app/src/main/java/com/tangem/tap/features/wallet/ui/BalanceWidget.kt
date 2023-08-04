package com.tangem.tap.features.wallet.ui

import androidx.annotation.IdRes
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.features.wallet.ui.utils.getFormattedCryptoAmount
import com.tangem.tap.features.wallet.ui.utils.getFormattedFiatAmount
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.CardBalanceBinding

class BalanceWidget(
    private val binding: CardBalanceBinding,
    private val fragment: WalletFragment,
    private val blockchainWalletData: WalletDataModel,
    private val tokenWalletData: WalletDataModel?,
) {

    @Suppress("LongMethod", "ComplexMethod")
    fun setup() {
        when (blockchainWalletData.status) {
            is WalletDataModel.Loading -> {
                with(binding) {
                    lBalance.root.show()
                    lBalanceError.root.hide()
                    lBalance.tvFiatAmount.hide()

                    lBalance.tvCurrency.text = blockchainWalletData.currency.currencyName
                    lBalance.tvAmount.text = ""
                }

                showStatus(R.id.tv_status_loading)

                if (tokenWalletData != null) {
                    showBalanceWithToken(blockchainWalletData, false)
                } else {
                    showBalanceWithoutToken(blockchainWalletData, false)
                }
            }
            is WalletDataModel.VerifiedOnline,
            is WalletDataModel.TransactionInProgress,
            -> with(binding.lBalance) {
                root.show()
                binding.lBalanceError.root.hide()
                val statusView = if (blockchainWalletData.status is WalletDataModel.VerifiedOnline) {
                    R.id.tv_status_verified
                } else {
                    tvStatusError.text =
                        fragment.getText(R.string.wallet_balance_tx_in_progress)
                    R.id.group_error
                }
                showStatus(statusView)
                tvStatusErrorMessage.hide()

                if (tokenWalletData != null) {
                    showBalanceWithToken(blockchainWalletData, true)
                } else {
                    showBalanceWithoutToken(blockchainWalletData, true)
                }
            }
            is WalletDataModel.Unreachable -> with(binding.lBalance) {
                root.show()
                binding.lBalanceError.root.hide()
                tvFiatAmount.hide()
                groupBaseCurrency.hide()

                val currency = tokenWalletData?.currency?.currencySymbol
                    ?: blockchainWalletData.currency.currencyName
                tvCurrency.text = currency
                tvAmount.text = ""

                tvStatusErrorMessage.text = blockchainWalletData.status.errorMessage
                tvStatusError.text =
                    fragment.getString(R.string.wallet_balance_blockchain_unreachable)

                showStatus(R.id.group_error)
                tvStatusErrorMessage.show(!blockchainWalletData.status.errorMessage.isNullOrBlank())
            }
            is WalletDataModel.NoAccount -> with(binding.lBalanceError) {
                binding.lBalance.root.hide()
                binding.lBalanceError.root.show()
                tvErrorTitle.text = fragment.getText(R.string.wallet_error_no_account)
                tvErrorDescriptions.text =
                    fragment.getString(
                        R.string.no_account_generic,
                        blockchainWalletData.status.amountToCreateAccount,
                        blockchainWalletData.currency.currencySymbol,
                    )
            }
            else -> {}
        }
    }

    private fun showStatus(@IdRes viewRes: Int) = with(binding.lBalance) {
        groupError.show(viewRes == R.id.group_error)
        tvStatusLoading.show(viewRes == R.id.tv_status_loading)
        tvStatusVerified.show(viewRes == R.id.tv_status_verified)
    }

    private fun showBalanceWithToken(data: WalletDataModel, showAmount: Boolean) = with(binding.lBalance) {
        groupBaseCurrency.show()
        tvCurrency.text = tokenWalletData?.currency?.currencySymbol
        tvBaseCurrency.text = data.currency.currencyName
        tvAmount.text = if (showAmount) tokenWalletData?.getFormattedCryptoAmount() else ""
        tvBaseAmount.text = if (showAmount) data.getFormattedCryptoAmount() else ""
        if (showAmount) {
            tvFiatAmount.show()
            tvFiatAmount.text = tokenWalletData?.getFormattedFiatAmount(store.state.globalState.appCurrency)
        }
    }

    private fun showBalanceWithoutToken(data: WalletDataModel, showAmount: Boolean) = with(binding.lBalance) {
        groupBaseCurrency.hide()
        tvCurrency.text = data.currency.currencyName
        tvAmount.text = if (showAmount) data.getFormattedCryptoAmount() else ""
        if (showAmount) {
            tvFiatAmount.show()
            tvFiatAmount.text = data.getFormattedFiatAmount(store.state.globalState.appCurrency)
        }
    }
}