package com.tangem.features.staking.impl.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.staking.impl.R

@Composable
internal fun ValidatorImagePlaceholder() {
    Icon(
        painter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_staking_filled_18)),
        contentDescription = null,
        tint = TangemTheme.colors.icon.inactive,
        modifier = Modifier
            .background(TangemTheme.colors.icon.primary1, CircleShape)
            .padding(TangemTheme.dimens.size9),
    )
}