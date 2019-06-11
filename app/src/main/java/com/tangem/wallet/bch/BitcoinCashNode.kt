package com.tangem.wallet.bch

enum class BitcoinCashNode(val host: String, val port: Int, val proto: String) {
    N_001("electrumx.hillsideinternet.com", 50002, "ssl"),
//    N_002("dedi.jochen-hoenicke.de", 51002, "ssl"),
//    N_003("crypto.mldlabs.com", 50002, "ssl"),
//    N_004("electroncash.cascharia.com", 50002, "ssl"),
//    N_005("bch.crypto.mldlabs.com", 50002, "ssl"),
    N_006("electron.coinucopia.io", 50002, "ssl"),
    N_007("blackie.c3-soft.com", 50002, "ssl"),
    N_008("electrum.imaginary.cash", 50002, "ssl"),
//    N_009("bitcoincash.quangld.com", 50002, "ssl"),
//    N_010("bch.stitthappens.com", 50002, "ssl"),
}