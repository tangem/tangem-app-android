package com.tangem.features.tangempay.cashback.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.joda.time.DateTime
import java.math.BigDecimal
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayCashbackModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    private val cashbackConverter = TangemPayCashbackUmConverter(onCloseClick = router::pop)

    val uiState: StateFlow<TangemPayCashbackUM>
        field = MutableStateFlow(cashbackConverter.convert(STUB_CASHBACK))

    private companion object {
        // TODO([REDACTED_TASK_KEY]): replace stub with repository load
        val STUB_CASHBACK = TangemPayCashback(
            confirmedAmount = BigDecimal("22.54"),
            currency = "USD",
            period = TangemPayCashback.Period(
                year = 2026,
                month = 6,
                payoutStart = DateTime.parse("2026-07-01"),
                payoutEnd = DateTime.parse("2026-07-05"),
            ),
        )
    }
}