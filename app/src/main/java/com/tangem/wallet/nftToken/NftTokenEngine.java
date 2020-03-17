package com.tangem.wallet.nftToken;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;

import com.tangem.data.Blockchain;
import com.tangem.data.network.ServerApiInfura;
import com.tangem.data.network.model.InfuraResponse;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.tasks.SignTask;
import com.tangem.wallet.BTCUtils;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.Keccak256;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;
import com.tangem.wallet.eth.EthData;
import com.tangem.wallet.token.TokenData;

import java.math.BigInteger;

public class NftTokenEngine extends CoinEngine {

    private static final String TAG = NftTokenEngine.class.getSimpleName();
    public TokenData coinData = null;

    public NftTokenEngine(TangemContext ctx) throws Exception {
        super(ctx);
        if (ctx.getCoinData() == null) {
            coinData = new TokenData();
            ctx.setCoinData(coinData);
        } else if (ctx.getCoinData() instanceof TokenData) {
            coinData = (TokenData) ctx.getCoinData();
        } else if (ctx.getCoinData() instanceof EthData) {
            // special case with receive card data substitution from server at the moment
            Bundle B = new Bundle();
            ctx.getCoinData().saveToBundle(B);
            coinData = new TokenData();
            coinData.loadFromBundle(B);
            ctx.setCoinData(coinData);
        } else {
            throw new Exception("Invalid type of Blockchain data for " + this.getClass().getSimpleName());
        }
    }

    public NftTokenEngine() {
        super();
    }

    public Blockchain getBlockchain() {
        return Blockchain.NftToken;
    }

    @Override
    public boolean awaitingConfirmation() {
        return false;
    }


    @Override
    public String getBalanceHTML() {
        if (hasBalanceInfo()) {
            if (isBalanceNotZero()) {
                return "GENUINE";// + getBalanceCurrency();
            } else {
                return "NOT FOUND";
            }
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        return ctx.getCard().getTokenSymbol().substring(4);
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        return new InputFilter[0];
    }

    protected String getContractAddress(TangemCard card) {
        return card.getContractAddress();
    }

    public boolean isNeedCheckNode() {
        return false;
    }

    @Override
    public String getBalanceEquivalent() {
        return null;
    }

    @Override
    public boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        if (!address.startsWith("0x") && !address.startsWith("0X")) {
            return false;
        }

        if (address.length() != 42) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isBalanceNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalanceInInternalUnits() == null) return false;
        return coinData.getBalanceInInternalUnits().notZero();
    }

    @Override
    public String calculateAddress(byte[] pkUncompressed) {
        Keccak256 kec = new Keccak256();
        int lenPk = pkUncompressed.length;
        if (lenPk < 2) {
            throw new IllegalArgumentException("Uncompress public key length is invalid");
        }
        byte[] cleanKey = new byte[lenPk - 1];
        for (int i = 0; i < cleanKey.length; ++i) {
            cleanKey[i] = pkUncompressed[i + 1];
        }
        byte[] r = kec.digest(cleanKey);

        byte[] address = new byte[20];
        for (int i = 0; i < 20; ++i) {
            address[i] = r[i + 12];
        }

        return String.format("0x%s", BTCUtils.toHex(address));
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return null;
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public byte[] convertToByteArray(InternalAmount amount) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public CoinData createCoinData() {
        return new TokenData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return "";
    }

    @Override
    public boolean hasBalanceInfo() {
        return coinData != null && coinData.getBalanceInInternalUnits() != null;
    }


    @Override
    public Uri getWalletExplorerUri() {
        return Uri.parse("https://etherscan.io/token/" + getContractAddress(ctx.getCard()) + "?a=" + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        return Uri.parse(ctx.getCoinData().getWallet());
    }

    @Override
    public boolean isExtractPossible() {
        return false;
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        return false;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amount, Amount fee, Boolean isFeeIncluded) {
        return false;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        if (coinData.getBalanceInInternalUnits() == null) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_no_connection);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_authenticity_not_verified);
            return false;
        }

        if (coinData.isBalanceReceived()) {
            if (isBalanceNotZero()) {
                balanceValidator.setScore(100);
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_in_blockchain);
                balanceValidator.setSecondLine(R.string.empty_string);
            } else {
                balanceValidator.setScore(0);
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_authenticity);
                balanceValidator.setSecondLine(R.string.empty_string);
            }
        }

        return true;
    }

    @Override
    public Amount getBalance() {
        return null;
    }


    @Override
    public String evaluateFeeEquivalent(String fee) {
        return null;
    }

    @Override
    public String getFeeCurrency() {
        return null;
    }

    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        final ServerApiInfura serverApiInfura = new ServerApiInfura();
        // request requestData listener
        ServerApiInfura.ResponseListener responseListener = new ServerApiInfura.ResponseListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                try {
                    if (validateAddress(getContractAddress(ctx.getCard()))) {
                        String balanceCap = infuraResponse.getResult();
                        balanceCap = balanceCap.substring(2);
                        BigInteger l = new BigInteger(balanceCap, 16);
                        coinData.setBalanceInInternalUnits(new InternalAmount(l, ctx.getCard().tokenSymbol));
                        coinData.setBalanceReceived(true);
//                              Log.i("$TAG eth_call", balanceCap)
                    } else {
                        ctx.setError("Smart contract address not defined");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                blockchainRequestsCallbacks.onComplete(!ctx.hasError());
            }

            @Override
            public void onFail(String method, String message) {
                Log.e(TAG, "onFail: " + method + " " + message);
                ctx.setError(message);
                blockchainRequestsCallbacks.onComplete(false);
            }
        };
        serverApiInfura.setResponseListener(responseListener);

        serverApiInfura.requestData(ServerApiInfura.INFURA_ETH_CALL, 67, coinData.getWallet(), getContractAddress(ctx.getCard()), "");
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) {
        ServerApiInfura serverApiInfura = new ServerApiInfura();
        // request requestData eth gasPrice listener
        ServerApiInfura.ResponseListener responseListener = new ServerApiInfura.ResponseListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                String gasPrice = infuraResponse.getResult();
                gasPrice = gasPrice.substring(2);

                // rounding gas price to integer gwei
                BigInteger l = new BigInteger(gasPrice, 16);

                Log.i(TAG, "Infura gas price: " + gasPrice + " (" + l.toString() + ")");
                BigInteger m;
                if (!amount.getCurrency().equals(Blockchain.Ethereum.getCurrency()))
                    m = BigInteger.valueOf(60000);
                else m = BigInteger.valueOf(21000);

                Log.i(TAG, "fee multiplier: " + m.toString());

                InternalAmount weiMinFee = new InternalAmount(l.multiply(m), "wei");
                InternalAmount weiNormalFee = new InternalAmount(l.multiply(BigInteger.valueOf(12)).divide(BigInteger.valueOf(10)).multiply(m), "wei");
                InternalAmount weiMaxFee = new InternalAmount(l.multiply(BigInteger.valueOf(15)).divide(BigInteger.valueOf(10)).multiply(m), "wei");
                Log.i(TAG, "min fee   : " + weiMinFee.toValueString() + " wei");
                Log.i(TAG, "normal fee: " + weiNormalFee.toValueString() + " wei");
                Log.i(TAG, "max fee   : " + weiMaxFee.toValueString() + " wei");

                try {
                    coinData.minFee = convertToAmount(weiMinFee);
                    coinData.normalFee = convertToAmount(weiNormalFee);
                    coinData.maxFee = convertToAmount(weiMaxFee);
                    Log.i(TAG, "min fee   : " + coinData.minFee.toString());
                    Log.i(TAG, "normal fee: " + coinData.normalFee.toString());
                    Log.i(TAG, "max fee   : " + coinData.maxFee.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                blockchainRequestsCallbacks.onComplete(true);
            }

            @Override
            public void onFail(String method, String message) {
                ctx.setError(message);
                blockchainRequestsCallbacks.onComplete(false);
            }
        };
        serverApiInfura.setResponseListener(responseListener);

        serverApiInfura.requestData(ServerApiInfura.INFURA_ETH_GAS_PRICE, 67, coinData.getWallet(), "", "");
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) {
    }

    public boolean isNftToken() {
        return true;
    }


}
