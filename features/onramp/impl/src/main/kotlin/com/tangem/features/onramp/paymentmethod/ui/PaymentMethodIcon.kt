package com.tangem.features.onramp.paymentmethod.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.SelectProviderBottomSheetTestTags
import com.tangem.domain.onramp.model.OnrampPaymentMethod

@Composable
internal fun PaymentMethodIcon(method: OnrampPaymentMethod, modifier: Modifier = Modifier) {
    val tileColor = if (method.hasThemedImages) TangemTheme.colors.field.focused else TangemColorPalette.Light1

    SubcomposeAsyncImage(
        modifier = modifier
            .size(TangemTheme.dimens.size40)
            .clip(TangemTheme.shapes.roundedCorners8)
            .background(tileColor)
            .padding(TangemTheme.dimens.spacing4)
            .testTag(SelectProviderBottomSheetTestTags.PAYMENT_METHOD_ICON),
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(method.themedImageUrl())
            .crossfade(enable = true)
            .allowHardware(false)
            .build(),
        contentDescription = null,
    )
}