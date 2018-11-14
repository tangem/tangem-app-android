package com.tangem.domain

enum class LitecoinNode(val host: String, val port: Int) {
    n1("ltc.rentonisk.com", 50001),
    n2("backup.electrum-ltc.org", 50001),
    n3("node.ispol.sk", 50003),
    n4("electrum-ltc.wilv.in", 50001),
	n5("ltc01.knas.systems", 50003),
	n6("electrumx.nmdps.net", 9433),
	n7("e-3.claudioboxx.com", 50003),
	n8("electrum.ltc.xurious.com", 50001),
	n9("e-1.claudioboxx.com", 50003),
}