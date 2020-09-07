package com.tangem.tap.common.redux.global

import com.tangem.tap.domain.tasks.ScanNoteResponse
import org.rekotlin.Action
import java.math.BigDecimal

sealed class GlobalAction : Action {

    data class SaveScanNoteResponse(val scanNoteResponse: ScanNoteResponse) : GlobalAction()
    data class SetFiatRate(val fiatRates: Pair<String, BigDecimal>) : GlobalAction()
}