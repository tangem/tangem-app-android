package com.tangem.features.send.impl.presentation.viewmodel

import androidx.core.text.isDigitsOnly
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.tokens.model.CryptoCurrency
import java.math.BigInteger

internal fun validateMemo(memo: String, cryptoCurrency: CryptoCurrency?): Boolean {
    if (cryptoCurrency == null) return false
    if (memo.isEmpty()) return true
    return when (cryptoCurrency.network.id.value) {
        Blockchain.XRP.id -> {
            val tag = memo.toLongOrNull()
            tag != null && tag <= XRP_TAG_MAX_NUMBER
        }
        Blockchain.Stellar.id -> {
            isAssignableValue(memo)
        }
        else -> true
    }
}

private fun isAssignableValue(value: String): Boolean {
    val memoType = when {
        value.isNotEmpty() && value.isDigitsOnly() -> XlmMemoType.ID
        else -> XlmMemoType.TEXT
    }
    return when (memoType) {
        XlmMemoType.TEXT -> {
            // from org.stellar.sdk.MemoText
            value.toByteArray().size <= XLM_MEMO_MAX_LENGTH
        }
        XlmMemoType.ID -> {
            try {
                // from com.tangem.blockchain.blockchains.stellar.StellarMemo.toStellarSdkMemo
                value.toBigInteger() in BigInteger.ZERO..Long.MAX_VALUE.toBigInteger() * 2.toBigInteger()
            } catch (ex: NumberFormatException) {
                false
            }
        }
    }
}

private enum class XlmMemoType { TEXT, ID }

private const val XRP_TAG_MAX_NUMBER = 4294967295
private const val XLM_MEMO_MAX_LENGTH = 28
