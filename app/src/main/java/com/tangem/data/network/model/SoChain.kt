package com.tangem.data.network.model

class SoChain {
    class Request {
        class SendTx{
            var tx_hex: String? = null
        }
    }

    class Response {
        class AddressBalance {
            class Data {
                var network: String? = null
                var address: String? = null
                var confirmed_balance: String? = null
                var unconfirmed_balance: String? = null
                var confirmations: String? = null
            }

            var status: String? = null
            var data: Data? = null
        }

        class TxUnspent {
            class Data {
                class Tx {
                    var txid: String? = null //"9b5c8fbeb1e42bb2a6da40e2eab49c368d1a205707a1ec88aa13f0f2ecdfe944",
                    var output_no: Int? = null // 0,
                    var script_asm: String? = null // "OP_DUP OP_HASH160 8541eb0593bb19c3755198e7d2a71e134da21a97 OP_EQUALVERIFY OP_CHECKSIG",
                    var script_hex: String? = null // "76a9148541eb0593bb19c3755198e7d2a71e134da21a9788ac",
                    var value: String? = null // "11.38404832",
                    var confirmations: Long? = null
                    var time: Long? = null// : 1555509495
                }

                var network: String? = null
                var address: String? = null
                var txs: Array<Tx>? = null
            }

            var status: String? = null
            var data: Data? = null

        }

        class SendTx {
            class Data {
                var network: String? = null
                var txid: String? = null
                var tx_hex: String? = null
            }
            var status: String? = null
            var data: Data? = null
        }

        class GetTx {
            class Data {
                // restricted data
                var network: String? = null
                var address: String? = null
                var txid: String? = null
                var tx_hex: String? = null
            }

            var status: String? = null
            var data: Data? = null
        }

    }
}