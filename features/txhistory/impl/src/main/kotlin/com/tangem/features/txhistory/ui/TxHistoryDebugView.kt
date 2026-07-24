package com.tangem.features.txhistory.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.DialogFullScreen
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo

@Composable
internal fun TxHistoryDebugView(model: TxHistoryInfo, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val fieldColors = with(TangemTheme.colors3.text.accent) {
        listOf(blue, red, green, violet, orange, yellow)
    }
    val json = model.toColoredDebugString(fieldColors)
    DialogFullScreen(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors3.bg.primary)
                .systemBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Debug view",
                    style = TangemTheme.typography3.heading.medium,
                    color = TangemTheme.colors3.text.primary,
                )
                TangemButton.Close(onClick = onDismiss)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SummaryRow(label = "Type", value = model.debugSubtype)
                SummaryRow(label = "On-chain leg", value = model.debugMatchedOnChainLeg)
            }

            SelectionContainer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TangemTheme.colors3.bg.secondary),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                ) {
                    Text(
                        text = json,
                        style = TangemTheme.typography3.caption.medium.copy(fontFamily = FontFamily.Monospace),
                        color = TangemTheme.colors3.text.primary,
                        softWrap = false,
                    )
                }
            }

            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringReference("Copy"),
                iconStart = TangemIconUM.Icon(Icons.ic_copy_24),
                onClick = { clipboard.setText(json) },
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        Text(
            text = value,
            style = TangemTheme.typography3.caption.medium.copy(fontFamily = FontFamily.Monospace),
            color = TangemTheme.colors3.text.primary,
        )
    }
}

val TxHistoryInfo.debugSubtype: String
    get() = when (this) {
        is OnChainTx.BSDK -> "OnChainTx.BSDK"
        is OnChainTx.TangemPay -> "OnChainTx.TangemPay"
        is ExpressTx.Swap -> "ExpressTx.Swap"
        is ExpressTx.Onramp -> "ExpressTx.Onramp"
    }

val TxHistoryInfo.debugMatchedOnChainLeg: String
    get() = when (this) {
        is ExpressTx -> txInfo?.debugSubtype ?: "null (unmatched or not loaded yet)"
        is OnChainTx -> "$debugSubtype (self)"
    }

/**
 * Pretty-prints `toString()` and tints each top-level field of the concrete [TxHistoryInfo] subtype with its own color
 * from [fieldColors], cycling. Only the direct fields are colored: everything nested inside a field keeps that field's
 * color. The class name and the outer brackets stay in the default text color.
 */
private fun TxHistoryInfo.toColoredDebugString(fieldColors: List<Color>): AnnotatedString {
    val raw = toString()
    val open = raw.indexOf('(')
    if (open < 0 || fieldColors.isEmpty()) return AnnotatedString(raw)

    val className = raw.substring(0, open)
    val fields = raw.substring(open + 1, raw.length - 1).splitTopLevelFields()

    return buildAnnotatedString {
        append(className)
        append('(')
        fields.forEachIndexed { i, field ->
            append("\n  ")
            withStyle(SpanStyle(color = fieldColors[i % fieldColors.size])) {
                append(field.prettyPrint(startDepth = 1))
            }
            if (i != fields.lastIndex) append(',')
        }
        append("\n)")
    }
}

/** Splits `a=1, b=Nested(x, y), c=[..]` into its top-level `name=value` parts, honoring bracket nesting. */
private fun String.splitTopLevelFields(): List<String> {
    val fields = mutableListOf<String>()
    val field = StringBuilder()
    var depth = 0
    var i = 0
    while (i < length) {
        when (val char = this[i]) {
            '(', '[' -> {
                depth++
                field.append(char)
            }
            ')', ']' -> {
                depth--
                field.append(char)
            }
            ',' -> {
                if (depth == 0) {
                    fields.add(field.toString())
                    field.clear()
                    if (i + 1 < length && this[i + 1] == ' ') i++
                } else {
                    field.append(char)
                }
            }
            else -> field.append(char)
        }
        i++
    }
    if (field.isNotEmpty()) fields.add(field.toString())
    return fields
}

/** Reflows one `name=value` field into an indented tree, starting nested content at [startDepth]. */
private fun String.prettyPrint(startDepth: Int): String {
    val builder = StringBuilder()
    var depth = startDepth
    var i = 0

    fun newLine() {
        builder.append('\n')
        repeat(depth) { builder.append("  ") }
    }

    while (i < length) {
        when (val char = this[i]) {
            '(', '[' -> {
                depth++
                builder.append(char)
                newLine()
            }
            ')', ']' -> {
                depth--
                newLine()
                builder.append(char)
            }
            ',' -> {
                builder.append(char)
                if (i + 1 < length && this[i + 1] == ' ') i++
                newLine()
            }
            else -> builder.append(char)
        }
        i++
    }
    return builder.toString()
}

// region Preview

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TxHistoryDebugViewPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryRow(label = "Type", value = "ExpressTx.Swap")
            SummaryRow(label = "On-chain leg", value = "OnChainTx.BSDK")
            Text(
                text = "Swap(\n  isOutgoing=true,\n  txInfo=BSDK(\n    txHash=0xdeadbeef\n  )\n)",
                style = TangemTheme.typography3.caption.medium.copy(fontFamily = FontFamily.Monospace),
                color = TangemTheme.colors3.text.primary,
            )
        }
    }
}

// endregion