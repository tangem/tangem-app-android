package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.converter.SwapAmountFieldConverter
import com.tangem.utils.transformer.Transformer

internal class SwapAmountBalanceHiddenTransformer(
    private val isBalanceHidden: Boolean,
    private val isSingleWallet: Boolean,
    private val userWallet: UserWallet,
    private val appCurrency: AppCurrency,
    private val swapDirection: SwapDirection,
    private val clickIntents: AmountScreenClickIntents,
) : Transformer<SwapAmountUM> {

    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        val content = prevState as? SwapAmountUM.Content ?: return prevState

        val amountFieldConverter = SwapAmountFieldConverter(
            swapDirection = swapDirection,
            isBalanceHidden = isBalanceHidden,
            userWallet = userWallet,
            appCurrency = appCurrency,
            clickIntents = clickIntents,
            isSingleWallet = isSingleWallet,
        )

        val recalculatedPrimary = amountFieldConverter.convert(
            selectedType = SwapAmountType.From,
            cryptoCurrencyStatus = content.primaryCryptoCurrencyStatus,
        ) as SwapAmountFieldUM.Content

        val oldPrimary = content.primaryAmount as? SwapAmountFieldUM.Content

        val mergedAmountField = if (
            oldPrimary?.amountField is AmountState.Data && recalculatedPrimary.amountField is AmountState.Data
        ) {
            val oldData = oldPrimary.amountField
            val newData = recalculatedPrimary.amountField
            newData.copy(
                amountTextField = oldData.amountTextField,
                selectedButton = oldData.selectedButton,
                isPrimaryButtonEnabled = oldData.isPrimaryButtonEnabled,
                isSegmentedButtonsEnabled = oldData.isSegmentedButtonsEnabled,
                isEditingDisabled = oldData.isEditingDisabled,
                reduceAmountBy = oldData.reduceAmountBy,
                isIgnoreReduce = oldData.isIgnoreReduce,
            )
        } else {
            recalculatedPrimary.amountField
        }

        val updatedPrimaryAmount = recalculatedPrimary.copy(
            amountField = mergedAmountField,
        )

        return content.copy(primaryAmount = updatedPrimaryAmount)
    }
}