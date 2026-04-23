package com.tangem.features.onboarding.v2.addresssync.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SpacerHHalf
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.extensions.pluralStringResourceSafe
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.addresssync.model.AddressSyncState
import com.tangem.features.onboarding.v2.impl.R

@Composable
internal fun AddressSyncButtonScreen(
    state: AddressSyncState.Success,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = TangemTheme.dimens.spacing56),
    ) {
        SpacerHHalf()
        Icon(
            painter = painterResource(id = R.drawable.ic_sync_56),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(TangemTheme.dimens.size56),
        )
        Text(
            text = stringResourceSafe(R.string.onboarding_address_sync_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(
                    start = TangemTheme.dimens.spacing34,
                    end = TangemTheme.dimens.spacing34,
                    top = TangemTheme.dimens.spacing28,
                ),
        )
        AddressSyncDescription(state.currenciesCount)
        SpacerHMax()
        PrimaryButtonIconEnd(
            text = stringResourceSafe(R.string.common_generate_addresses),
            onClick = onSyncClick,
            iconResId = R.drawable.ic_tangem_24,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    top = TangemTheme.dimens.spacing154,
                    bottom = TangemTheme.dimens.spacing16,
                ),
            showProgress = state.isButtonLoading,
        )
    }
}

@Composable
private fun ColumnScope.AddressSyncDescription(currenciesCount: Int) {
    Text(
        text = pluralStringResourceSafe(
            id = R.plurals.onboarding_address_sync_description,
            count = currenciesCount,
            currenciesCount,
        ),
        style = TangemTheme.typography.body2,
        color = TangemTheme.colors.text.secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(
                start = TangemTheme.dimens.spacing34,
                end = TangemTheme.dimens.spacing34,
                top = TangemTheme.dimens.spacing28,
            ),
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddressSyncButtonScreenPreview() {
    TangemThemePreview {
        AddressSyncButtonScreen(
            state = AddressSyncState.Success(
                currencies = emptyList(),
            ),
            onSyncClick = {},
        )
    }
}