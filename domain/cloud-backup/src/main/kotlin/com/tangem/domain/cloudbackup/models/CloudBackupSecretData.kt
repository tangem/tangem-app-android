package com.tangem.domain.cloudbackup.models


data class CloudBackupSecretData(
    val mnemonic: String,
    val isPassphraseRequired: Boolean,
)