package com.tangem.features.tangempay.campaign.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation

@Composable
internal fun TangemPayCampaignLayout(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    card: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    texts: @Composable ColumnScope.() -> Unit,
) {
    TangemTopBarScaffold(modifier = modifier, topBar = topBar) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                card()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp),
                    content = texts,
                )
            }
            footer()
        }
    }
}

@Composable
internal fun TangemPayCampaignTopBar(onCloseClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    TangemTopNavigation(
        modifier = modifier,
        blurBackground = false,
        endButton = {
            if (onCloseClick != null) {
                TangemButton.Close(onClick = onCloseClick)
            } else {
                Spacer(modifier = Modifier.size(TangemTopNavigation.ButtonSlotSize))
            }
        },
    )
}