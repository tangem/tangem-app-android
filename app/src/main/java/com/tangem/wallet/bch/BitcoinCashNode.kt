package com.tangem.wallet.bch

enum class BitcoinCashNode(val host: String, val port: Int, val proto: String) {
    N_001("electrumx.hillsideinternet.com", 50002, "ssl"),
    N_002("blackie.c3-soft.com", 50002, "ssl"),
    N_003("electrum.imaginary.cash", 50002, "ssl"),
}