package com.tangem.feature.wallet.presentation.preview

import androidx.compose.ui.text.SpanStyle
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledStringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletBalanceUM

internal object WalletBalancePreview {

    val content: WalletBalanceUM.Content = WalletBalanceUM.Content(
        id = UserWalletId("0"),
        name = "My Wallet",
        balanceInAppBar = combinedReference(
            stringReference("1,234"),
            styledStringReference(
                ".56",
                { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
            ),
            stringReference(" $"),
        ),
        balance = combinedReference(
            stringReference("1,234"),
            styledStringReference(
                ".56",
                {
                    TangemTheme.typography2.headingRegular28.toSpanStyle()
                },
            ),
            stringReference(" $"),
        ),
        deviceIcon = DeviceIconUM.Stub(cardsCount = 3),
        isBalanceFlickering = false,
        isZeroBalance = false,
    )

    val hiddenBalanceContent = content.copy(balance = content.balance.orMaskWithStars(true))

    val loading: WalletBalanceUM.Loading = WalletBalanceUM.Loading(
        id = UserWalletId("1"),
        name = "My Wallet",
        deviceIcon = DeviceIconUM.Mobile,
    )

    val error: WalletBalanceUM.Error = WalletBalanceUM.Error(
        id = UserWalletId("2"),
        name = "My Wallet",
        deviceIcon = DeviceIconUM.Stub(cardsCount = 3),
    )

    val empty: WalletBalanceUM.Empty = WalletBalanceUM.Empty(
        id = UserWalletId("2"),
        name = "My Wallet",
        deviceIcon = DeviceIconUM.Stub(cardsCount = 3),
    )
}