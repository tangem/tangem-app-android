package com.tangem.domain.cloudbackup.password

enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG,
    ;

    val isAcceptable: Boolean get() = this == STRONG
}