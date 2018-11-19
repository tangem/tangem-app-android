package com.tangem.domain.wallet.btc

enum class BitcoinNode(val host: String, val port: Int, val proto: String) {
    N_001("electrum.anduck.net", 50001, "tcp"),
    N_002("electrum-server.ninja", 50001, "tcp"),
    N_003("btc.cihar.com", 50001, "tcp"),
    N_004("vps.hsmiths.com", 50001, "tcp"),
    N_005("electrum.hsmiths.com", 50001, "tcp"),
    N_006("electrum.vom-stausee.de", 50001, "tcp"),
    N_007("node.ispol.sk", 50001, "tcp"),
    N_008("electrum2.eff.ro", 50001, "tcp"),
    N_009("electrumx.nmdps.net", 50001, "tcp"),
    N_010("kirsche.emzy.de", 50001, "tcp"),
    N_011("electrum.petrkr.net", 50001, "tcp"),
    N_012("electrum.dk", 50001, "tcp"),
    N_013("electrum.anduck.net", 50012, "ssl"),
    N_014("electrum.eff.ro", 50002, "ssl"),
    N_015("vps.hsmiths.com", 50002, "ssl"),
}