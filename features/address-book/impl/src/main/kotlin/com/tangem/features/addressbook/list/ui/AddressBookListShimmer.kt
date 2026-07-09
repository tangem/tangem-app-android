package com.tangem.features.addressbook.list.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun AddressBookListShimmer(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        TangemTopNavigation(
            title = resourceReference(R.string.address_book_title),
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            blurBackground = false,
            onBack = onBackClick,
        )
        ProvideTangemShimmer {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp)
                    .background(
                        color = TangemTheme.colors3.bg.secondary,
                        shape = RoundedCornerShape(24.dp),
                    ),
            ) {
                repeat(SHIMMER_ROW_COUNT) { ContactRowShimmer() }
            }
        }
    }
}

@Composable
private fun ContactRowShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RectangleShimmer(
            modifier = Modifier.size(40.dp),
            radius = 32.dp,
        )
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RectangleShimmer(modifier = Modifier.size(width = 140.dp, height = 16.dp))
            RectangleShimmer(modifier = Modifier.size(width = 90.dp, height = 12.dp))
        }
    }
}

private const val SHIMMER_ROW_COUNT = 3

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AddressBookListShimmer() {
    TangemThemePreviewRedesign {
        AddressBookListShimmer(
            onBackClick = {},
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}