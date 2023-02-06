package com.tangem.tap.features.wallet.ui.images

import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.utils.widget.ImageFilterView
import coil.imageLoader
import coil.request.ImageRequest
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.IconsUtil
import com.tangem.blockchain.common.Token
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.tap.common.extensions.getColor
import com.tangem.tap.common.extensions.getTextColor
import com.tangem.tap.domain.extensions.getCustomIconUrl
import com.tangem.tap.domain.tokens.getIconUrl
import com.tangem.wallet.R

private const val QCX = "QCX"
private const val VOYR = "VOYRME"

class CurrencyIconRequest(
    private val currencyImageView: ImageFilterView,
    private val currencyTextView: TextView?,
    private val token: Token?,
    private val blockchain: Blockchain,
    private val getLocalImage: Boolean = false,
) {
    fun load() {
        when {
            token == null && blockchain.isTestnet() -> loadTestnetBlockchainIcon()
            token == null -> loadBlockchainIcon()
            blockchain.isTestnet() -> loadTestnetTokenIcon()
            else -> loadTokenIcon()
        }
    }

    private fun loadBlockchainIcon() {
        loadBlockchainIconBase(
            onStart = {
                currencyImageView.colorFilter = null
            },
        )
    }

    private fun loadTestnetBlockchainIcon() {
        loadBlockchainIconBase(
            onStart = {
                currencyImageView.saturation = 0f
            },
        )
    }

    private fun loadTokenIcon() {
        loadTokenIconBase(
            onStart = {
                currencyImageView.setColorFilter(it.getColor())
            },
            onSuccess = {
                currencyImageView.colorFilter = null
            },
            onError = {
                currencyImageView.setColorFilter(it.getColor())
                currencyTextView?.setTextColor(it.getTextColor())
            },
        )
    }

    private fun loadTestnetTokenIcon() {
        loadTokenIconBase(
            onStart = {
                currencyImageView.saturation = 0f
            },
            onError = {
                currencyImageView.saturation = 0f
                currencyImageView.setColorFilter(it.getColor(true))
                currencyTextView?.setTextColor(it.getTextColor(true))
            },
        )
    }

    private inline fun loadBlockchainIconBase(
        crossinline onStart: (Blockchain) -> Unit = {},
        crossinline onSuccess: (Blockchain) -> Unit = {},
        crossinline onError: (Blockchain) -> Unit = {},
    ) {
        currencyImageView.loadIcon(
            data = if (getLocalImage) getActiveIconRes(blockchain.id) else getIconUrl(blockchain.toNetworkId()),
            placeholderRes = getActiveIconRes(blockchain.id),
            onStart = { onStart(blockchain) },
            onSuccess = { onSuccess(blockchain) },
            onError = { onError(blockchain) },
        )
    }

    private inline fun loadTokenIconBase(
        crossinline onStart: (Token) -> Unit = {},
        crossinline onSuccess: (Token) -> Unit = {},
        crossinline onError: (Token) -> Unit = {},
    ) {
        if (token == null) return

        currencyImageView.loadIcon(
            data = getTokenIcon(token, blockchain),
            placeholderRes = R.drawable.shape_circle,
            onStart = {
                currencyTextView?.text = token.name.take(1)
                currencyTextView?.setTextColor(token.getTextColor())
                onStart(token)
            },
            onSuccess = {
                currencyTextView?.text = null
                onSuccess(token)
            },
            onError = {
                // for some reason the onStart doesn't call if an error occurs
                currencyTextView?.text = token.name.take(1)
                onError(token)
            },
        )
    }
}

private inline fun ImageView.loadIcon(
    data: Any?,
    placeholderRes: Int,
    crossinline onStart: () -> Unit = {},
    crossinline onSuccess: () -> Unit = {},
    crossinline onError: () -> Unit = {},
) {
    ImageRequest.Builder(context)
        .data(data)
        .placeholder(placeholderRes)
        .error(placeholderRes)
        .fallback(placeholderRes)
        .listener(
            onStart = { onStart() },
            onSuccess = { _, _ -> onSuccess() },
            onError = { _, _ -> onError() },
        )
        .target(imageView = this)
        .build()
        .also(context.imageLoader::enqueue)
}

private fun getTokenIcon(token: Token, blockchain: Blockchain): Any? {
    return when (token.symbol) {
        QCX -> R.drawable.ic_qcx
        VOYR -> R.drawable.ic_voyr
        else -> {
            token.id?.let(::getIconUrl)
                ?: token.getCustomIconUrl()
                ?: IconsUtil.getTokenIconUri(blockchain, token)
                    ?.toString()
        }
    }
}
