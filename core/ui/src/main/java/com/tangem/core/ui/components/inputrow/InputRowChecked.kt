package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun InputRowChecked(text: TextReference, checked: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(TangemTheme.dimens.spacing12),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AnimatedVisibility(
            modifier = Modifier,
            visible = checked,
        ) {
            Icon(
                painter = rememberVectorPainter(image = ImageVector.vectorResource(id = R.drawable.ic_check_24)),
                tint = TangemTheme.colors.icon.accent,
                contentDescription = null,
            )
        }
        if (checked.not()) {
            Box(Modifier.height(TangemTheme.dimens.size24))
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        Column(Modifier.background(TangemTheme.colors.background.primary)) {
            InputRowChecked(
                text = stringReference("Title Title Title Title Title Title Title Title Title"),
                checked = false,
                modifier = Modifier.width(300.dp),
            )
            InputRowChecked(
                text = stringReference("Title Title Title Title Title Title Title Title Title"),
                checked = true,
                modifier = Modifier.width(300.dp),
            )
        }
    }
}