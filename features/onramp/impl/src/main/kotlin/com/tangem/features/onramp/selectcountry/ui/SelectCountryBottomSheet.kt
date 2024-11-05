package com.tangem.features.onramp.selectcountry.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.onramp.selectcountry.entity.CountryListUM

@Composable
internal fun SelectCountryBottomSheet(config: TangemBottomSheetConfig, content: @Composable () -> Unit) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = config,
        content = { content() },
    )
}

@Composable
internal fun OnrampCountryList(state: CountryListUM, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(
            items = state.items,
            key = { item -> item.id },
            contentType = { item -> item::class.java },
            itemContent = { item ->
                CountryListItem(
                    state = item,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                )
            },
        )
    }
}
