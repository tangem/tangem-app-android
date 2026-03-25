package com.tangem.common.ui.userwallet.converter

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.models.wallet.UserWalletIcon
import com.tangem.utils.converter.Converter
import javax.inject.Inject

/**
 *  Converter for mapping [UserWalletIcon] to [DeviceIconUM],
 *  which is used for displaying the wallet icon in the UI.
 */
class WalletIconUMConverter @Inject constructor() : Converter<UserWalletIcon, DeviceIconUM> {

    override fun convert(value: UserWalletIcon): DeviceIconUM = with(value) {
        fun String.parseHexColor(): Color = try {
            Color(this.toColorInt())
        } catch (_: IllegalArgumentException) {
            Color.Unspecified
        }

        return when (this) {
            UserWalletIcon.Hot -> DeviceIconUM.Mobile
            is UserWalletIcon.Stub ->
                DeviceIconUM.Stub(cardsCount = this.cardsCount)
            is UserWalletIcon.Default -> if (isRing) {
                DeviceIconUM.Ring(
                    mainColor = Color.Unspecified,
                    cardColor = Color.Unspecified,
                    secondCardColor = if (cardsCount > 2) Color.Unspecified else null,
                )
            } else {
                DeviceIconUM.Card(
                    mainColor = Color.Unspecified,
                    secondColor = if (cardsCount > 1) Color.Unspecified else null,
                    thirdColor = if (cardsCount > 2) Color.Unspecified else null,
                )
            }
            is UserWalletIcon.Colored -> if (isRing) {
                DeviceIconUM.Ring(
                    mainColor = mainColor.parseHexColor(),
                    cardColor = secondColor?.parseHexColor(),
                    secondCardColor = thirdColor?.parseHexColor(),
                )
            } else {
                DeviceIconUM.Card(
                    mainColor = mainColor.parseHexColor(),
                    secondColor = secondColor?.parseHexColor(),
                    thirdColor = thirdColor?.parseHexColor(),
                )
            }
        }
    }
}