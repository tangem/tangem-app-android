package com.tangem.features.earn.impl.info.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.inputrow.inner.InputRowAsyncImage
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_20
import com.tangem.features.earn.api.ui.EarnRowUM
import com.tangem.features.earn.impl.info.EarnInfoTexts
import com.tangem.features.earn.impl.info.ui.state.EarnInfoUM
import com.tangem.features.earn.impl.picker.ui.EarnBenefitRow
import com.tangem.features.earn.impl.picker.ui.PAGE_MARGIN
import com.tangem.features.earn.impl.picker.ui.state.EarnBenefitUM
import com.tangem.features.earn.impl.ui.EarnDetailRow
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun MyFundsSection(state: EarnInfoUM.MyFundsUM, modifier: Modifier = Modifier) {
    Section(title = EarnInfoTexts.myFundsSection, modifier = modifier) {
        ProviderRow(
            title = state.provider,
            icon = state.providerIcon,
            onHowItWorksClick = state.onHowItWorksClick,
            modifier = Modifier.padding(top = 12.dp),
        )
        state.description?.let { description ->
            Text(
                text = description.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = PAGE_MARGIN, end = PAGE_MARGIN, top = 8.dp, bottom = 12.dp),
            )
        }
        // Only this card is padded in the design: the rows of More details start right under the heading
        DetailRows(rows = state.rows, modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
internal fun BenefitsSection(benefits: ImmutableList<EarnBenefitUM>, modifier: Modifier = Modifier) {
    Section(title = EarnInfoTexts.benefitsSection, modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = PAGE_MARGIN, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            benefits.forEach { benefit -> EarnBenefitRow(benefit = benefit) }
        }
    }
}

@Composable
internal fun DetailsSection(details: ImmutableList<EarnRowUM>, modifier: Modifier = Modifier) {
    Section(title = EarnInfoTexts.detailsSection, modifier = modifier) {
        DetailRows(rows = details)
    }
}

/** The rows keep 8 dp of the page margin and 16 dp of their own: their text lines up with the heading. */
@Composable
private fun DetailRows(rows: ImmutableList<EarnRowUM>, modifier: Modifier = Modifier) {
    if (rows.isEmpty()) return

    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        rows.forEach { row -> EarnDetailRow(state = row, divider = true) }
    }
}

@Composable
private fun Section(title: TextReference, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier.padding(horizontal = PAGE_MARGIN, vertical = 12.dp),
        )
        content()
    }
}

@Composable
private fun ProviderRow(
    title: TextReference,
    icon: ImageReference,
    onHowItWorksClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_MARGIN, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProviderIcon(icon = icon)
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        onHowItWorksClick?.let { onClick -> HowItWorksLink(onClick = onClick) }
    }
}

@Composable
private fun ProviderIcon(icon: ImageReference) {
    val modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)

    when (icon) {
        is ImageReference.Url -> InputRowAsyncImage(imageUrl = icon.url, modifier = modifier)
        is ImageReference.Res -> Image(
            painter = painterResource(icon.resId),
            contentDescription = null,
            modifier = modifier,
        )
    }
}

@Composable
private fun HowItWorksLink(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = EarnInfoTexts.howItWorks.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Icon(
            imageVector = Icons.ic_chevron_right_20,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}