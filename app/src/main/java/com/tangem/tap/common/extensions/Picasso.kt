package com.tangem.tap.common.extensions

import android.graphics.PorterDuff
import android.net.Uri
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.core.content.ContextCompat
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.IconsUtil
import com.tangem.blockchain.common.Token
import com.tangem.tap.domain.extensions.getCustomIconUrl
import com.tangem.wallet.R

fun Picasso.loadCurrenciesIcon(
    imageView: ImageFilterView,
    textView: TextView,
    token: Token? = null,
    blockchain: Blockchain,
) {

    val url = if (token != null) {
        token.getCustomIconUrl()?.let { Uri.parse(it) } ?: IconsUtil.getTokenIconUri(blockchain, token)
    } else {
        IconsUtil.getBlockchainIconUri(blockchain)
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
                setTokenImage(imageView, textView, token)
            }
            this.load(url)
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
    if (token != null) {
        setTokenImage(imageView, textView, token)
    } else {
        setBlockchainImage(imageView, textView, blockchain)
    }
    if (blockchain.isTestnet()) imageView.tint(R.color.tint)
}

fun ImageView.tint(colorRes: Int) {
//    val color = ContextCompat.getColor(context, colorRes);
//    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(color));
    this.setColorFilter(ContextCompat.getColor(context, colorRes), PorterDuff.Mode.DARKEN);

}

private fun setBlockchainImage(
    imageView: ImageFilterView,
    textView: TextView,
    blockchain: Blockchain,
) {
    imageView.setImageResource(blockchain.getIconRes())
    imageView.colorFilter = null
    textView.text = null
}

private fun setTokenImage(
    imageView: ImageFilterView,
    textView: TextView,
    token: Token,
) {
    imageView.setImageResource(R.drawable.shape_circle)
    if (token.blockchain.isTestnet()) {
        imageView.saturation = 0f
    } else {
        imageView.setColorFilter(token.getColor())
    }
    textView.text = token.symbol.take(1)
}

