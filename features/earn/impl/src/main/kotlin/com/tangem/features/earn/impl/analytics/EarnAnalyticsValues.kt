package com.tangem.features.earn.impl.analytics

import com.tangem.features.earn.impl.getfunds.EarnGetTokenAction
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerTab
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorComponent
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnRowKind

internal enum class EarnAddFundsSource(val value: String) {
    YieldPicker("Yield Picker"),
    InfoScreen("Info Screen"),
}

internal fun EarnPickerTab.toAnalyticsValue(): String = when (this) {
    EarnPickerTab.Calculator -> "Calculator"
    EarnPickerTab.Benefits -> "Benefits"
    EarnPickerTab.Details -> "Details"
}

internal fun EarnHorizon.toAnalyticsValue(): String = when (this) {
    EarnHorizon.Months6 -> "6M"
    EarnHorizon.Year1 -> "1Y"
    EarnHorizon.Year3 -> "3Y"
    EarnHorizon.Year5 -> "5Y"
    EarnHorizon.Year10 -> "10Y"
}

internal fun EarnRowKind.toAnalyticsValue(): String = when (this) {
    EarnRowKind.Risk -> "Risk"
    EarnRowKind.Protocol -> "Protocol"
    EarnRowKind.Withdrawals -> "Withdrawals"
    EarnRowKind.PerformanceFee -> "Performance Fee"
    EarnRowKind.Audits -> "Audits"
}

internal fun EarnCalculatorComponent.Input.toAnalyticsValue(): String = when (this) {
    EarnCalculatorComponent.Input.InitialBalance -> "Initial Balance"
    EarnCalculatorComponent.Input.MonthlyTopUp -> "Monthly Top-Up"
}

internal fun EarnGetTokenAction.toAnalyticsValue(): String = when (this) {
    EarnGetTokenAction.Deposit -> "Deposit"
    EarnGetTokenAction.Swap -> "Swap"
    EarnGetTokenAction.Buy -> "Buy"
}