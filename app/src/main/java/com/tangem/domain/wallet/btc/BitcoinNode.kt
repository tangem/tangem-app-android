package com.tangem.domain.wallet.btc

enum class BitcoinNode(val host: String, val port: Int, val proto: String) {
    N_001("dedi.jochen-hoenicke.de", 50002, "ssl"),
    N_002("technetium.network", 50002, "ssl"),
    N_003("btc.gravitech.net", 50002, "ssl"),
    N_004("fn.48.org", 50002, "ssl"),
    N_005("vps.hsmiths.com", 50002, "ssl"),
    N_006("electrum.hsmiths.com", 995, "ssl"),
    N_007("electrum.hsmiths.com", 50002, "ssl"),
}