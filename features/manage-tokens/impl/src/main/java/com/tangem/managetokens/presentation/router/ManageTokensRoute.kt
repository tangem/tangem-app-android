package com.tangem.managetokens.presentation.router

/**
 * Manage Tokens screens
 *
 * @property route route string representation
 *
 */
internal sealed class ManageTokensRoute(val route: String) {

    object ManageTokens : ManageTokensRoute(route = "manage_tokens")

    object AddCustomToken : ManageTokensRoute(route = "manage_tokens/add_custom_token") {
        object Main : ManageTokensRoute(AddCustomToken.route + "/main")
        object ChooseNetwork : ManageTokensRoute(AddCustomToken.route + "/choose_network")
        object ChooseWallet : ManageTokensRoute(AddCustomToken.route + "/choose_wallet")
        object ChooseDerivation : ManageTokensRoute(AddCustomToken.route + "/choose_derivation")
    }
}
