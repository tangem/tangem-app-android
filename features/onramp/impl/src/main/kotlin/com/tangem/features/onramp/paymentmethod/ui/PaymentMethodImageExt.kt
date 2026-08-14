package com.tangem.features.onramp.paymentmethod.ui

import androidx.compose.runtime.Composable
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.domain.onramp.model.OnrampPaymentMethod

@Composable
internal fun OnrampPaymentMethod.themedImageUrl(): String = imageUrl(isDark = LocalIsInDarkTheme.current)