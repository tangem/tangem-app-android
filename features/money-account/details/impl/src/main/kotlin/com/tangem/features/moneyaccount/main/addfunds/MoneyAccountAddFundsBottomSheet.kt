package com.tangem.features.moneyaccount.main.addfunds

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.core.ui.res.generated.icons.ic_info_24
import com.tangem.core.ui.res.generated.icons.ic_sign_usd_32
import com.tangem.core.ui.test.VirtualAccountRequisitesBottomSheetTestTags
import com.tangem.features.moneyaccount.details.impl.R
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun MoneyAccountAddFundsBottomSheet(state: MoneyAccountAddFundsUM) {
    val title = stringReference("Account details")
        .takeIf { state.content is MoneyAccountAddFundsUM.Content.Details }

    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopBar(
                type = TangemTopBarType.BottomSheet,
                title = title,
                endContent = { TangemButton.Close(onClick = state.onDismiss) },
            )
        },
        content = { _ ->
            when (val content = state.content) {
                is MoneyAccountAddFundsUM.Content.Intro -> IntroContent(content)
                is MoneyAccountAddFundsUM.Content.Details -> DetailsContent(content)
            }
        },
    )
}

@Composable
private fun IntroContent(content: MoneyAccountAddFundsUM.Content.Intro, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IntroIcons(modifier = Modifier.padding(top = 16.dp))
        TitleText(
            text = stringReference("Received USD will be converted to USDC by 1:1 rate"),
            modifier = Modifier.padding(top = 32.dp),
        )
        SubtitleText(
            text = stringReference("It might take 1-3 days to receive the money"),
            modifier = Modifier.padding(top = 8.dp),
        )
        InfoNotification(
            title = stringReference("Only ACH and domestic wire transfers are available"),
            subtitle = stringReference("SWIFT won't pass"),
            modifier = Modifier.padding(top = 24.dp),
        )
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            text = stringReference("Show details"),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            onClick = content.onShowDetailsClick,
        )
    }
}

@Composable
private fun DetailsContent(content: MoneyAccountAddFundsUM.Content.Details, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        content.items.forEachIndexed { index, item ->
            CopyableRow(
                item = item,
                divider = index != content.items.lastIndex,
            )
        }
        InfoNotification(
            title = stringReference("Available to deposit per day: ${content.dailyLimit}"),
            subtitle = stringReference("Limit is resetting every day"),
            titleTestTag = VirtualAccountRequisitesBottomSheetTestTags.DEPOSIT_LIMIT_TITLE,
            subtitleTestTag = VirtualAccountRequisitesBottomSheetTestTags.DEPOSIT_LIMIT_SUBTITLE,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
        )
        TangemButton(
            text = resourceReference(R.string.common_share),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            onClick = content.onShareClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .testTag(VirtualAccountRequisitesBottomSheetTestTags.SHARE_BUTTON),
        )
    }
}

@Composable
private fun CopyableRow(item: MoneyAccountAddFundsUM.DetailItem, divider: Boolean, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        divider = divider,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            TangemRowText(
                text = item.label,
                role = TangemRowTextRole.Subtitle,
            )
        },
        subtitleSlot = {
            TangemRowText(
                modifier = Modifier.testTag(VirtualAccountRequisitesBottomSheetTestTags.value(item.key)),
                text = item.value,
                role = TangemRowTextRole.Title,
                maxLines = Int.MAX_VALUE,
            )
        },
        endSlot = {
            TangemButton(
                modifier = Modifier.testTag(VirtualAccountRequisitesBottomSheetTestTags.copyButton(item.key)),
                iconStart = TangemIconUM.Icon(imageVector = Icons.ic_copy_24),
                onClick = item.onCopyClick,
                size = TangemButton.Size.X9,
                variant = TangemButton.Variant.Ghost,
                contentDescription = item.label.resolveReference(),
            )
        },
    )
}

@Composable
private fun InfoNotification(
    title: TextReference,
    subtitle: TextReference,
    modifier: Modifier = Modifier,
    titleTestTag: String = "",
    subtitleTestTag: String = "",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.status.infoSubtle)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.ic_info_24,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.status.info,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                modifier = Modifier.conditional(
                    condition = titleTestTag.isNotEmpty(),
                    modifier = { testTag(titleTestTag) },
                ),
                text = title.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                modifier = Modifier.conditional(
                    condition = subtitleTestTag.isNotEmpty(),
                    modifier = { testTag(subtitleTestTag) },
                ),
                text = subtitle.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun IntroIcons(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(-16.dp),
    ) {
        UsdIcon()
        UsdcIcon()
    }
}

@Composable
private fun UsdIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.transparent.primary)
            .border(width = 1.dp, color = TangemTheme.colors3.border.secondary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            imageVector = Icons.ic_sign_usd_32,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
        )
    }
}

@Composable
private fun UsdcIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(80.dp)) {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(width = 1.dp, color = TangemTheme.colors3.border.secondary, shape = CircleShape),
            painter = painterResource(CoreUiR.drawable.img_usdc_16),
            contentDescription = null,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(color = TangemTheme.colors3.bg.accent.violet, shape = CircleShape)
                .border(
                    width = 2.dp,
                    color = TangemTheme.colors3.bg.secondary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(CoreUiR.drawable.ic_polygon_22),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.inverse,
            )
        }
    }
}

@Composable
private fun TitleText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text.resolveReference(),
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SubtitleText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text.resolveReference(),
        style = TangemTheme.typography3.subheading.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoneyAccountAddFundsIntroPreview() {
    TangemThemePreviewRedesign {
        IntroContent(
            content = MoneyAccountAddFundsUM.Content.Intro(
                onShowDetailsClick = {},
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoneyAccountAddFundsDetailsPreview() {
    TangemThemePreviewRedesign {
        DetailsContent(
            content = MoneyAccountAddFundsUM.Content.Details(
                items = persistentListOf(
                    MoneyAccountAddFundsUM.DetailItem(
                        key = "Beneficiary name",
                        label = stringReference("Beneficiary name and address"),
                        value = "Ivan Ivanov\n18, Rue Rubens 20, Paris, Ile-de-France 75013, US",
                        onCopyClick = {},
                    ),
                    MoneyAccountAddFundsUM.DetailItem(
                        key = "Account number",
                        label = stringReference("Account number"),
                        value = "707613210122",
                        onCopyClick = {},
                    ),
                ),
                dailyLimit = "$10,000",
                onShareClick = {},
            ),
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
        )
    }
}