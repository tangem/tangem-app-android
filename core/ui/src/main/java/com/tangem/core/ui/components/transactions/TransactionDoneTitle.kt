package com.tangem.core.ui.components.transactions

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toTimeFormat

/**
 * Common transaction done screen title
 *
 * @param titleRes title resource
 * @param date transaction timestamp in millis
 */
@Composable
fun TransactionDoneTitle(@StringRes titleRes: Int, date: Long, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_in_process_64),
            contentDescription = null,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing8)
                .size(TangemTheme.dimens.size64),
        )
        Text(
            text = stringResource(id = titleRes),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing16),
        )
        Text(
            text = stringResource(
                id = R.string.send_date_format,
                date.toTimeFormat(DateTimeFormatters.dateFormatter),
                date.toTimeFormat(),
            ),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing4),
        )
    }
}

// region Previews
@Preview
@Composable
private fun TransactionDoneTitlePreview_Light() {
    TangemTheme {
        TransactionDoneTitle(
            titleRes = R.string.sent_transaction_sent_title,
            date = 0,
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary)
                .padding(TangemTheme.dimens.spacing16),
        )
    }
}

@Preview
@Composable
private fun TransactionDoneTitlePreview_Dark() {
    TangemTheme(isDark = true) {
        TransactionDoneTitle(
            titleRes = R.string.sent_transaction_sent_title,
            date = 0,
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary)
                .padding(TangemTheme.dimens.spacing16),
        )
    }
}
// endregion
