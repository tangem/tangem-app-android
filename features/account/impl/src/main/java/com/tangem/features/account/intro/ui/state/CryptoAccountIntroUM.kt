package com.tangem.features.account.intro.ui.state

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class CryptoAccountIntroUM(
    val title: TextReference,
    val subtitle: TextReference,
    val points: ImmutableList<PointUM>,
    val isContinueLoading: Boolean = false,
    val onContinueClick: () -> Unit,
    val onCloseClick: () -> Unit,
) {

    data class PointUM(
        val id: String,
        val icon: TangemIconUM,
        val lead: TextReference,
        val description: TextReference,
    )
}