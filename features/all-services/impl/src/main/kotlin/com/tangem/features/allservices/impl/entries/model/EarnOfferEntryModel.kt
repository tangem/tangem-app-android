package com.tangem.features.allservices.impl.entries.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.earn.usecase.GetBestEarnOfferUseCase
import com.tangem.domain.models.earn.EarnType
import com.tangem.utils.StringsSigns.PERCENT
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Best APY Earn offers for one [EarnType], formatted for an entry's subtitle; `null` until loaded and when
 * there is no offer or the request fails — the entry then shows no rate rather than a wrong one.
 */
@ModelScoped
internal class EarnOfferEntryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val getBestEarnOfferUseCase: GetBestEarnOfferUseCase,
) : Model() {

    private val type = paramsContainer.require<EarnType>()

    val bestApy: StateFlow<String?>
        field = MutableStateFlow(null)

    init {
        modelScope.launch {
            getBestEarnOfferUseCase(type)
                .onLeft { TangemLogger.e("All Services: failed to load the best $type offer", it) }
                .onRight { offer ->
                    // The Earn list reports a fraction (`0.0384` is 3.84%)
                    bestApy.value = offer?.apy?.toBigDecimalOrNull()
                        ?.format { percent(withPercentSign = false) }
                        ?.plus(PERCENT)
                }
        }
    }
}