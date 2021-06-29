package com.tangem.tap.common.extensions

import android.graphics.PorterDuff
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.IconsUtil
import com.tangem.blockchain.common.Token
import com.tangem.wallet.R

fun Picasso.loadCurrenciesIcon(
    imageView: ImageView,
    textView: TextView,
    token: Token? = null,
    blockchain: Blockchain?,
) {
    val blockchain = blockchain ?: Blockchain.Ethereum

    val url = if (token != null) {
        IconsUtil.getTokenIconUri(blockchain, token)
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
                            if (blockchain.isTestnet()) imageView.tint(R.color.tint)
                        }
                    })
        }
        else -> {
            setOfflineCurrencyImage(imageView, textView, token, blockchain)
        }
    }
}

private const val QCX = "QCX"

private fun setOfflineCurrencyImage(
    imageView: ImageView,
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
    imageView: ImageView,
    textView: TextView,
    blockchain: Blockchain,
) {
    imageView.setImageResource(blockchain.getIconRes())
    imageView.colorFilter = null
    textView.text = null
}

private fun setTokenImage(
    imageView: ImageView,
    textView: TextView,
    token: Token,
) {
    imageView.setImageResource(R.drawable.shape_circle)
    imageView.setColorFilter(token.getColor())
    textView.text = token.symbol.take(1)
}
