package com.tangem.feature.swap.domain.transfer

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.extenstions.unwrap
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.domain.models.ui.TxFeeState
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.utils.extensions.orZero
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import javax.inject.Inject

class SwapTransferInteractorImpl @Inject constructor(
    private val swapFeatureToggles: SwapFeatureToggles,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
) : SwapTransferInteractor {

    override suspend fun updateTransfer(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
    ): SwapState {
        val fromToken = fromSwapCurrencyStatus.currency
        val toToken = toSwapCurrencyStatus.currency
        val appCurrency = getSelectedAppCurrencyUseCase.unwrap()
        val isBalanceHidden = getBalanceHidingSettingsUseCase.isBalanceHidden().first()
        val isAccountsMode = isAccountsModeEnabledUseCase.invokeSync()
        val fromTokenAmountValue = fromTokenAmount.parseBigDecimalOrNull() ?: return createEmptyAmountState(appCurrency)
        val fromTokenAmountFiat = fromSwapCurrencyStatus.status.value.fiatRate.orZero() * fromTokenAmountValue

        val fromTokenInfo = TokenSwapInfo(
            tokenAmount = SwapAmount(fromTokenAmountValue, fromToken.decimals),
            swapCurrencyStatus = fromSwapCurrencyStatus,
            amountFiat = fromTokenAmountFiat,
        )
        // it is the same with fromToken
        val toTokenInfo = TokenSwapInfo(
            tokenAmount = SwapAmount(fromTokenAmountValue, toToken.decimals),
            swapCurrencyStatus = toSwapCurrencyStatus,
            amountFiat = fromTokenAmountFiat,
        )
        return SwapState.Transfer(
            userWallet = toSwapCurrencyStatus.userWallet,
            fromTokenInfo = fromTokenInfo,
            toTokenInfo = toTokenInfo,
            txFee = TxFeeState.Empty, // todo Will be implemented in [REDACTED_TASK_KEY]
            appCurrency = appCurrency,
            isBalanceHidden = isBalanceHidden,
            isAccountsMode = isAccountsMode,
        )
    }

    private fun createEmptyAmountState(appCurrency: AppCurrency): SwapState.EmptyAmountState {
        return SwapState.EmptyAmountState(
            zeroAmountEquivalent = stringReference(
                BigDecimal.ZERO.format {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                    )
                },
            ),
            isTransferMode = true,
        )
    }

    override fun shouldTransferInsteadOfSwap(
        fromSwapCurrency: CryptoCurrency,
        toSwapCurrency: CryptoCurrency,
    ): Boolean {
        if (swapFeatureToggles.isSwapSwitchToTransferEnabled.not()) return false
        val isSameCurrency = when {
            fromSwapCurrency is CryptoCurrency.Coin && toSwapCurrency is CryptoCurrency.Coin -> {
                fromSwapCurrency.network.rawId == toSwapCurrency.network.rawId
            }
            fromSwapCurrency is CryptoCurrency.Token && toSwapCurrency is CryptoCurrency.Token -> {
                val isContractAddressSame = fromSwapCurrency.contractAddress == toSwapCurrency.contractAddress
                fromSwapCurrency.network.rawId == toSwapCurrency.network.rawId && isContractAddressSame
            }
            else -> false
        }
        return isSameCurrency
    }
}