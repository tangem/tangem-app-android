package com.tangem.core.ui.ds.image

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
sealed interface DeviceIconUM {

    data class Card(
        val mainColor: Color,
        val secondColor: Color?,
        val thirdColor: Color? = null,
    ) : DeviceIconUM

    data class Ring(
        val mainColor: Color = Color.Unspecified,
        val cardColor: Color? = null,
        val secondCardColor: Color? = null,
    ) : DeviceIconUM

    data class Stub(val cardsCount: Int) : DeviceIconUM

    data object Mobile : DeviceIconUM
}