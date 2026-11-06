package com.tangem.features.earn.impl.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.impl.picker.model.EarnPickerModel
import com.tangem.features.earn.impl.picker.ui.EarnPickerContent
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * The Earn product picker of one currency: chips, description and the Calculator / Benefits / Details tabs of the
 * selected product.
 */
internal class EarnYieldPickerComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
    calculatorFactory: EarnCalculatorComponent.Factory,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: EarnPickerModel = getOrCreateModel(params)

    private val calculator = calculatorFactory.create(
        context = child(key = EarnCalculatorComponent.KEY),
        params = EarnCalculatorComponent.Params(
            projection = model.projection,
            incentiveRate = model.incentiveRate,
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        EarnPickerContent(
            state = state,
            modifier = modifier,
            calculator = { calculator.Content(modifier = Modifier) },
        )
    }

    /**
     * @property preselected the product the user came from, `null` selects the first one
     * @property shouldRedirectToActive `Auto`: a currency with an active product opens its Info instead of the picker
     */
    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val preselected: EarnProductId?,
        val shouldRedirectToActive: Boolean,
    )

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): EarnYieldPickerComponent
    }
}