package com.tangem.features.tangempay.tiers.select

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ComparePlansBottomSheet(compare: TangemPaySelectPlanUM.ComparePlans?) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = compare != null,
            onDismissRequest = compare?.onDismiss ?: {},
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.primary,
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = resourceReference(R.string.tangempay_select_plan_compare).resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                )
                TangemButton.Close(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = compare?.onDismiss ?: {},
                )
            }
        },
        content = {
            if (compare != null) {
                ComparePlansContent(compare)
            }
        },
    )
}

@Composable
private fun ComparePlansContent(compare: TangemPaySelectPlanUM.ComparePlans) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        compare.attributes.fastForEachIndexed { attributeIndex, attribute ->
            CompareSection(
                title = attribute,
                plans = compare.plans,
                attributeIndex = attributeIndex,
            )
        }
    }
}

@Composable
private fun CompareSection(
    title: TextReference,
    plans: List<TangemPaySelectPlanUM.ComparePlans.Plan>,
    attributeIndex: Int,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            text = title.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(TangemTheme.colors3.bg.secondary),
        ) {
            plans.fastForEachIndexed { planIndex, plan ->
                CompareRow(
                    name = plan.name,
                    value = plan.values.getOrNull(attributeIndex) ?: stringReference(""),
                    divider = planIndex != plans.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun CompareRow(name: TextReference, value: TextReference, divider: Boolean) {
    TangemRow(
        divider = divider,
        contentLead = TangemRowContentLead.End,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            Text(
                text = name.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        valueSlot = {
            TangemRowText(
                text = value,
                role = TangemRowTextRole.Value,
                maxLines = Int.MAX_VALUE,
            )
        },
    )
}

@Preview(showBackground = true, widthDp = 402)
@Preview(showBackground = true, widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComparePlansContentPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.primary),
        ) {
            ComparePlansContent(compare = previewCompare())
        }
    }
}

private fun previewCompare() = TangemPaySelectPlanUM.ComparePlans(
    attributes = persistentListOf(
        stringReference("Available cards"),
        stringReference("Visa programme"),
        stringReference("Plan fee"),
        stringReference("FX fee (for non-USD purchases)"),
        stringReference("Daily spending limit, per card"),
    ),
    plans = persistentListOf(
        TangemPaySelectPlanUM.ComparePlans.Plan(
            name = stringReference("Basic"),
            values = persistentListOf(
                stringReference("Very Very Very Very Very Very Very Very Very Very Very Long Text"),
                stringReference("Platinum"),
                stringReference("$0"),
                stringReference("1%"),
                stringReference("$10,000"),
            ),
        ),
        TangemPaySelectPlanUM.ComparePlans.Plan(
            name = stringReference("Plus"),
            values = persistentListOf(
                stringReference("Virtual, up to 5 cards"),
                stringReference("Signature"),
                stringReference("$29.99/month"),
                stringReference("1%\n2%\n3%"),
                stringReference("$50,000"),
            ),
        ),
    ),
    onDismiss = {},
)