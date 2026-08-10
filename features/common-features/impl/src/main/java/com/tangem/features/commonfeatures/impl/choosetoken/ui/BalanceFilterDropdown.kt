package com.tangem.features.commonfeatures.impl.choosetoken.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds2.filter.TangemFilterItem
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.commonfeatures.api.choosetoken.model.BalanceFilter
import com.tangem.features.commonfeatures.api.choosetoken.model.BalanceFilterUM
import com.tangem.features.commonfeatures.impl.R as CommonFeaturesR

/** Header chip that opens a checkmark dropdown to switch [BalanceFilter] in the FROM token selector. */
@Composable
internal fun BalanceFilterDropdown(um: BalanceFilterUM, modifier: Modifier = Modifier) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        TangemFilterItem(
            state = TangemFilterItemUM.Inactive(
                id = "balance_filter",
                label = um.selected.labelReference(),
                onClick = { isExpanded = true },
            ),
            variant = TangemFilterItem.Variant.Transparent,
        )
        TangemContextMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            BalanceFilter.entries.forEachIndexed { index, filter ->
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(238.dp)
                            .clickableSingle(
                                onClick = {
                                    um.onOptionSelected(filter)
                                    isExpanded = false
                                },
                            )
                            .padding(vertical = 10.dp, horizontal = 16.dp),
                    ) {
                        Text(
                            text = filter.labelReference().resolveReference(),
                            style = TangemTheme.typography3.body.medium,
                            color = TangemTheme.colors3.text.primary,
                            maxLines = 1,
                        )
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            if (um.selected == filter) {
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .size(20.dp)
                                        .background(color = TangemTheme.colors3.icon.primary, shape = CircleShape),
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_check_default_24),
                                        contentDescription = null,
                                        tint = TangemTheme.colors3.icon.inverse,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(2.dp)
                                            .size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (index < BalanceFilter.entries.size - 1) {
                        HorizontalDivider(thickness = 0.5.dp, color = TangemTheme.colors3.border.tertiary)
                    }
                }
            }
        }
    }
}

private fun BalanceFilter.labelReference(): TextReference = when (this) {
    BalanceFilter.All -> resourceReference(CommonFeaturesR.string.common_all)
    BalanceFilter.HideZero -> resourceReference(CommonFeaturesR.string.swap_token_selector_filter_hide_zero_balance)
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun BalanceFilterDropdownPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BalanceFilterDropdown(um = BalanceFilterUM(selected = BalanceFilter.HideZero, onOptionSelected = {}))
            BalanceFilterDropdown(um = BalanceFilterUM(selected = BalanceFilter.All, onOptionSelected = {}))
        }
    }
}