package com.tangem.tap.common.extensions

import android.widget.ImageView
import android.widget.TextView
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
        url != null -> {
            this.load(url)
                .placeholder(R.drawable.shape_circle)
                ?.into(imageView,
                    object : Callback {
                        override fun onError(e: Exception?) {
                            setOfflineCurrencyImage(imageView, textView, token, blockchain)
                        }

                        override fun onSuccess() {
                        }
                    })
        }
        token?.symbol == QCX -> {
            this.load(R.drawable.ic_qcx)?.into(imageView)
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
}

private fun setBlockchainImage(
    imageView: ImageView,
    textView: TextView,
    blockchain: Blockchain
) {
    imageView.setImageResource(blockchain.getIconRes())
    imageView.colorFilter = null
    textView.text = null
}

private fun setTokenImage(
    imageView: ImageView,
    textView: TextView,
    token: Token
) {
    imageView.setImageResource(R.drawable.shape_circle)
    imageView.setColorFilter(token.getColor())
    textView.text = token.symbol.take(1)
}
