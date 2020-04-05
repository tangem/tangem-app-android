package com.tangem.wallet.matic;

import android.net.Uri;
import android.util.Log;

import com.tangem.data.Blockchain;
import com.tangem.data.network.ServerApiMatic;
import com.tangem.data.network.model.InfuraResponse;
import com.tangem.tangem_card.tasks.SignTask;
import com.tangem.wallet.BTCUtils;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.EthTransaction;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;
import com.tangem.wallet.token.TokenEngine;

import java.math.BigInteger;

public class MaticTokenEngine extends TokenEngine {

    private static final String TAG = MaticTokenEngine.class.getSimpleName();

    public MaticTokenEngine(TangemContext ctx) throws Exception {
        super(ctx);
    }

    public MaticTokenEngine() {
        super();
    }

    @Override
    public Blockchain getBlockchain() {
        return ctx.getBlockchain();
    }

    @Override
    protected int getChainIdNum() {
        return EthTransaction.ChainEnum.Matic_Testnet.getValue();
    }

    @Override
    public String getBalanceHTML() {
        if (hasBalanceInfo()) {
            try {
                return " " + convertToAmount(coinData.getBalanceInInternalUnits()).toDescriptionString(getTokenDecimals());
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceEquivalent() {
        return "";
    }

    @Override
    public Uri getWalletExplorerUri() {
        return Uri.parse("https://explorer.testnet2.matic.network/account/" + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        if (ctx.getCard().getDenomination() != null) {
            return Uri.parse(ctx.getCoinData().getWallet());// + "?value=" + mCard.getDenomination() +"e18");
        } else {
            return Uri.parse(ctx.getCoinData().getWallet());
        }
    }

    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.loaded_wallet_error_obtaining_blockchain_data);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.general_wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.loaded_wallet_message_wait);
        } else {
            return true;
        }
        return false;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amount, Amount fee, Boolean isFeeIncluded) {
        return true;
    }

    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
            return constructTransactionToken(feeValue, amountValue, IncFee, targetAddress);
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        final ServerApiMatic serverApiMatic = new ServerApiMatic();
        // request requestData listener
        ServerApiMatic.ResponseListener responseListener = new ServerApiMatic.ResponseListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                switch (method) {
                    case ServerApiMatic.MATIC_ETH_GET_BALANCE: {
                        String balanceCap = infuraResponse.getResult();
                        balanceCap = balanceCap.substring(2);
                        BigInteger l = new BigInteger(balanceCap, 16);
                        coinData.setBalanceReceived(true);
                        coinData.setBalanceAlterInInternalUnits(new CoinEngine.InternalAmount(l, "wei"));
//                        Log.i("$TAG eth_get_balance", balanceCap)
                    }
                    break;

                    case ServerApiMatic.MATIC_ETH_GET_TRANSACTION_COUNT: {
                        String nonce = infuraResponse.getResult();
                        nonce = nonce.substring(2);
                        BigInteger count = new BigInteger(nonce, 16);
                        coinData.setConfirmedTXCount(count);
                        if (serverApiMatic.isRequestsSequenceCompleted()) { //getting balances after checking for pending to avoid showing old balance as verified
                            serverApiMatic.requestData(ServerApiMatic.MATIC_ETH_CALL, 67, coinData.getWallet(), getContractAddress(ctx.getCard()), "");
                            serverApiMatic.requestData(ServerApiMatic.MATIC_ETH_GET_BALANCE, 67, coinData.getWallet(), "", "");
                        }
//                        Log.i("$TAG eth_getTransCount", nonce)
                    }
                    break;

                    case ServerApiMatic.MATIC_ETH_GET_PENDING_COUNT: {
                        String pending = infuraResponse.getResult();
                        pending = pending.substring(2);
                        BigInteger count = new BigInteger(pending, 16);
                        coinData.setUnconfirmedTXCount(count);
                        if (serverApiMatic.isRequestsSequenceCompleted()) { //getting balances after checking for pending to avoid showing old balance as verified
                            serverApiMatic.requestData(ServerApiMatic.MATIC_ETH_CALL, 67, coinData.getWallet(), getContractAddress(ctx.getCard()), "");
                            serverApiMatic.requestData(ServerApiMatic.MATIC_ETH_GET_BALANCE, 67, coinData.getWallet(), "", "");
                        }
//                        Log.i("$TAG eth_getPendingTxCount", pending)
                    }
                    break;
//
                    case ServerApiMatic.MATIC_ETH_CALL: {
                        try {

                            String balanceCap = infuraResponse.getResult();
                            balanceCap = balanceCap.substring(2);
                            BigInteger l = new BigInteger(balanceCap, 16);
                            coinData.setBalanceInInternalUnits(new CoinEngine.InternalAmount(l, ctx.getCard().tokenSymbol));
//                              Log.i("$TAG eth_call", balanceCap)
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                }
                if (serverApiMatic.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }

            @Override
            public void onFail(String method, String message) {
                Log.e(TAG, "onFail: " + method + " " + message);
                ctx.setError(message);
                if (serverApiMatic.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(false);
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }
        };
        serverApiMatic.setResponseListener(responseListener);

        if (validateAddress(getContractAddress(ctx.getCard()))) {
            serverApiMatic.requestData(ServerApiMatic.MATIC_ETH_GET_TRANSACTION_COUNT, 67, coinData.getWallet(), "", "");
            serverApiMatic.requestData(ServerApiMatic.MATIC_ETH_GET_PENDING_COUNT, 67, coinData.getWallet(), "", "");
        } else {
            ctx.setError("Smart contract address not defined");
            blockchainRequestsCallbacks.onComplete(false);
        }
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) {
        coinData.minFee = coinData.normalFee = coinData.maxFee = new Amount(0L, getFeeCurrency());
        blockchainRequestsCallbacks.onComplete(true);
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) {

        String txStr = String.format("0x%s", BTCUtils.toHex(txForSend));

        final ServerApiMatic serverApiMatic = new ServerApiMatic();
        // request requestData listener
        ServerApiMatic.ResponseListener responseListener = new ServerApiMatic.ResponseListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                if (method.equals(ServerApiMatic.MATIC_ETH_SEND_RAW_TRANSACTION)) {
                    if (infuraResponse.getResult()==null || infuraResponse.getResult().isEmpty()) {
                        ctx.setError("Rejected by node: " + infuraResponse.getError());
                        blockchainRequestsCallbacks.onComplete(false);
                    } else {
                        BigInteger nonce = coinData.getConfirmedTXCount();
                        nonce = nonce.add(BigInteger.valueOf(1));
                        coinData.setConfirmedTXCount(nonce);
                        ctx.setError(null);
                        blockchainRequestsCallbacks.onComplete(true);
                    }
                }
            }

            @Override
            public void onFail(String method, String message) {
                if (method.equals(ServerApiMatic.MATIC_ETH_SEND_RAW_TRANSACTION)) {
                    ctx.setError(message);
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }
        };

        serverApiMatic.setResponseListener(responseListener);

        serverApiMatic.requestData(ServerApiMatic.MATIC_ETH_SEND_RAW_TRANSACTION, 67, coinData.getWallet(), "", txStr);

    }

    @Override
    public boolean needMultipleLinesForBalance() {
        return false;
    }

    @Override
    public boolean allowSelectFeeLevel() {
        return false;
    }

    @Override
    public boolean allowSelectFeeInclusion() {
        return false;
    }
}
