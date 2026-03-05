package com.tangem.core.ui.ds.contextmenu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.checkbox.TangemCheckbox
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Item with checkbox for [TangemContextMenu].
 */
@Composable
fun TangemContextMenuCheckboxItem(title: TextReference, isChecked: Boolean, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .width(238.dp)
            .clickableSingle(onClick = onClick)
            .padding(
                vertical = TangemTheme.dimens2.x5,
                horizontal = TangemTheme.dimens2.x4,
            ),
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography2.headingSemibold17,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
        )
        TangemCheckbox(
            modifier = Modifier,
            isRounded = true,
            isChecked = isChecked,
            onCheckedChange = { onClick() },
        )
    }
}