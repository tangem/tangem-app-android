package com.tangem.util.extensions

import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_card.util.Util

fun TangemCard.isStart2CoinCard(): Boolean = (Util.bytesToHex(this.cid).startsWith("1"))