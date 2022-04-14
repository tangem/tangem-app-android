package com.tangem.tap.common.extensions

import android.graphics.*
import android.widget.TextView
import androidx.constraintlayout.utils.widget.ImageFilterView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.IconsUtil
import com.tangem.blockchain.common.Token
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.tap.domain.extensions.getCustomIconUrl
import com.tangem.tap.domain.tokens.getIconUrl
import com.tangem.wallet.R

fun Picasso.loadCurrenciesIcon(
    imageView: ImageFilterView,
    textView: TextView,
    token: Token? = null,
    blockchain: Blockchain,
) {

    val url: String? = if (token != null) {
        token.id?.let { getIconUrl(it) } ?: token.getCustomIconUrl()
        ?: IconsUtil.getTokenIconUri(blockchain, token)?.toString()
    } else {
        getIconUrl(blockchain.toNetworkId())
    }

    imageView.setImageDrawable(null)
    imageView.colorFilter = null
    textView.text = null

    when {
        token?.symbol == QCX -> {
            this.load(R.drawable.ic_qcx)?.into(imageView)
        }
        token?.symbol == VOYR -> {
            this.load(R.drawable.ic_voyr)?.into(imageView)
        }
        url != null -> {
            if (token != null) {
                setTokenImage(imageView, textView, token, blockchain)
            }
            this.load(url)
                .transform(RoundedCornersTransform())
                .noPlaceholder()
                ?.into(imageView,
                    object : Callback {
                        override fun onError(e: Exception?) {
                            setOfflineCurrencyImage(imageView, textView, token, blockchain)
                        }

                        override fun onSuccess() {
                            if (token != null) {
                                imageView.colorFilter = null
                                textView.text = null
                            }
                            if (blockchain.isTestnet()) imageView.saturation = 0f
                        }
                    })
        }
        else -> {
            setOfflineCurrencyImage(imageView, textView, token, blockchain)
        }
    }
}

private const val QCX = "QCX"
private const val VOYR = "VOYRME"

private fun setOfflineCurrencyImage(
    imageView: ImageFilterView,
    textView: TextView,
    token: Token?,
    blockchain: Blockchain,
) {
    when (token) {
        null -> setBlockchainImage(imageView, textView, blockchain)
        else -> setTokenImage(imageView, textView, token, blockchain)
    }
}

private fun setBlockchainImage(
    imageView: ImageFilterView,
    textView: TextView,
    blockchain: Blockchain,
) {
    imageView.setImageResource(blockchain.getRoundIconRes())
    imageView.colorFilter = null
    if (blockchain.isTestnet()) imageView.saturation = 0f
    textView.text = null
}

private fun setTokenImage(
    imageView: ImageFilterView,
    textView: TextView,
    token: Token,
    tokenBlockchain: Blockchain
) {
    imageView.setImageResource(R.drawable.shape_circle)
    if (tokenBlockchain.isTestnet()) {
        imageView.saturation = 0f
    } else {
        imageView.setColorFilter(token.getColor())
    }
    textView.text = token.symbol.take(1)
}

private class RoundedCornersTransform : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val size = source.width.coerceAtMost(source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2
        val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
        if (squaredBitmap != source) source.recycle()

        val paint = Paint().apply {
            shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            isAntiAlias = true
        }

        val rectF = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        val radius = size / 8f
        val bitmap = Bitmap.createBitmap(size, size, source.config)
        Canvas(bitmap).drawRoundRect(rectF, radius, radius, paint)
        squaredBitmap.recycle()

        return bitmap
    }

    override fun key(): String = "rounded_corners"
}