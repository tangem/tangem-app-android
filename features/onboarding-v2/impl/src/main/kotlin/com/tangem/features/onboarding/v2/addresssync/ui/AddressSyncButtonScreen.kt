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
import androidx.compose.ui.unit.dp
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
            .padding(top = 56.dp),
    ) {
        SpacerHHalf()
        Icon(
            painter = painterResource(id = R.drawable.ic_sync_56),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(56.dp),
        )
        Text(
            text = stringResourceSafe(R.string.onboarding_address_sync_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(
                    start = 34.dp,
                    end = 34.dp,
                    top = 28.dp,
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
                    start = 16.dp,
                    end = 16.dp,
                    top = 154.dp,
                    bottom = 16.dp,
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
                start = 34.dp,
                end = 34.dp,
                top = 28.dp,
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