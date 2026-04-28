package com.tangem.features.tangempay.limit.setup

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.findCardWithId
import com.tangem.domain.models.account.requireCardWithId
import com.tangem.domain.models.pay.TangemPayCardLimitPeriod
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.usecase.SetTangemPayCardLimitUseCase
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.navigation.TangemPayDetailsInnerRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Currency
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayCardLimitSetupModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    private val setTangemPayCardLimitUseCase: SetTangemPayCardLimitUseCase,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params: TangemPayDetailsContainerComponent.Params = paramsContainer.require()

    val uiState: StateFlow<TangemPayCardLimitSetupUM>
        field = MutableStateFlow(
            TangemPayCardLimitSetupUM(
                isInitialDataLoading = true,
                amountFieldModel = TangemPayCardLimitSetupUM.AmountFieldModel(
                    value = "",
                    decimals = 0,
                    onValueChange = {},
                ),
                subtitle = TextReference.EMPTY,
                currencyCode = "",
                presets = persistentListOf(),
                isSubmitButtonEnabled = false,
                isSubmitButtonLoading = false,
                onSubmitClick = ::onSubmitClick,
                onBackClick = router::pop,
            ),
        )

    init {
        observeCardState()
    }

    private fun observeCardState() {
        paymentAccountStatusSupplier.invoke(params.userWalletId)
            .map { it.value }
            .filterIsInstance<PaymentAccountStatusValue.Loaded>()
            .filter { status ->
                status.source == StatusSource.ACTUAL && status.findCardWithId(params.config.cardId) != null
            }
            .withIndex()
            .onEach { (index, status) ->
                val card = status.requireCardWithId(params.config.cardId)

                val currentLimit = card.limit?.actualCardLimit
                    ?.takeIf { it.period == TangemPayCardLimitPeriod.DAY }
                    ?.amount

                val adminLimit = card.limit?.adminCardLimit
                    ?.takeIf { it.period == TangemPayCardLimitPeriod.DAY }
                    ?.amount

                val currency = getJavaCurrencyByCode(status.currencyCode)
                uiState.update { state ->
                    val amount = if (index == 0) {
                        currentLimit?.stripTrailingZeros()?.toPlainString().orEmpty()
                    } else {
                        state.amountFieldModel.value
                    }
                    state.copy(
                        isInitialDataLoading = false,
                        amountFieldModel = TangemPayCardLimitSetupUM.AmountFieldModel(
                            value = amount,
                            decimals = currency.defaultFractionDigits,
                            onValueChange = ::onAmountChange,
                        ),
                        subtitle = buildSubtitle(adminLimit, currency),
                        currencyCode = currency.symbol,
                        presets = buildPresets(currency),
                        isSubmitButtonEnabled = isValid(amount),
                    )
                }
            }
            .launchIn(modelScope)
    }

    private fun onAmountChange(newValue: String) {
        uiState.update { state ->
            state.copy(
                amountFieldModel = state.amountFieldModel.copy(value = newValue),
                isSubmitButtonEnabled = isValid(newValue),
            )
        }
    }

    private fun onPresetClick(preset: BigDecimal) {
        val amount = uiState.value.amountFieldModel.value.toBigDecimalOrNull() ?: return
        val newAmount = if (preset == BigDecimal.ZERO) BigDecimal.ZERO else amount + preset
        onAmountChange(newAmount.stripTrailingZeros().toPlainString())
    }

    private fun onSubmitClick() {
        val amount = uiState.value.amountFieldModel.value.toBigDecimalOrNull() ?: return
        modelScope.launch {
            uiState.update { it.copy(isSubmitButtonLoading = true) }
            setTangemPayCardLimitUseCase(
                cardId = params.config.cardId,
                userWalletId = params.userWalletId,
                amount = amount,
            ).fold(
                ifLeft = {
                    uiState.update { state -> state.copy(isSubmitButtonLoading = false) }
                    uiMessageSender.send(
                        DialogMessage(
                            title = TextReference.Res(R.string.common_something_went_wrong),
                            message = TextReference.Res(R.string.tangempay_card_limit_setup_error_message),
                        ),
                    )
                },
                ifRight = {
                    uiState.update { state -> state.copy(isSubmitButtonLoading = false) }
                    router.push(TangemPayDetailsInnerRoute.LimitSetupSuccess)
                },
            )
        }
    }

    private fun isValid(value: String): Boolean {
        val amount = value.toBigDecimalOrNull() ?: return false
        return amount >= BigDecimal.ZERO
    }

    private fun buildSubtitle(maxLimit: BigDecimal?, currency: Currency): TextReference {
        return if (maxLimit == null) {
            TextReference.Res(
                id = R.string.tangempay_card_limit_setup_amount_subtitle,
                formatArgs = WrappedList(
                    listOf(
                        BigDecimal.ZERO.format { fiat(currency.currencyCode, currency.symbol).anyDecimals(0) },
                    ),
                ),
            )
        } else {
            TextReference.Res(
                id = R.string.tangempay_daily_limit_hint,
                formatArgs = WrappedList(
                    listOf(
                        BigDecimal.ZERO.format { fiat(currency.currencyCode, currency.symbol).anyDecimals(0) },
                        maxLimit.format { fiat(currency.currencyCode, currency.symbol).anyDecimals(0) },
                    ),
                ),
            )
        }
    }

    private fun buildPresets(currency: Currency) = listOf(
        BigDecimal.ZERO,
        BigDecimal("5000"),
        BigDecimal("10000"),
        BigDecimal("25000"),
    ).map { preset ->
        val label = preset.format { fiat(currency.currencyCode, currency.symbol).anyDecimals(0) }
        TangemPayCardLimitSetupUM.LimitPresetUM(
            label = if (preset == BigDecimal.ZERO) "0" else "+$label",
            onClick = { onPresetClick(preset) },
        )
    }.toPersistentList()
}