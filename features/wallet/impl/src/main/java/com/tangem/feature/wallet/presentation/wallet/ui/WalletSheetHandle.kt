package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.compose.runtime.Stable
import com.tangem.core.ui.ds2.shtorka.TangemShtorka
import com.tangem.core.ui.ds2.shtorka.TangemShtorkaState

/**
 * Everything the wallet content needs to know about the [TangemShtorka] that hosts the feed.
 *
 * Both members read snapshot state, so they can be observed from `derivedStateOf`.
 */
@Stable
internal interface WalletSheetHandle {

    /** Whether the sheet is expanded or animating towards expanded. */
    val isExpanded: Boolean

    /** Top edge of the sheet in root coordinates, px; `0` before the first layout pass. */
    fun topOffset(): Float
}

/** [WalletSheetHandle] over [TangemShtorkaState]; expanded means the shtorka is at full height. */
internal class ShtorkaWalletSheetHandle(private val state: TangemShtorkaState) : WalletSheetHandle {

    override val isExpanded: Boolean
        get() = state.targetDetent is TangemShtorka.Detent.Full

    override fun topOffset(): Float = runCatching { state.requireOffset() }.getOrElse { 0f }
}