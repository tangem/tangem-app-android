package com.tangem.tap.features.wallet.ui

import androidx.annotation.IdRes
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.features.wallet.ui.utils.getFormattedAmount
import com.tangem.tap.features.wallet.ui.utils.getFormattedFiatAmount
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.CardBalanceBinding
import java.math.BigDecimal

enum class BalanceStatus {
    VerifiedOnline,
    TransactionInProgress,
    SameCurrencyTransactionInProgress,
    Unreachable,
    Loading,
    Refreshing,
    NoAccount,
    EmptyCard,
    UnknownBlockchain,
    MissedDerivation,
}

data class BalanceWidgetData(
    val status: BalanceStatus? = null,
    val currency: String? = null,
    val currencySymbol: String? = null,
    val amount: BigDecimal? = null,
    val amountFormatted: String? = null,
    val fiatAmount: BigDecimal? = null,
    val fiatAmountFormatted: String? = null,
    val blockchainAmount: BigDecimal? = BigDecimal.ZERO,
    val amountToCreateAccount: String? = null,
    val errorMessage: String? = null,
)

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
            // BalanceStatus.EmptyCard -> with(binding.lBalanceError) {
            //     binding.lBalance.root.hide()
            //     binding.lBalanceError.root.show()
            //     if (isTwinCard) {
            //         tvErrorTitle.text = fragment.getText(R.string.wallet_error_empty_twin_card)
            //         tvErrorDescriptions.text =
            //             fragment.getText(R.string.wallet_error_empty_twin_card_subtitle)
            //     } else {
            //         tvErrorTitle.text = fragment.getText(R.string.wallet_error_empty_card)
            //         tvErrorDescriptions.text =
            //             fragment.getText(R.string.wallet_error_empty_card_subtitle)
            //     }
            // }
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
            // BalanceStatus.UnknownBlockchain -> with(binding.lBalanceError) {
            //     binding.lBalance.root.hide()
            //     binding.lBalanceError.root.show()
            //     tvErrorTitle.text =
            //         fragment.getText(R.string.wallet_error_unsupported_blockchain)
            //     tvErrorDescriptions.text =
            //         fragment.getString(R.string.wallet_error_unsupported_blockchain_subtitle)
            // }
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
        tvAmount.text = if (showAmount) tokenWalletData?.getFormattedAmount() else ""
        tvBaseAmount.text = if (showAmount) data.getFormattedAmount() else ""
        if (showAmount) {
            tvFiatAmount.show()
            tvFiatAmount.text = tokenWalletData?.getFormattedFiatAmount(store.state.globalState.appCurrency)
        }
    }

    private fun showBalanceWithoutToken(data: WalletDataModel, showAmount: Boolean) = with(binding.lBalance) {
        groupBaseCurrency.hide()
        tvCurrency.text = data.currency.currencyName
        tvAmount.text = if (showAmount) data.getFormattedAmount() else ""
        if (showAmount) {
            tvFiatAmount.show()
            tvFiatAmount.text = data.getFormattedFiatAmount(store.state.globalState.appCurrency)
        }
    }
}
