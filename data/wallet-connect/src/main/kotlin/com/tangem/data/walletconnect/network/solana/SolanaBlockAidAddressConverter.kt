package com.tangem.data.walletconnect.network.solana

import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.data.walletconnect.utils.WC_TAG
import com.tangem.blockchain.extensions.encodeBase64NoWrap
import com.tangem.utils.converter.Converter
import timber.log.Timber
import javax.inject.Inject

internal class SolanaBlockAidAddressConverter @Inject constructor() : Converter<String, String?> {

    override fun convert(value: String): String? {
        return value.decodeBase58()?.encodeBase64NoWrap() ?: run {
            Timber.tag(WC_TAG).e("Error while converting Solana transaction account address")
            null
        }
    }
}