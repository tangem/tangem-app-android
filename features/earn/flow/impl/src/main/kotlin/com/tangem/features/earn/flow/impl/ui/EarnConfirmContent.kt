@file:Suppress("MagicNumber")

package com.tangem.features.earn.flow.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.legal.LegalCaption
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.earn.flow.impl.ui.state.EarnConfirmUM
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorBlockComponent

@Composable
internal fun EarnConfirmContent(
    state: EarnConfirmUM,
    title: TextReference,
    feeBlock: FeeSelectorBlockComponent?,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasBottomHint = state.legal != null || state.stepLabel != null
    EarnFlowScaffold(
        topBar = {
            TangemTopNavigation(
                title = title,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                onBack = onBack,
                onClose = onClose,
            )
        },
        bottomExtraTop = if (hasBottomHint) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.legal?.let { legal ->
                        LegalCaption(
                            onTermsOfServiceClick = { legal.onClick(legal.termsOfUseUrl) },
                            onPrivacyPolicyClick = { legal.onClick(legal.privacyPolicyUrl) },
                            termsTitleRes = R.string.common_terms_of_use,
                        )
                    }
                    state.stepLabel?.let { stepLabel -> EarnSecondaryText(text = stepLabel) }
                }
            }
        } else {
            null
        },
        bottom = { EarnFlowButton(button = state.primary, modifier = Modifier.fillMaxWidth()) },
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            item(key = "amount") { Amount(state) }
            state.notice?.let { notice ->
                item(key = "notice") { EarnNotice(text = notice, modifier = Modifier.padding(vertical = 12.dp)) }
            }
            item(key = "details") { Details(state = state, feeBlock = feeBlock) }
        }
    }
}

@Composable
private fun Amount(state: EarnConfirmUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EarnLogos(icons = state.icons, size = 72.dp, overlap = 16.dp)
        Text(
            text = state.amountFiat.resolveReference(),
            style = TangemTheme.typography3.display.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = state.amountCrypto.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun Details(state: EarnConfirmUM, feeBlock: FeeSelectorBlockComponent?) {
    Column(
        modifier = Modifier.padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.rows.isNotEmpty()) {
            EarnRows(
                rows = state.rows,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(TangemTheme.colors3.bg.secondary),
            )
        }
        feeBlock?.Content(Modifier.fillMaxWidth())
        EarnNotifications(state.notifications)
    }
}