package com.tangem.tap.common.redux.global

import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.Action

sealed class GlobalAction : Action {

    data class SaveScanResponse(val scanResponse: ScanResponse) : GlobalAction()

    data class IsSignWithRing(val isSignWithRing: Boolean) : GlobalAction()
}