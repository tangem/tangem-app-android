package com.tangem.domain.wallet.btc

enum class BitcoinNode(val host: String, val port: Int, val proto: String) {
    N_001("electrum.coinop.cc", 50002, "ssl"),
    N_002("dedi.jochen-hoenicke.de", 50002, "ssl"),
    N_003("technetium.network", 50002, "ssl"),
    N_004("electrum.coinucopia.io", 50002, "ssl"),
    N_005("dimon.trimon.de", 50002, "ssl"),
    N_006("btc.gravitech.net", 50002, "ssl"),
    N_007("fn.48.org", 50002, "ssl"),
    N_008("vps.hsmiths.com", 50002, "ssl"),
    N_009("electrum.qtornado.com", 50002, "ssl"),
    N_010("electrum2.eff.ro", 50002, "ssl"),
    N_011("electrum.hsmiths.com", 995, "ssl"),
    N_012("electrum.hsmiths.com", 50002, "ssl"),
    N_013("tardis.bauerj.eu", 50002, "ssl"),
}