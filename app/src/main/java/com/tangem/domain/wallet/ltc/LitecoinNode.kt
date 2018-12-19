package com.tangem.domain.wallet.ltc

enum class LitecoinNode(val host: String, val port: Int, val proto: String) {
	N_001("node.ispol.sk", 50004, "ssl"),
	N_002("electrum-ltc.klippb.org", 50002, "ssl"),
	N_003("backup.electrum-ltc.org", 443, "ssl"),
	N_004("electrum-ltc.petrkr.net", 60002, "ssl"),
	N_005("technetium.network", 50003, "ssl"),
	N_006("167.99.146.166", 50002, "ssl"),
	N_007("electrum-ltc.bysh.me", 50002, "ssl"),
	N_008("e-3.claudioboxx.com", 50004, "ssl"),
	N_009("electrum-ltc.wilv.in", 50002, "ssl"),
	N_010("ltc.rentonisk.com", 50002, "ssl"),
	N_011("e-1.claudioboxx.com", 50004, "ssl"),
	N_012("electrum.ltc.xurious.com", 50002, "ssl"),
	N_013("ltc01.knas.systems", 50004, "ssl"),
}