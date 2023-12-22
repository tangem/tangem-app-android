package com.tangem.managetokens.presentation.router

/**
 * Manage Tokens screens
 *
 * @property route route string representation
 *
 */
internal sealed class ManageTokensRoute(val route: String) {

    object ManageTokens : ManageTokensRoute(route = "manage_tokens")

    object CustomTokens : ManageTokensRoute(route = "manage_tokens/custom_tokens") {
        object Main : ManageTokensRoute(CustomTokens.route + "/main")
        object ChooseNetwork : ManageTokensRoute(CustomTokens.route + "/choose_network")
        object ChooseWallet : ManageTokensRoute(CustomTokens.route + "/choose_wallet")
        object ChooseDerivation : ManageTokensRoute(CustomTokens.route + "/choose_derivation")
    }
}