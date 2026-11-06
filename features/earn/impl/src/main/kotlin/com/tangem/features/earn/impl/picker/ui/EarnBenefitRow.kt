package com.tangem.features.earn.impl.picker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.earn.impl.picker.ui.state.EarnBenefitUM

/** An icon and one paragraph: the bold title leads, the description follows it. */
@Composable
internal fun EarnBenefitRow(benefit: EarnBenefitUM, modifier: Modifier = Modifier) {
    val titleColor = TangemTheme.colors3.text.primary
    val descriptionColor = TangemTheme.colors3.text.secondary
    val title = benefit.title.resolveReference()
    val description = benefit.description.resolveReference()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        benefit.icon?.let { icon ->
            Icon(
                imageVector = icon.imageVector(),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = titleColor)) { append(title) }
                withStyle(SpanStyle(color = descriptionColor)) { append(" $description") }
            },
            style = TangemTheme.typography3.subheading.medium,
        )
    }
}