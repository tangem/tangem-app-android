package com.tangem.features.tangempay.withdraw.bank.recipients.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_binoculars_24
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.recipients.state.TangemPayBankWithdrawRecipientsUM
import com.tangem.features.tangempay.withdraw.bank.recipients.state.TangemPayBankWithdrawRecipientsUM.ContentUM
import com.tangem.features.tangempay.withdraw.bank.recipients.state.TangemPayBankWithdrawRecipientsUM.RecipientUM

@Composable
internal fun TangemPayBankWithdrawRecipientsScreen(
    state: TangemPayBankWithdrawRecipientsUM,
    modifier: Modifier = Modifier,
) {
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                title = resourceReference(R.string.tangempay_withdraw_bank_recipients_title),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = state.onBackClick,
                onClose = state.onCloseClick,
            )
        },
        overlay = {
            BankWithdrawSearchFooter(
                state = state.search,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        },
    ) { contentPadding ->
        AnimatedContent(
            targetState = state.content,
            contentKey = { content -> content::class },
            modifier = Modifier.fillMaxSize(),
        ) { content ->
            when (content) {
                ContentUM.Loading -> LoadingContent(contentPadding = contentPadding)
                ContentUM.NoResults -> NoResultsContent(contentPadding = contentPadding)
                is ContentUM.Recipients -> RecipientsContent(content = content, contentPadding = contentPadding)
            }
        }
    }
}

@Composable
private fun RecipientsContent(
    content: ContentUM.Recipients,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            // The scaffold padding already covers the navigation bar, so only the keyboard's excess lifts the list.
            .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + FooterInset,
        ),
    ) {
        items(items = content.items, key = RecipientUM::id) { item ->
            RecipientRow(item = item)
        }
    }
}

@Composable
private fun RecipientRow(item: RecipientUM) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = item.onClick,
        startSlot = { RecipientInitial(initial = item.initial) },
        titleSlot = { TangemRowText(text = item.name, role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = item.subtitle, role = TangemRowTextRole.Subtitle) },
    )
}

@Composable
private fun RecipientInitial(initial: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(AvatarSize)
            .background(color = TangemTheme.colors3.bg.opaque.secondary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun LoadingContent(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            repeat(times = LOADING_ROWS_COUNT) {
                TangemRow(
                    verticalAlignment = TangemRowVerticalAlignment.Center,
                    startSlot = {
                        TangemShimmer(modifier = Modifier.size(AvatarSize), radius = AvatarSize / 2)
                    },
                    titleSlot = {
                        TangemShimmer(style = TangemTheme.typography3.body.medium, modifier = Modifier.width(140.dp))
                    },
                    subtitleSlot = {
                        TangemShimmer(style = TangemTheme.typography3.caption.medium, modifier = Modifier.width(88.dp))
                    },
                )
            }
        }
    }
}

@Composable
private fun NoResultsContent(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = TangemTheme.colors3.bg.opaque.primary, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_binoculars_24,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            modifier = Modifier.padding(top = 32.dp),
            text = stringResourceSafe(R.string.tangempay_withdraw_bank_recipients_search_empty),
            textAlign = TextAlign.Center,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
        )
    }
}

private val AvatarSize = 40.dp
private val FooterInset = 88.dp
private const val LOADING_ROWS_COUNT = 6

private class RecipientsPreviewProvider : PreviewParameterProvider<TangemPayBankWithdrawRecipientsUM> {
    override val values: Sequence<TangemPayBankWithdrawRecipientsUM> = sequenceOf(
        TangemPayBankWithdrawRecipientsPreviewData.content,
        TangemPayBankWithdrawRecipientsPreviewData.loading,
        TangemPayBankWithdrawRecipientsPreviewData.noResults,
    )
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TangemPayBankWithdrawRecipientsScreen(
    @PreviewParameter(RecipientsPreviewProvider::class) state: TangemPayBankWithdrawRecipientsUM,
) {
    TangemThemePreviewRedesign {
        TangemPayBankWithdrawRecipientsScreen(state = state)
    }
}