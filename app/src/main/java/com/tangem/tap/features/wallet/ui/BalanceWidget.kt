package com.tangem.tap.features.wallet.ui

import androidx.annotation.IdRes
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
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
    val blockchainAmount: BigDecimal? = BigDecimal.ZERO,
    val amount: BigDecimal? = null,
    val amountFormatted: String? = null,
    val fiatAmount: BigDecimal? = null,
    val fiatAmountFormatted: String? = null,
    val token: TokenData? = null,
    val amountToCreateAccount: String? = null,
    val errorMessage: String? = null,
)

data class TokenData(
    val amountFormatted: String?,
    val amount: BigDecimal? = null,
    val tokenSymbol: String,
    val fiatAmountFormatted: String? = null,
    val fiatAmount: BigDecimal? = null,
    val fiatRateString: String? = null,
    val fiatRate: BigDecimal? = null,
)

class BalanceWidget(
    private val binding: CardBalanceBinding,
    private val fragment: WalletFragment,
    private val data: BalanceWidgetData,
    private val isTwinCard: Boolean,
) {

    @Suppress("LongMethod", "ComplexMethod")
    fun setup() {
        when (data.status) {
            BalanceStatus.Loading -> {
                with(binding) {
                    lBalance.root.show()
                    lBalanceError.root.hide()
                    lBalance.tvFiatAmount.hide()

                    lBalance.tvCurrency.text = data.currency
                    lBalance.tvAmount.text = ""
                }

                showStatus(R.id.tv_status_loading)

                if (data.token != null) {
                    showBalanceWithToken(data, false)
                } else {
                    showBalanceWithoutToken(data, false)
                }
            }
            BalanceStatus.VerifiedOnline, BalanceStatus.TransactionInProgress -> with(binding.lBalance) {
                root.show()
                binding.lBalanceError.root.hide()
                val statusView = if (data.status == BalanceStatus.VerifiedOnline) {
                    R.id.tv_status_verified
                } else {
                    tvStatusError.text =
                        fragment.getText(R.string.wallet_balance_tx_in_progress)
                    R.id.group_error
                }
                showStatus(statusView)
                tvStatusErrorMessage.hide()

                if (data.token != null) {
                    showBalanceWithToken(data, true)
                } else {
                    showBalanceWithoutToken(data, true)
                }
            }
            BalanceStatus.Unreachable -> with(binding.lBalance) {
                root.show()
                binding.lBalanceError.root.hide()
                tvFiatAmount.hide()
                groupBaseCurrency.hide()

                val currency = if (data.token != null) data.token.tokenSymbol else data.currency
                tvCurrency.text = currency
                tvAmount.text = ""

                tvStatusErrorMessage.text = data.errorMessage
                tvStatusError.text =
                    fragment.getString(R.string.wallet_balance_blockchain_unreachable)

                showStatus(R.id.group_error)
                tvStatusErrorMessage.show(!data.errorMessage.isNullOrBlank())
            }
            BalanceStatus.EmptyCard -> with(binding.lBalanceError) {
                binding.lBalance.root.hide()
                binding.lBalanceError.root.show()
                if (isTwinCard) {
                    tvErrorTitle.text = fragment.getText(R.string.wallet_error_empty_twin_card)
                    tvErrorDescriptions.text =
                        fragment.getText(R.string.wallet_error_empty_twin_card_subtitle)
                } else {
                    tvErrorTitle.text = fragment.getText(R.string.wallet_error_empty_card)
                    tvErrorDescriptions.text =
                        fragment.getText(R.string.wallet_error_empty_card_subtitle)
                }
            }
            BalanceStatus.NoAccount -> with(binding.lBalanceError) {
                binding.lBalance.root.hide()
                binding.lBalanceError.root.show()
                tvErrorTitle.text = fragment.getText(R.string.wallet_error_no_account)
                tvErrorDescriptions.text =
                    fragment.getString(
                        R.string.no_account_generic,
                        data.amountToCreateAccount,
                        data.currencySymbol,
                    )
            }
            BalanceStatus.UnknownBlockchain -> with(binding.lBalanceError) {
                binding.lBalance.root.hide()
                binding.lBalanceError.root.show()
                tvErrorTitle.text =
                    fragment.getText(R.string.wallet_error_unsupported_blockchain)
                tvErrorDescriptions.text =
                    fragment.getString(R.string.wallet_error_unsupported_blockchain_subtitle)
            }
            else -> {}
        }
    }

    private fun showStatus(@IdRes viewRes: Int) = with(binding.lBalance) {
        groupError.show(viewRes == R.id.group_error)
        tvStatusLoading.show(viewRes == R.id.tv_status_loading)
        tvStatusVerified.show(viewRes == R.id.tv_status_verified)
    }

    private fun showBalanceWithToken(data: BalanceWidgetData, showAmount: Boolean) = with(binding.lBalance) {
        groupBaseCurrency.show()
        tvCurrency.text = data.token?.tokenSymbol
        tvBaseCurrency.text = data.currency
        tvAmount.text = if (showAmount) data.token?.amountFormatted else ""
        tvBaseAmount.text = if (showAmount) data.amountFormatted else ""
        if (showAmount) {
            tvFiatAmount.show()
            tvFiatAmount.text = data.token?.fiatAmountFormatted
        }
    }

    private fun showBalanceWithoutToken(data: BalanceWidgetData, showAmount: Boolean) =
        with(binding.lBalance) {
            groupBaseCurrency.hide()
            tvCurrency.text = data.currency
            tvAmount.text = if (showAmount) data.amountFormatted else ""
            if (showAmount) {
                tvFiatAmount.show()
                tvFiatAmount.text = data.fiatAmountFormatted
            }
        }
}
