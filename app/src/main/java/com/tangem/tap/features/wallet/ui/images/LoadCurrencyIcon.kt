package com.tangem.tap.features.wallet.ui.images

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.utils.widget.ImageFilterView
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.IconsUtil
import com.tangem.blockchain.common.Token
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.tap.common.extensions.getColor
import com.tangem.tap.common.extensions.getRoundIconRes
import com.tangem.tap.common.extensions.getTextColor
import com.tangem.tap.domain.extensions.getCustomIconUrl
import com.tangem.tap.domain.tokens.getIconUrl
import com.tangem.wallet.R

private const val QCX = "QCX"
private const val VOYR = "VOYRME"

fun loadCurrencyIcon(
    currencyImageView: ImageFilterView,
    currencyTextView: TextView,
    token: Token?,
    blockchain: Blockchain,
) {
    when {
        token == null -> currencyImageView.loadIcon(
            iconUrl = getIconUrl(blockchain.toNetworkId()),
            placeholderRes = blockchain.getRoundIconRes(),
            onStart = {
                if (blockchain.isTestnet()) {
                    currencyImageView.saturation = 0f
                } else {
                    currencyImageView.colorFilter = null
                }
            },
        )
        token.symbol == QCX -> currencyImageView.load(R.drawable.ic_qcx)
        token.symbol == VOYR -> currencyImageView.load(R.drawable.ic_voyr)
        else -> currencyImageView.loadIcon(
            iconUrl = getTokenIconUrl(token, blockchain),
            placeholderRes = R.drawable.shape_circle,
            onStart = {
                currencyTextView.text = token.symbol.take(1)
                currencyTextView.setTextColor(token.getTextColor())

                if (blockchain.isTestnet()) {
                    currencyImageView.saturation = 0f
                }
            },
            onError = {
                currencyImageView.colorFilter = PorterDuffColorFilter(
                    /* color = */
                    token.getColor(),
                    /* mode = */
                    PorterDuff.Mode.SRC_ATOP,
                )
            },
            onSuccess = {
                if (!blockchain.isTestnet()) {
                    currencyImageView.colorFilter = null
                }
            },
        )
    }
}

private inline fun ImageView.loadIcon(
    iconUrl: String?,
    placeholderRes: Int,
    crossinline onStart: () -> Unit = {},
    crossinline onSuccess: () -> Unit = {},
    crossinline onError: () -> Unit = {},
) {
    ImageRequest.Builder(context)
        .data(iconUrl)
        .placeholder(placeholderRes)
        .error(placeholderRes)
        .fallback(placeholderRes)
        .transformations(
            RoundedCornersTransformation(
                topLeft = 32f,
                topRight = 32f,
                bottomLeft = 32f,
                bottomRight = 32f,
            ),
        )
        .listener(
            onStart = { onStart() },
            onSuccess = { _, _ -> onSuccess() },
            onError = { _, _ -> onError() },
        )
        .target(imageView = this)
        .build()
        .also(context.imageLoader::enqueue)
}

private fun getTokenIconUrl(token: Token, blockchain: Blockchain): String? {
    return token.id?.let(::getIconUrl)
        ?: token.getCustomIconUrl()
        ?: IconsUtil.getTokenIconUri(blockchain, token)
            ?.toString()
}
