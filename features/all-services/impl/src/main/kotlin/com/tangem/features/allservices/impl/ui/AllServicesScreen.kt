package com.tangem.features.allservices.impl.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.GroupEntry
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_gift_24
import com.tangem.features.allservices.impl.entries.AllServicesEntry
import com.tangem.features.allservices.impl.entries.AllServicesSection
import com.tangem.features.allservices.impl.ui.state.AllServicesUM

@Composable
internal fun AllServicesScreen(
    state: AllServicesUM,
    sections: List<AllServicesSection>,
    onAddWalletClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            TangemTopNavigation(
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                isEndButtonsGroupBackgroundShown = true,
                endButtonsGroup = {
                    TangemButton.GroupEntry(
                        iconUM = TangemIconUM.Icon(imageVector = Icons.ic_gift_24),
                        onClick = state.onReferralClick,
                    )
                },
                endButton = {
                    TangemButton.Close(onClick = state.onCloseClick)
                },
            )
        },
        content = { contentPadding ->
            // Collected here rather than in a host flow: a StateFlow's current value is read on the very
            // first frame, so a row appears or disappears with no intermediate empty state.
            val visibleSections = sections.mapNotNull { section ->
                val visibleEntries = section.entries.filter { entry ->
                    val isVisible by entry.isVisible.collectAsStateWithLifecycle()
                    isVisible
                }
                visibleEntries.takeIf { it.isNotEmpty() }?.let { section to it }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
                item(key = "header", contentType = "header") {
                    AllServicesHeader(onAddWalletClick = onAddWalletClick, onBuyTangemClick = state.onBuyTangemClick)
                }
                visibleSections.forEach { (section, entries) -> section(section, entries) }
            }
        },
    )
}

private fun LazyListScope.section(section: AllServicesSection, entries: List<AllServicesEntry>) {
    section.title?.let { title ->
        item(key = section.key, contentType = "section_title") {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            )
        }
    }
    items(items = entries, key = AllServicesEntry::key, contentType = { "entry" }) { entry ->
        entry.Content(modifier = Modifier)
    }
}