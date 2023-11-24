package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.exchange

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.R
import com.tangem.core.ui.components.inputrow.InputRowBestRate
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun ExchangeProvider(providerName: TextReference, providerType: TextReference, imageUrl: String) {
    Column(
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action),
    ) {
        Text(
            text = stringResource(id = R.string.express_provider),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier
                .padding(
                    start = TangemTheme.dimens.spacing12,
                    end = TangemTheme.dimens.spacing12,
                    top = TangemTheme.dimens.spacing12,
                ),
        )
        InputRowBestRate(
            imageUrl = imageUrl,
            title = providerName,
            titleExtra = providerType,
            subtitle = TextReference.Res(R.string.express_floating_rate),
        )
    }
}
