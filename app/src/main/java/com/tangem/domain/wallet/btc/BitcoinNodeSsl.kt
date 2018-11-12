package com.tangem.domain.wallet.btc

enum class BitcoinNodeSsl(val host: String, val port: Int) {
    n1("electrum.anduck.net", 50012),
    n2("electrum.eff.ro", 50002),
    n3("vps.hsmiths.com", 50002)
}