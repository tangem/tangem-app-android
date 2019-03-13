package com.tangem.domain.wallet.ltc

enum class LitecoinNode(val host: String, val port: Int, val proto: String) {
	N_001("backup.electrum-ltc.org", 443, "ssl"),
	N_002("electrum-ltc.petrkr.net", 60002, "ssl"),
	N_003("electrum-ltc.bysh.me", 50002, "ssl"),
	N_004("e-1.claudioboxx.com", 50004, "ssl"),
	N_005("e-2.claudioboxx.com", 50004, "ssl"),
	N_006("electrum.ltc.xurious.com", 50002, "ssl"),
}