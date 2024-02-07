package com.tangem.lib.visa;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Bytes4;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Flowable;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.5.0.
 */
@SuppressWarnings("rawtypes")
class TangemBridgeProcessor extends Contract {
    public static final String BINARY = "608060405262278d0060085534801562000017575f80fd5b5060405162002457380380620024578339810160408190526200003a91620001fa565b600580546001600160a01b038088166001600160a01b031992831617909255600680548784169083161790556002805486841690831617905560038054928516929091169190911790556009819055620000955f33620000a1565b5050505050506200025d565b5f80620000af8484620000dc565b90508015620000d3575f848152600160205260409020620000d1908462000187565b505b90505b92915050565b5f828152602081815260408083206001600160a01b038516845290915281205460ff166200017f575f838152602081815260408083206001600160a01b03861684529091529020805460ff19166001179055620001363390565b6001600160a01b0316826001600160a01b0316847f2f8788117e7eff1d82e926ec794901d17c78024a50270940304540a733656f0d60405160405180910390a4506001620000d6565b505f620000d6565b5f620000d3836001600160a01b0384165f8181526001830160205260408120546200017f57508154600181810184555f848152602080822090930184905584548482528286019093526040902091909155620000d6565b80516001600160a01b0381168114620001f5575f80fd5b919050565b5f805f805f60a086880312156200020f575f80fd5b6200021a86620001de565b94506200022a60208701620001de565b93506200023a60408701620001de565b92506200024a60608701620001de565b9150608086015190509295509295909350565b6121ec806200026b5f395ff3fe608060405234801561000f575f80fd5b506004361061024a575f3560e01c806391792d5b11610140578063c904ea39116100bf578063d6d32b2a11610084578063d6d32b2a14610551578063da90f3a014610564578063e24fd1f114610577578063e452f20d1461058a578063e96ede8e1461059d578063fe2208aa146105b0575f80fd5b8063c904ea39146104fc578063ca15c8731461050f578063cb37f3b214610522578063d47ae89c14610535578063d547741f1461053e575f80fd5b8063a217fddf11610105578063a217fddf14610495578063b6878ac91461049c578063b77c6d35146104af578063be4d1851146104c2578063c6b273e4146104d5575f80fd5b806391792d5b1461041757806391d14854146104205780639353ce4c1461043357806396bc563d1461045a578063978975591461046e575f80fd5b80634413e098116101cc57806373d3467c1161019157806373d3467c146103b85780637f2fadb0146103cb5780638072a022146103de57806383af133d146103f15780639010d07c14610404575f80fd5b80634413e098146103445780635673795114610357578063601c60651461036b57806365ebf99a1461037e57806370e4306e14610391575f80fd5b80632f2ff15d116102125780632f2ff15d146102cc5780633013ce29146102df57806336568abe1461030a57806337de81061461031d5780633d409a8614610330575f80fd5b806301ffc9a71461024e5780630f1071be1461027657806311dce7711461028d578063248a9ca3146102a25780632cc32641146102c4575b5f80fd5b61026161025c366004611c84565b6105c3565b60405190151581526020015b60405180910390f35b61027f60085481565b60405190815260200161026d565b6102a061029b366004611cbf565b6105ed565b005b61027f6102b0366004611cda565b5f9081526020819052604090206001015490565b6102a061065a565b6102a06102da366004611cf1565b6106d2565b6003546102f2906001600160a01b031681565b6040516001600160a01b03909116815260200161026d565b6102a0610318366004611cf1565b6106fc565b6102a061032b366004611cda565b610734565b61027f5f8051602061216e83398151915281565b6102a0610352366004611d1f565b610780565b61027f5f805160206120f983398151915281565b6102a0610379366004611d49565b610847565b6102a061038c366004611cbf565b610a6a565b61027f7f6ac2c8b5322a929d300af17766189693ff6db6939ffe690430ad4720baf47d1c81565b6102a06103c6366004611d88565b610acf565b6102a06103d9366004611d1f565b610bdc565b6102a06103ec366004611cda565b610cb1565b6102a06103ff366004611d1f565b610d3b565b6102f2610412366004611e06565b610df5565b61027f60075481565b61026161042e366004611cf1565b610e13565b61027f7f79af5a85184b45263c1b3721b9c8eb1becd98e55ade5a61da0fed18b90b8325681565b61027f5f8051602061204983398151915281565b61027f7f581be84b4822abe392a3bcac0d4d656e4cd57faf2e321a32108215cdd4a8365681565b61027f5f81565b6102a06104aa366004611e26565b610e3b565b6002546102f2906001600160a01b031681565b6006546102f2906001600160a01b031681565b61027f7f56d36dba8e2d5c88fc1038bd95811d491e67c9a66c6848c9d4c9309b9587e06481565b6102a061050a366004611e5e565b610f3f565b61027f61051d366004611cda565b611030565b6005546102f2906001600160a01b031681565b61027f60095481565b6102a061054c366004611cf1565b611046565b6102a061055f366004611cda565b61106a565b6102a0610572366004611d1f565b6110f3565b6102a0610585366004611ea5565b611357565b6102f2610598366004611cbf565b611495565b6102a06105ab366004611d49565b6114f3565b6102a06105be366004611d49565b6115d6565b5f6001600160e01b03198216635a05180f60e01b14806105e757506105e7826116a1565b92915050565b5f80516020612049833981519152610604816116d5565b600680546001600160a01b0319166001600160a01b0384169081179091556040519081527f68c7b435e0ad7bfb2140fab0735300efbda206f62906049e0b35763cef67cc5e906020015b60405180910390a15050565b600a543390610668826116e2565b5f811160405180606001604052806029815260200161218e60299139906106ab5760405162461bcd60e51b81526004016106a29190611ef3565b60405180910390fd5b505f600a556006546003546106ce916001600160a01b0391821691168484611785565b5050565b5f828152602081905260409020600101546106ec816116d5565b6106f683836117df565b50505050565b6001600160a01b03811633146107255760405163334bd91960e11b815260040160405180910390fd5b61072f8282611812565b505050565b5f8051602061204983398151915261074b816116d5565b60078290556040518281527fb5aa183eb20407e22587bcd13d5c82a85835a4bd60e2a13d7c7efee3c2e9ed489060200161064e565b7f581be84b4822abe392a3bcac0d4d656e4cd57faf2e321a32108215cdd4a836566107aa816116d5565b60405163b9603bdf60e01b8152600481018390526001600160a01b0384169063b9603bdf906024015f604051808303815f87803b1580156107e9575f80fd5b505af11580156107fb573d5f803e3d5ffd5b50505050826001600160a01b03167fc8818db5b3e4e986e2f21e002090b1513e17c05a7a326c3737af624bf6ef78c48360405161083a91815260200190565b60405180910390a2505050565b5f805160206120f983398151915261085e816116d5565b60408051808201909152601b81527f353130307c436f6d6d6f6e3a20616d6f756e74206973207a65726f00000000006020820152826108b05760405162461bcd60e51b81526004016106a29190611ef3565b505f6108bb85611495565b6003546040516370a0823160e01b81526001600160a01b0380841660048301529293505f92909116906370a0823190602401602060405180830381865afa158015610908573d5f803e3d5ffd5b505050506040513d601f19601f8201168201806040525081019061092c9190611f25565b9050838110156040518060400160405280601f81526020017f353130317c436f6d6d6f6e3a20696e73756666696369656e742066756e647300815250906109865760405162461bcd60e51b81526004016106a29190611ef3565b50600a84905560405163f89db45760e01b8152600481018690526001600160a01b0383169063f89db457906024015f604051808303815f87803b1580156109cb575f80fd5b505af11580156109dd573d5f803e3d5ffd5b50505050600a545f146040518060600160405280602e8152602001612119602e913990610a1d5760405162461bcd60e51b81526004016106a29190611ef3565b5084866001600160a01b03167fa5411d116afcb8415f0019792cccf595f94852ba82886dacbf756d9c3ce85f3f86604051610a5a91815260200190565b60405180910390a3505050505050565b5f80516020612049833981519152610a81816116d5565b600580546001600160a01b0319166001600160a01b0384169081179091556040519081527fccbdeb71dc680b2f0fd85f93e7ef0f70eda32a12b918c9715be07bb646360e599060200161064e565b5f8051602061216e833981519152610ae6816116d5565b5f610af088611495565b90505f60075487610b019190611f50565b6040516334284dfb60e01b8152600481018a9052602481018290526001600160801b03198816604482015261ffff8716606482015285151560848201529091506001600160a01b038316906334284dfb9060a4015f604051808303815f87803b158015610b6c575f80fd5b505af1158015610b7e573d5f803e3d5ffd5b5050505087896001600160a01b03167f92c1dbadbb7b791052341ce7c185ca37861e432438355dc3e1642b7c1287609a89600754604051610bc9929190918252602082015260400190565b60405180910390a3505050505050505050565b7f79af5a85184b45263c1b3721b9c8eb1becd98e55ade5a61da0fed18b90b83256610c06816116d5565b5f610c1084611495565b604051636df6c7d560e11b8152600481018590529091506001600160a01b0382169063dbed8faa906024015f604051808303815f87803b158015610c52575f80fd5b505af1158015610c64573d5f803e3d5ffd5b50505050836001600160a01b03167f466723bb873015d5baf515fce6b0c8df2cb154adfd8badb6bc77a620a6ef676184604051610ca391815260200190565b60405180910390a250505050565b5f80516020612049833981519152610cc8816116d5565b6283d60082106040518060600160405280602a815260200161201f602a913990610d055760405162461bcd60e51b81526004016106a29190611ef3565b5060088290556040518281527fdc10143650bb79cd7a92cdf792545dcc2c3b0a719bebb3d83a8a21bb8fbfc3d69060200161064e565b7f581be84b4822abe392a3bcac0d4d656e4cd57faf2e321a32108215cdd4a83656610d65816116d5565b6040516306b2a38360e41b8152600481018390526001600160a01b03841690636b2a3830906024015f604051808303815f87803b158015610da4575f80fd5b505af1158015610db6573d5f803e3d5ffd5b50505050826001600160a01b03167fcc1685e553848099cad0e11271e780371c44b5ed92a54552fbedf9f1b6b9f2478360405161083a91815260200190565b5f828152600160205260408120610e0c908361183d565b9392505050565b5f918252602082815260408084206001600160a01b0393909316845291905290205460ff1690565b7f6ac2c8b5322a929d300af17766189693ff6db6939ffe690430ad4720baf47d1c610e65816116d5565b5f610e6f86611495565b90505f60075484610e809190611f50565b6040516304e7595f60e21b81526004810188905260248101879052604481018290529091506001600160a01b0383169063139d657c906064015f604051808303815f87803b158015610ed0575f80fd5b505af1158015610ee2573d5f803e3d5ffd5b505050508486886001600160a01b03167fcb06dfb173e98cd60a8931644d288167e193c33d81c1019b9f9aab36b13fbc0387600754604051610f2e929190918252602082015260400190565b60405180910390a450505050505050565b5f8051602061216e833981519152610f56816116d5565b5f610f6086611495565b90505f60075485610f719190611f50565b6040516320199e7960e21b8152600481018890526024810182905285151560448201529091506001600160a01b0383169063806679e4906064015f604051808303815f87803b158015610fc2575f80fd5b505af1158015610fd4573d5f803e3d5ffd5b5050505085876001600160a01b03167f92c1dbadbb7b791052341ce7c185ca37861e432438355dc3e1642b7c1287609a8760075460405161101f929190918252602082015260400190565b60405180910390a350505050505050565b5f8181526001602052604081206105e790611848565b5f82815260208190526040902060010154611060816116d5565b6106f68383611812565b5f80516020612049833981519152611081816116d5565b610e10821060405180606001604052806027815260200161214760279139906110bd5760405162461bcd60e51b81526004016106a29190611ef3565b5060098290556040518281527f7f63f876249f25f7856e620802edd09af38a83c9c6040fcdb3717915403083119060200161064e565b5f805160206120f983398151915261110a816116d5565b5f61111484611495565b604051631902cad960e31b8152600481018590529091505f906001600160a01b0383169063c81656c890602401602060405180830381865afa15801561115c573d5f803e3d5ffd5b505050506040513d601f19601f820116820180604052508101906111809190611f25565b6003546040516370a0823160e01b81526001600160a01b0385811660048301529293505f92909116906370a0823190602401602060405180830381865afa1580156111cd573d5f803e3d5ffd5b505050506040513d601f19601f820116820180604052508101906111f19190611f25565b90505f82116040518060600160405280602781526020016120d2602791399061122d5760405162461bcd60e51b81526004016106a29190611ef3565b5060408051808201909152601f81527f353130317c436f6d6d6f6e3a20696e73756666696369656e742066756e6473006020820152828210156112835760405162461bcd60e51b81526004016106a29190611ef3565b50600a82905560405163e3dbffd560e01b8152600481018690526001600160a01b0384169063e3dbffd5906024015f604051808303815f87803b1580156112c8575f80fd5b505af11580156112da573d5f803e3d5ffd5b50505050600a545f146040518060600160405280602e8152602001612119602e91399061131a5760405162461bcd60e51b81526004016106a29190611ef3565b5084866001600160a01b03167fdfab01fc691e8b69fca482c65142d1bcfeafeb9eb6e9fd5220867d7196d9476e84604051610a5a91815260200190565b7f56d36dba8e2d5c88fc1038bd95811d491e67c9a66c6848c9d4c9309b9587e064611381816116d5565b61138a826116e2565b306001600160a01b0316826001600160a01b031663ce1b1d436040518163ffffffff1660e01b8152600401602060405180830381865afa1580156113d0573d5f803e3d5ffd5b505050506040513d601f19601f820116820180604052508101906113f49190611f63565b6001600160a01b0316146040518060600160405280603281526020016120a060329139906114355760405162461bcd60e51b81526004016106a29190611ef3565b506001600160a01b038381165f8181526004602090815260409182902080546001600160a01b0319169487169485179055905192835290917f0c1464e8c817204497a2076f3b466fe64093e83033c83303b0cb44001cd9b6b5910161083a565b6001600160a01b038082165f90815260046020908152604080832054815160608101909252602f8083529394169283151592611ff090830139906114ec5760405162461bcd60e51b81526004016106a29190611ef3565b5092915050565b5f8051602061216e83398151915261150a816116d5565b5f61151485611495565b90505f831561152557600754611527565b5f5b90505f6115348286611f50565b60405163e4d7ddb960e01b815260048101889052602481018290529091506001600160a01b0384169063e4d7ddb9906044015f604051808303815f87803b15801561157d575f80fd5b505af115801561158f573d5f803e3d5ffd5b505060408051888152602081018690528993506001600160a01b038b1692507f8ba3ad1bd4ff5c3d75861d56df66b7942244d700f4d4cb67df693bac78e805a2910161101f565b5f805160206120f98339815191526115ed816116d5565b5f6115f785611495565b60405163456575e560e01b815260048101869052602481018590529091506001600160a01b0382169063456575e5906044015f604051808303815f87803b158015611640575f80fd5b505af1158015611652573d5f803e3d5ffd5b5050505083856001600160a01b03167f6f60b80b8dcecc3741c03485554a9a35ced96ebba66405fd324caf12a16261da8560405161169291815260200190565b60405180910390a35050505050565b5f6001600160e01b03198216637965db0b60e01b14806105e757506301ffc9a760e01b6001600160e01b03198316146105e7565b6116df8133611851565b50565b6002546040516385bb392360e01b81526001600160a01b038381166004830152909116906385bb392390602401602060405180830381865afa15801561172a573d5f803e3d5ffd5b505050506040513d601f19601f8201168201806040525081019061174e9190611f7e565b60405180606001604052806037815260200161206960379139906106ce5760405162461bcd60e51b81526004016106a29190611ef3565b604080516001600160a01b0385811660248301528416604482015260648082018490528251808303909101815260849091019091526020810180516001600160e01b03166323b872dd60e01b1790526106f690859061188a565b5f806117eb84846118eb565b90508015610e0c575f84815260016020526040902061180a908461197a565b509392505050565b5f8061181e848461198e565b90508015610e0c575f84815260016020526040902061180a90846119f7565b5f610e0c8383611a0b565b5f6105e7825490565b61185b8282610e13565b6106ce5760405163e2517d3f60e01b81526001600160a01b0382166004820152602481018390526044016106a2565b5f61189e6001600160a01b03841683611a31565b905080515f141580156118c25750808060200190518101906118c09190611f7e565b155b1561072f57604051635274afe760e01b81526001600160a01b03841660048201526024016106a2565b5f6118f68383610e13565b611973575f838152602081815260408083206001600160a01b03861684529091529020805460ff1916600117905561192b3390565b6001600160a01b0316826001600160a01b0316847f2f8788117e7eff1d82e926ec794901d17c78024a50270940304540a733656f0d60405160405180910390a45060016105e7565b505f6105e7565b5f610e0c836001600160a01b038416611a3e565b5f6119998383610e13565b15611973575f838152602081815260408083206001600160a01b0386168085529252808320805460ff1916905551339286917ff6391f5c32d9c69d2a47ea670b442974b53935d1edc7fd64eb21e047a839171b9190a45060016105e7565b5f610e0c836001600160a01b038416611a83565b5f825f018281548110611a2057611a20611f99565b905f5260205f200154905092915050565b6060610e0c83835f611b66565b5f81815260018301602052604081205461197357508154600181810184555f8481526020808220909301849055845484825282860190935260409020919091556105e7565b5f8181526001830160205260408120548015611b5d575f611aa5600183611fad565b85549091505f90611ab890600190611fad565b9050808214611b17575f865f018281548110611ad657611ad6611f99565b905f5260205f200154905080875f018481548110611af657611af6611f99565b5f918252602080832090910192909255918252600188019052604090208390555b8554869080611b2857611b28611fc0565b600190038181905f5260205f20015f90559055856001015f8681526020019081526020015f205f9055600193505050506105e7565b5f9150506105e7565b606081471015611b8b5760405163cd78605960e01b81523060048201526024016106a2565b5f80856001600160a01b03168486604051611ba69190611fd4565b5f6040518083038185875af1925050503d805f8114611be0576040519150601f19603f3d011682016040523d82523d5f602084013e611be5565b606091505b5091509150611bf5868383611bff565b9695505050505050565b606082611c1457611c0f82611c5b565b610e0c565b8151158015611c2b57506001600160a01b0384163b155b15611c5457604051639996b31560e01b81526001600160a01b03851660048201526024016106a2565b5080610e0c565b805115611c6b5780518082602001fd5b604051630a12f52160e11b815260040160405180910390fd5b5f60208284031215611c94575f80fd5b81356001600160e01b031981168114610e0c575f80fd5b6001600160a01b03811681146116df575f80fd5b5f60208284031215611ccf575f80fd5b8135610e0c81611cab565b5f60208284031215611cea575f80fd5b5035919050565b5f8060408385031215611d02575f80fd5b823591506020830135611d1481611cab565b809150509250929050565b5f8060408385031215611d30575f80fd5b8235611d3b81611cab565b946020939093013593505050565b5f805f60608486031215611d5b575f80fd5b8335611d6681611cab565b95602085013595506040909401359392505050565b80151581146116df575f80fd5b5f805f805f8060c08789031215611d9d575f80fd5b8635611da881611cab565b9550602087013594506040870135935060608701356001600160801b031981168114611dd2575f80fd5b9250608087013561ffff81168114611de8575f80fd5b915060a0870135611df881611d7b565b809150509295509295509295565b5f8060408385031215611e17575f80fd5b50508035926020909101359150565b5f805f8060808587031215611e39575f80fd5b8435611e4481611cab565b966020860135965060408601359560600135945092505050565b5f805f8060808587031215611e71575f80fd5b8435611e7c81611cab565b935060208501359250604085013591506060850135611e9a81611d7b565b939692955090935050565b5f8060408385031215611eb6575f80fd5b8235611ec181611cab565b91506020830135611d1481611cab565b5f5b83811015611eeb578181015183820152602001611ed3565b50505f910152565b602081525f8251806020840152611f11816040850160208701611ed1565b601f01601f19169190910160400192915050565b5f60208284031215611f35575f80fd5b5051919050565b634e487b7160e01b5f52601160045260245ffd5b808201808211156105e7576105e7611f3c565b5f60208284031215611f73575f80fd5b8151610e0c81611cab565b5f60208284031215611f8e575f80fd5b8151610e0c81611d7b565b634e487b7160e01b5f52603260045260245ffd5b818103818111156105e7576105e7611f3c565b634e487b7160e01b5f52603160045260245ffd5b5f8251611fe5818460208701611ed1565b919091019291505056fe353230327c50726f636573736f723a206e6f207061796d656e74206163636f756e7420666f72207468652063617264353231307c50726f636573736f723a20736574746c656d656e7420706572696f6420746f6f206c6f6e67fa658e47c75a6eb0e27156b2f72e1b295c98b53e8a1d238d7ca6ea6345b34880353230307c50726f636573736f723a207061796d656e74206163636f756e74206e6f74206465706c6f79656420627920666163746f7279353230317c50726f636573736f723a207061796d656e74206163636f756e742070726f636573736f72206d69736d61746368353235307c50726f636573736f723a20726566756e64207265636f7264206e6f7420666f756e64d77cc12a543481a2b3ef8fd055979569715a10db9879cd9e664395eca3a54dff353235327c50726f636573736f723a20726566756e6420746f2070726f63657373207761736e2774207265736574353231317c50726f636573736f723a2073656375726974792064656c617920746f6f206c6f6e67a567244cad934c87b7f7e15b15dd0e75f95a76afd1916f8eab4a391410d6f278353235317c50726f636573736f723a20726566756e6420746f2070726f63657373206973207a65726fa2646970667358221220abcbacdb9048a2744f99cdd5031db80dd0390bf503ccade142542ce178f0668364736f6c63430008160033";

    public static final String FUNC_AUTHORIZATION_PROCESSOR_ROLE = "AUTHORIZATION_PROCESSOR_ROLE";

    public static final String FUNC_BALANCE_VERIFIER_ROLE = "BALANCE_VERIFIER_ROLE";

    public static final String FUNC_DEBT_PROCESSOR_ROLE = "DEBT_PROCESSOR_ROLE";

    public static final String FUNC_DEFAULT_ADMIN_ROLE = "DEFAULT_ADMIN_ROLE";

    public static final String FUNC_PAYMENT_ACCOUNT_SETTER = "PAYMENT_ACCOUNT_SETTER";

    public static final String FUNC_PROPERTY_SETTER_ROLE = "PROPERTY_SETTER_ROLE";

    public static final String FUNC_REFUND_PROCESSOR_ROLE = "REFUND_PROCESSOR_ROLE";

    public static final String FUNC_SETTLEMENT_PROCESSOR_ROLE = "SETTLEMENT_PROCESSOR_ROLE";

    public static final String FUNC_FIXEDFEE = "fixedFee";

    public static final String FUNC_GETPAYMENTACCOUNT = "getPaymentAccount";

    public static final String FUNC_GETROLEADMIN = "getRoleAdmin";

    public static final String FUNC_GETROLEMEMBER = "getRoleMember";

    public static final String FUNC_GETROLEMEMBERCOUNT = "getRoleMemberCount";

    public static final String FUNC_GRANTROLE = "grantRole";

    public static final String FUNC_HASROLE = "hasRole";

    public static final String FUNC_INCREASEVERIFIEDBALANCEFOR = "increaseVerifiedBalanceFor";

    public static final String FUNC_PAYMENTACCOUNTFACTORY = "paymentAccountFactory";

    public static final String FUNC_PAYMENTRECEIVER = "paymentReceiver";

    public static final String FUNC_PAYMENTTOKEN = "paymentToken";

    public static final String FUNC_PROCESSAUTHORIZATION = "processAuthorization";

    public static final String FUNC_PROCESSAUTHORIZATIONCHANGE = "processAuthorizationChange";

    public static final String FUNC_PROCESSAUTHORIZATIONNOOTP = "processAuthorizationNoOtp";

    public static final String FUNC_PROCESSPENDINGREFUND = "processPendingRefund";

    public static final String FUNC_PROCESSREFUND = "processRefund";

    public static final String FUNC_PROCESSREFUNDCALLBACK = "processRefundCallback";

    public static final String FUNC_PROCESSSETTLEMENT = "processSettlement";

    public static final String FUNC_REFUNDACCOUNT = "refundAccount";

    public static final String FUNC_RENOUNCEROLE = "renounceRole";

    public static final String FUNC_REVOKEROLE = "revokeRole";

    public static final String FUNC_SAVEPENDINGREFUND = "savePendingRefund";

    public static final String FUNC_SECURITYDELAY = "securityDelay";

    public static final String FUNC_SETFIXEDFEE = "setFixedFee";

    public static final String FUNC_SETPAYMENTACCOUNT = "setPaymentAccount";

    public static final String FUNC_SETPAYMENTRECEIVER = "setPaymentReceiver";

    public static final String FUNC_SETREFUNDACCOUNT = "setRefundAccount";

    public static final String FUNC_SETSECURITYDELAY = "setSecurityDelay";

    public static final String FUNC_SETSETTLEMENTPERIOD = "setSettlementPeriod";

    public static final String FUNC_SETVERIFIEDBALANCEFOR = "setVerifiedBalanceFor";

    public static final String FUNC_SETTLEMENTPERIOD = "settlementPeriod";

    public static final String FUNC_SUPPORTSINTERFACE = "supportsInterface";

    public static final String FUNC_WRITEOFFDEBT = "writeOffDebt";

    public static final Event AUTHORIZATIONCHANGEPROCESSED_EVENT = new Event("AuthorizationChangeProcessed",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Uint256>(true) {
            }, new TypeReference<Uint256>() {
            }, new TypeReference<Uint256>() {
            }));

    public static final Event AUTHORIZATIONPROCESSED_EVENT = new Event("AuthorizationProcessed",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Uint256>(true) {
            }, new TypeReference<Uint256>() {
            }, new TypeReference<Uint256>() {
            }));

    public static final Event DEBTWRITEOFFPROCESSED_EVENT = new Event("DebtWriteOffProcessed",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Uint256>() {
            }));

    public static final Event FIXEDFEESET_EVENT = new Event("FixedFeeSet",
            List.of(new TypeReference<Uint256>() {
            }));

    public static final Event PAYMENTACCOUNTSET_EVENT = new Event("PaymentAccountSet",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Address>() {
            }));

    public static final Event PAYMENTRECEIVERSET_EVENT = new Event("PaymentReceiverSet",
            List.of(new TypeReference<Address>() {
            }));

    public static final Event PENDINGREFUNDPROCESSED_EVENT = new Event("PendingRefundProcessed",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Uint256>(true) {
            }, new TypeReference<Uint256>() {
            }));

    public static final Event REFUNDACCOUNTSET_EVENT = new Event("RefundAccountSet",
            List.of(new TypeReference<Address>() {
            }));

    public static final Event REFUNDPROCESSED_EVENT = new Event("RefundProcessed",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Uint256>(true) {
            }, new TypeReference<Uint256>() {
            }));

    public static final Event ROLEADMINCHANGED_EVENT = new Event("RoleAdminChanged",
            Arrays.asList(new TypeReference<Bytes32>(true) {
            }, new TypeReference<Bytes32>(true) {
            }, new TypeReference<Bytes32>(true) {
            }));

    public static final Event ROLEGRANTED_EVENT = new Event("RoleGranted",
            Arrays.asList(new TypeReference<Bytes32>(true) {
            }, new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }));

    public static final Event ROLEREVOKED_EVENT = new Event("RoleRevoked",
            Arrays.asList(new TypeReference<Bytes32>(true) {
            }, new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }));

    public static final Event SAVEREFUNDPROCESSED_EVENT = new Event("SaveRefundProcessed",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Uint256>(true) {
            }, new TypeReference<Uint256>() {
            }));

    public static final Event SECURITYDELAYSET_EVENT = new Event("SecurityDelaySet",
            List.of(new TypeReference<Uint256>() {
            }));

    public static final Event SETTLEMENTPERIODSET_EVENT = new Event("SettlementPeriodSet",
            List.of(new TypeReference<Uint256>() {
            }));

    public static final Event SETTLEMENTPROCESSED_EVENT = new Event("SettlementProcessed",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Uint256>(true) {
            }, new TypeReference<Uint256>(true) {
            }, new TypeReference<Uint256>() {
            }, new TypeReference<Uint256>() {
            }));

    public static final Event VERIFIEDBALANCEINCREASEDFOR_EVENT = new Event("VerifiedBalanceIncreasedFor",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Uint256>() {
            }));

    public static final Event VERIFIEDBALANCESETFOR_EVENT = new Event("VerifiedBalanceSetFor",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Uint256>() {
            }));

    @Deprecated
    protected TangemBridgeProcessor(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TangemBridgeProcessor(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TangemBridgeProcessor(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TangemBridgeProcessor(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<AuthorizationChangeProcessedEventResponse> getAuthorizationChangeProcessedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(AUTHORIZATIONCHANGEPROCESSED_EVENT, transactionReceipt);
        ArrayList<AuthorizationChangeProcessedEventResponse> responses = new ArrayList<AuthorizationChangeProcessedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            AuthorizationChangeProcessedEventResponse typedResponse = new AuthorizationChangeProcessedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.fee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AuthorizationChangeProcessedEventResponse getAuthorizationChangeProcessedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(AUTHORIZATIONCHANGEPROCESSED_EVENT, log);
        AuthorizationChangeProcessedEventResponse typedResponse = new AuthorizationChangeProcessedEventResponse();
        typedResponse.log = log;
        typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.fee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public static List<AuthorizationProcessedEventResponse> getAuthorizationProcessedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(AUTHORIZATIONPROCESSED_EVENT, transactionReceipt);
        ArrayList<AuthorizationProcessedEventResponse> responses = new ArrayList<AuthorizationProcessedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            AuthorizationProcessedEventResponse typedResponse = new AuthorizationProcessedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.fee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static List<DebtWriteOffProcessedEventResponse> getDebtWriteOffProcessedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(DEBTWRITEOFFPROCESSED_EVENT, transactionReceipt);
        ArrayList<DebtWriteOffProcessedEventResponse> responses = new ArrayList<DebtWriteOffProcessedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            DebtWriteOffProcessedEventResponse typedResponse = new DebtWriteOffProcessedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static List<FixedFeeSetEventResponse> getFixedFeeSetEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(FIXEDFEESET_EVENT, transactionReceipt);
        ArrayList<FixedFeeSetEventResponse> responses = new ArrayList<FixedFeeSetEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            FixedFeeSetEventResponse typedResponse = new FixedFeeSetEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.fixedFee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AuthorizationProcessedEventResponse getAuthorizationProcessedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(AUTHORIZATIONPROCESSED_EVENT, log);
        AuthorizationProcessedEventResponse typedResponse = new AuthorizationProcessedEventResponse();
        typedResponse.log = log;
        typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.fee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public static List<PaymentAccountSetEventResponse> getPaymentAccountSetEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTACCOUNTSET_EVENT, transactionReceipt);
        ArrayList<PaymentAccountSetEventResponse> responses = new ArrayList<PaymentAccountSetEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            PaymentAccountSetEventResponse typedResponse = new PaymentAccountSetEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.paymentAccount = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static List<PaymentReceiverSetEventResponse> getPaymentReceiverSetEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTRECEIVERSET_EVENT, transactionReceipt);
        ArrayList<PaymentReceiverSetEventResponse> responses = new ArrayList<PaymentReceiverSetEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            PaymentReceiverSetEventResponse typedResponse = new PaymentReceiverSetEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentReceiver = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static List<PendingRefundProcessedEventResponse> getPendingRefundProcessedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PENDINGREFUNDPROCESSED_EVENT, transactionReceipt);
        ArrayList<PendingRefundProcessedEventResponse> responses = new ArrayList<PendingRefundProcessedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            PendingRefundProcessedEventResponse typedResponse = new PendingRefundProcessedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static DebtWriteOffProcessedEventResponse getDebtWriteOffProcessedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(DEBTWRITEOFFPROCESSED_EVENT, log);
        DebtWriteOffProcessedEventResponse typedResponse = new DebtWriteOffProcessedEventResponse();
        typedResponse.log = log;
        typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public static PendingRefundProcessedEventResponse getPendingRefundProcessedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PENDINGREFUNDPROCESSED_EVENT, log);
        PendingRefundProcessedEventResponse typedResponse = new PendingRefundProcessedEventResponse();
        typedResponse.log = log;
        typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public static List<RefundAccountSetEventResponse> getRefundAccountSetEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(REFUNDACCOUNTSET_EVENT, transactionReceipt);
        ArrayList<RefundAccountSetEventResponse> responses = new ArrayList<RefundAccountSetEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            RefundAccountSetEventResponse typedResponse = new RefundAccountSetEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.refundAccount = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static List<RefundProcessedEventResponse> getRefundProcessedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(REFUNDPROCESSED_EVENT, transactionReceipt);
        ArrayList<RefundProcessedEventResponse> responses = new ArrayList<RefundProcessedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            RefundProcessedEventResponse typedResponse = new RefundProcessedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static FixedFeeSetEventResponse getFixedFeeSetEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(FIXEDFEESET_EVENT, log);
        FixedFeeSetEventResponse typedResponse = new FixedFeeSetEventResponse();
        typedResponse.log = log;
        typedResponse.fixedFee = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public static List<RoleAdminChangedEventResponse> getRoleAdminChangedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROLEADMINCHANGED_EVENT, transactionReceipt);
        ArrayList<RoleAdminChangedEventResponse> responses = new ArrayList<RoleAdminChangedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            RoleAdminChangedEventResponse typedResponse = new RoleAdminChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.previousAdminRole = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.newAdminRole = (byte[]) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static List<RoleGrantedEventResponse> getRoleGrantedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROLEGRANTED_EVENT, transactionReceipt);
        ArrayList<RoleGrantedEventResponse> responses = new ArrayList<RoleGrantedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            RoleGrantedEventResponse typedResponse = new RoleGrantedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.account = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static List<RoleRevokedEventResponse> getRoleRevokedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROLEREVOKED_EVENT, transactionReceipt);
        ArrayList<RoleRevokedEventResponse> responses = new ArrayList<RoleRevokedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            RoleRevokedEventResponse typedResponse = new RoleRevokedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.account = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentAccountSetEventResponse getPaymentAccountSetEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTACCOUNTSET_EVENT, log);
        PaymentAccountSetEventResponse typedResponse = new PaymentAccountSetEventResponse();
        typedResponse.log = log;
        typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.paymentAccount = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public static List<SaveRefundProcessedEventResponse> getSaveRefundProcessedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SAVEREFUNDPROCESSED_EVENT, transactionReceipt);
        ArrayList<SaveRefundProcessedEventResponse> responses = new ArrayList<SaveRefundProcessedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            SaveRefundProcessedEventResponse typedResponse = new SaveRefundProcessedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static List<SecurityDelaySetEventResponse> getSecurityDelaySetEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SECURITYDELAYSET_EVENT, transactionReceipt);
        ArrayList<SecurityDelaySetEventResponse> responses = new ArrayList<SecurityDelaySetEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            SecurityDelaySetEventResponse typedResponse = new SecurityDelaySetEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.securityDelay = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static List<SettlementPeriodSetEventResponse> getSettlementPeriodSetEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SETTLEMENTPERIODSET_EVENT, transactionReceipt);
        ArrayList<SettlementPeriodSetEventResponse> responses = new ArrayList<SettlementPeriodSetEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            SettlementPeriodSetEventResponse typedResponse = new SettlementPeriodSetEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.settlementPeriod = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentReceiverSetEventResponse getPaymentReceiverSetEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTRECEIVERSET_EVENT, log);
        PaymentReceiverSetEventResponse typedResponse = new PaymentReceiverSetEventResponse();
        typedResponse.log = log;
        typedResponse.paymentReceiver = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public static List<SettlementProcessedEventResponse> getSettlementProcessedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SETTLEMENTPROCESSED_EVENT, transactionReceipt);
        ArrayList<SettlementProcessedEventResponse> responses = new ArrayList<SettlementProcessedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            SettlementProcessedEventResponse typedResponse = new SettlementProcessedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.settlementId = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.fee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static List<VerifiedBalanceIncreasedForEventResponse> getVerifiedBalanceIncreasedForEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VERIFIEDBALANCEINCREASEDFOR_EVENT, transactionReceipt);
        ArrayList<VerifiedBalanceIncreasedForEventResponse> responses = new ArrayList<VerifiedBalanceIncreasedForEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            VerifiedBalanceIncreasedForEventResponse typedResponse = new VerifiedBalanceIncreasedForEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.increase = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static List<VerifiedBalanceSetForEventResponse> getVerifiedBalanceSetForEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VERIFIEDBALANCESETFOR_EVENT, transactionReceipt);
        ArrayList<VerifiedBalanceSetForEventResponse> responses = new ArrayList<VerifiedBalanceSetForEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            VerifiedBalanceSetForEventResponse typedResponse = new VerifiedBalanceSetForEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.verifiedBalance = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    @Deprecated
    public static TangemBridgeProcessor load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new TangemBridgeProcessor(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TangemBridgeProcessor load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TangemBridgeProcessor(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TangemBridgeProcessor load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new TangemBridgeProcessor(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TangemBridgeProcessor load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TangemBridgeProcessor(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RefundAccountSetEventResponse getRefundAccountSetEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(REFUNDACCOUNTSET_EVENT, log);
        RefundAccountSetEventResponse typedResponse = new RefundAccountSetEventResponse();
        typedResponse.log = log;
        typedResponse.refundAccount = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public static RemoteCall<TangemBridgeProcessor> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String paymentReceiver_, String refundAccount_, String paymentAccountFactory_, String paymentToken_, BigInteger securityDelay_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.asList(new Address(160, paymentReceiver_),
                new Address(160, refundAccount_),
                new Address(160, paymentAccountFactory_),
                new Address(160, paymentToken_),
                new Uint256(securityDelay_)));
        return deployRemoteCall(TangemBridgeProcessor.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<TangemBridgeProcessor> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String paymentReceiver_, String refundAccount_, String paymentAccountFactory_, String paymentToken_, BigInteger securityDelay_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.asList(new Address(160, paymentReceiver_),
                new Address(160, refundAccount_),
                new Address(160, paymentAccountFactory_),
                new Address(160, paymentToken_),
                new Uint256(securityDelay_)));
        return deployRemoteCall(TangemBridgeProcessor.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TangemBridgeProcessor> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String paymentReceiver_, String refundAccount_, String paymentAccountFactory_, String paymentToken_, BigInteger securityDelay_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.asList(new Address(160, paymentReceiver_),
                new Address(160, refundAccount_),
                new Address(160, paymentAccountFactory_),
                new Address(160, paymentToken_),
                new Uint256(securityDelay_)));
        return deployRemoteCall(TangemBridgeProcessor.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static RefundProcessedEventResponse getRefundProcessedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(REFUNDPROCESSED_EVENT, log);
        RefundProcessedEventResponse typedResponse = new RefundProcessedEventResponse();
        typedResponse.log = log;
        typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    @Deprecated
    public static RemoteCall<TangemBridgeProcessor> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String paymentReceiver_, String refundAccount_, String paymentAccountFactory_, String paymentToken_, BigInteger securityDelay_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.asList(new Address(160, paymentReceiver_),
                new Address(160, refundAccount_),
                new Address(160, paymentAccountFactory_),
                new Address(160, paymentToken_),
                new Uint256(securityDelay_)));
        return deployRemoteCall(TangemBridgeProcessor.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static RoleAdminChangedEventResponse getRoleAdminChangedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROLEADMINCHANGED_EVENT, log);
        RoleAdminChangedEventResponse typedResponse = new RoleAdminChangedEventResponse();
        typedResponse.log = log;
        typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.previousAdminRole = (byte[]) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.newAdminRole = (byte[]) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public static RoleGrantedEventResponse getRoleGrantedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROLEGRANTED_EVENT, log);
        RoleGrantedEventResponse typedResponse = new RoleGrantedEventResponse();
        typedResponse.log = log;
        typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.account = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.sender = (String) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public static RoleRevokedEventResponse getRoleRevokedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROLEREVOKED_EVENT, log);
        RoleRevokedEventResponse typedResponse = new RoleRevokedEventResponse();
        typedResponse.log = log;
        typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.account = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.sender = (String) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public static SaveRefundProcessedEventResponse getSaveRefundProcessedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SAVEREFUNDPROCESSED_EVENT, log);
        SaveRefundProcessedEventResponse typedResponse = new SaveRefundProcessedEventResponse();
        typedResponse.log = log;
        typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public static SecurityDelaySetEventResponse getSecurityDelaySetEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SECURITYDELAYSET_EVENT, log);
        SecurityDelaySetEventResponse typedResponse = new SecurityDelaySetEventResponse();
        typedResponse.log = log;
        typedResponse.securityDelay = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public static SettlementPeriodSetEventResponse getSettlementPeriodSetEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SETTLEMENTPERIODSET_EVENT, log);
        SettlementPeriodSetEventResponse typedResponse = new SettlementPeriodSetEventResponse();
        typedResponse.log = log;
        typedResponse.settlementPeriod = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public static SettlementProcessedEventResponse getSettlementProcessedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SETTLEMENTPROCESSED_EVENT, log);
        SettlementProcessedEventResponse typedResponse = new SettlementProcessedEventResponse();
        typedResponse.log = log;
        typedResponse.card = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.transactionId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.settlementId = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
        typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.fee = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public static VerifiedBalanceIncreasedForEventResponse getVerifiedBalanceIncreasedForEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VERIFIEDBALANCEINCREASEDFOR_EVENT, log);
        VerifiedBalanceIncreasedForEventResponse typedResponse = new VerifiedBalanceIncreasedForEventResponse();
        typedResponse.log = log;
        typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.increase = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public static VerifiedBalanceSetForEventResponse getVerifiedBalanceSetForEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VERIFIEDBALANCESETFOR_EVENT, log);
        VerifiedBalanceSetForEventResponse typedResponse = new VerifiedBalanceSetForEventResponse();
        typedResponse.log = log;
        typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.verifiedBalance = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<AuthorizationChangeProcessedEventResponse> authorizationChangeProcessedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAuthorizationChangeProcessedEventFromLog(log));
    }

    public Flowable<AuthorizationChangeProcessedEventResponse> authorizationChangeProcessedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(AUTHORIZATIONCHANGEPROCESSED_EVENT));
        return authorizationChangeProcessedEventFlowable(filter);
    }

    public Flowable<AuthorizationProcessedEventResponse> authorizationProcessedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAuthorizationProcessedEventFromLog(log));
    }

    public Flowable<AuthorizationProcessedEventResponse> authorizationProcessedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(AUTHORIZATIONPROCESSED_EVENT));
        return authorizationProcessedEventFlowable(filter);
    }

    public Flowable<DebtWriteOffProcessedEventResponse> debtWriteOffProcessedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getDebtWriteOffProcessedEventFromLog(log));
    }

    public Flowable<DebtWriteOffProcessedEventResponse> debtWriteOffProcessedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DEBTWRITEOFFPROCESSED_EVENT));
        return debtWriteOffProcessedEventFlowable(filter);
    }

    public Flowable<FixedFeeSetEventResponse> fixedFeeSetEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getFixedFeeSetEventFromLog(log));
    }

    public Flowable<FixedFeeSetEventResponse> fixedFeeSetEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(FIXEDFEESET_EVENT));
        return fixedFeeSetEventFlowable(filter);
    }

    public Flowable<PaymentAccountSetEventResponse> paymentAccountSetEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentAccountSetEventFromLog(log));
    }

    public Flowable<PaymentAccountSetEventResponse> paymentAccountSetEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTACCOUNTSET_EVENT));
        return paymentAccountSetEventFlowable(filter);
    }

    public Flowable<PaymentReceiverSetEventResponse> paymentReceiverSetEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentReceiverSetEventFromLog(log));
    }

    public Flowable<PaymentReceiverSetEventResponse> paymentReceiverSetEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTRECEIVERSET_EVENT));
        return paymentReceiverSetEventFlowable(filter);
    }

    public Flowable<PendingRefundProcessedEventResponse> pendingRefundProcessedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPendingRefundProcessedEventFromLog(log));
    }

    public Flowable<PendingRefundProcessedEventResponse> pendingRefundProcessedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PENDINGREFUNDPROCESSED_EVENT));
        return pendingRefundProcessedEventFlowable(filter);
    }

    public Flowable<RefundAccountSetEventResponse> refundAccountSetEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRefundAccountSetEventFromLog(log));
    }

    public Flowable<RefundAccountSetEventResponse> refundAccountSetEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REFUNDACCOUNTSET_EVENT));
        return refundAccountSetEventFlowable(filter);
    }

    public Flowable<RefundProcessedEventResponse> refundProcessedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRefundProcessedEventFromLog(log));
    }

    public Flowable<RefundProcessedEventResponse> refundProcessedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REFUNDPROCESSED_EVENT));
        return refundProcessedEventFlowable(filter);
    }

    public Flowable<RoleAdminChangedEventResponse> roleAdminChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRoleAdminChangedEventFromLog(log));
    }

    public Flowable<RoleAdminChangedEventResponse> roleAdminChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROLEADMINCHANGED_EVENT));
        return roleAdminChangedEventFlowable(filter);
    }

    public Flowable<RoleGrantedEventResponse> roleGrantedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRoleGrantedEventFromLog(log));
    }

    public Flowable<RoleGrantedEventResponse> roleGrantedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROLEGRANTED_EVENT));
        return roleGrantedEventFlowable(filter);
    }

    public Flowable<RoleRevokedEventResponse> roleRevokedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRoleRevokedEventFromLog(log));
    }

    public Flowable<RoleRevokedEventResponse> roleRevokedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROLEREVOKED_EVENT));
        return roleRevokedEventFlowable(filter);
    }

    public Flowable<SaveRefundProcessedEventResponse> saveRefundProcessedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSaveRefundProcessedEventFromLog(log));
    }

    public Flowable<SaveRefundProcessedEventResponse> saveRefundProcessedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SAVEREFUNDPROCESSED_EVENT));
        return saveRefundProcessedEventFlowable(filter);
    }

    public Flowable<VerifiedBalanceSetForEventResponse> verifiedBalanceSetForEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getVerifiedBalanceSetForEventFromLog(log));
    }

    public Flowable<VerifiedBalanceSetForEventResponse> verifiedBalanceSetForEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VERIFIEDBALANCESETFOR_EVENT));
        return verifiedBalanceSetForEventFlowable(filter);
    }

    public Flowable<SecurityDelaySetEventResponse> securityDelaySetEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSecurityDelaySetEventFromLog(log));
    }

    public Flowable<SecurityDelaySetEventResponse> securityDelaySetEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SECURITYDELAYSET_EVENT));
        return securityDelaySetEventFlowable(filter);
    }

    public Flowable<SettlementPeriodSetEventResponse> settlementPeriodSetEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSettlementPeriodSetEventFromLog(log));
    }

    public Flowable<SettlementPeriodSetEventResponse> settlementPeriodSetEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SETTLEMENTPERIODSET_EVENT));
        return settlementPeriodSetEventFlowable(filter);
    }

    public Flowable<SettlementProcessedEventResponse> settlementProcessedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSettlementProcessedEventFromLog(log));
    }

    public Flowable<SettlementProcessedEventResponse> settlementProcessedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SETTLEMENTPROCESSED_EVENT));
        return settlementProcessedEventFlowable(filter);
    }

    public Flowable<VerifiedBalanceIncreasedForEventResponse> verifiedBalanceIncreasedForEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getVerifiedBalanceIncreasedForEventFromLog(log));
    }

    public Flowable<VerifiedBalanceIncreasedForEventResponse> verifiedBalanceIncreasedForEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VERIFIEDBALANCEINCREASEDFOR_EVENT));
        return verifiedBalanceIncreasedForEventFlowable(filter);
    }

    public RemoteFunctionCall<byte[]> AUTHORIZATION_PROCESSOR_ROLE() {
        final Function function = new Function(FUNC_AUTHORIZATION_PROCESSOR_ROLE,
                List.of(),
                List.of(new TypeReference<Bytes32>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> BALANCE_VERIFIER_ROLE() {
        final Function function = new Function(FUNC_BALANCE_VERIFIER_ROLE,
                List.of(),
                List.of(new TypeReference<Bytes32>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> DEBT_PROCESSOR_ROLE() {
        final Function function = new Function(FUNC_DEBT_PROCESSOR_ROLE,
                List.of(),
                List.of(new TypeReference<Bytes32>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> DEFAULT_ADMIN_ROLE() {
        final Function function = new Function(FUNC_DEFAULT_ADMIN_ROLE,
                List.of(),
                List.of(new TypeReference<Bytes32>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> PAYMENT_ACCOUNT_SETTER() {
        final Function function = new Function(FUNC_PAYMENT_ACCOUNT_SETTER,
                List.of(),
                List.of(new TypeReference<Bytes32>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> PROPERTY_SETTER_ROLE() {
        final Function function = new Function(FUNC_PROPERTY_SETTER_ROLE,
                List.of(),
                List.of(new TypeReference<Bytes32>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> REFUND_PROCESSOR_ROLE() {
        final Function function = new Function(FUNC_REFUND_PROCESSOR_ROLE,
                List.of(),
                List.of(new TypeReference<Bytes32>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> SETTLEMENT_PROCESSOR_ROLE() {
        final Function function = new Function(FUNC_SETTLEMENT_PROCESSOR_ROLE,
                List.of(),
                List.of(new TypeReference<Bytes32>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<BigInteger> fixedFee() {
        final Function function = new Function(FUNC_FIXEDFEE,
                List.of(),
                List.of(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> getPaymentAccount(String card) {
        final Function function = new Function(FUNC_GETPAYMENTACCOUNT,
                List.of(new Address(160, card)),
                List.of(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<byte[]> getRoleAdmin(byte[] role) {
        final Function function = new Function(FUNC_GETROLEADMIN,
                List.of(new Bytes32(role)),
                List.of(new TypeReference<Bytes32>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<String> getRoleMember(byte[] role, BigInteger index) {
        final Function function = new Function(FUNC_GETROLEMEMBER,
                Arrays.asList(new Bytes32(role),
                        new Uint256(index)),
                List.of(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> getRoleMemberCount(byte[] role) {
        final Function function = new Function(FUNC_GETROLEMEMBERCOUNT,
                List.of(new Bytes32(role)),
                List.of(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> grantRole(byte[] role, String account) {
        final Function function = new Function(
                FUNC_GRANTROLE,
                Arrays.asList(new Bytes32(role),
                        new Address(160, account)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> hasRole(byte[] role, String account) {
        final Function function = new Function(FUNC_HASROLE,
                Arrays.asList(new Bytes32(role),
                        new Address(160, account)),
                List.of(new TypeReference<Bool>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> increaseVerifiedBalanceFor(String paymentAccount, BigInteger increase) {
        final Function function = new Function(
                FUNC_INCREASEVERIFIEDBALANCEFOR,
                Arrays.asList(new Address(160, paymentAccount),
                        new Uint256(increase)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> paymentAccountFactory() {
        final Function function = new Function(FUNC_PAYMENTACCOUNTFACTORY,
                List.of(),
                List.of(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> paymentReceiver() {
        final Function function = new Function(FUNC_PAYMENTRECEIVER,
                List.of(),
                List.of(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> paymentToken() {
        final Function function = new Function(FUNC_PAYMENTTOKEN,
                List.of(),
                List.of(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> processAuthorization(String card, BigInteger transactionId, BigInteger amount, byte[] otp, BigInteger otpCounter, Boolean forced) {
        final Function function = new Function(
                FUNC_PROCESSAUTHORIZATION,
                Arrays.asList(new Address(160, card),
                        new Uint256(transactionId),
                        new Uint256(amount),
                        new org.web3j.abi.datatypes.generated.Bytes16(otp),
                        new org.web3j.abi.datatypes.generated.Uint16(otpCounter),
                        new Bool(forced)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> processAuthorizationChange(String card, BigInteger transactionId, BigInteger amount) {
        final Function function = new Function(
                FUNC_PROCESSAUTHORIZATIONCHANGE,
                Arrays.asList(new Address(160, card),
                        new Uint256(transactionId),
                        new Uint256(amount)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> processAuthorizationNoOtp(String card, BigInteger transactionId, BigInteger amount, Boolean forced) {
        final Function function = new Function(
                FUNC_PROCESSAUTHORIZATIONNOOTP,
                Arrays.asList(new Address(160, card),
                        new Uint256(transactionId),
                        new Uint256(amount),
                        new Bool(forced)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> processPendingRefund(String card, BigInteger transactionId) {
        final Function function = new Function(
                FUNC_PROCESSPENDINGREFUND,
                Arrays.asList(new Address(160, card),
                        new Uint256(transactionId)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> processRefund(String card, BigInteger transactionId, BigInteger refundAmount) {
        final Function function = new Function(
                FUNC_PROCESSREFUND,
                Arrays.asList(new Address(160, card),
                        new Uint256(transactionId),
                        new Uint256(refundAmount)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> processRefundCallback() {
        final Function function = new Function(
                FUNC_PROCESSREFUNDCALLBACK,
                List.of(),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> processSettlement(String card, BigInteger transactionId, BigInteger settlementId, BigInteger amount) {
        final Function function = new Function(
                FUNC_PROCESSSETTLEMENT,
                Arrays.asList(new Address(160, card),
                        new Uint256(transactionId),
                        new Uint256(settlementId),
                        new Uint256(amount)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> refundAccount() {
        final Function function = new Function(FUNC_REFUNDACCOUNT,
                List.of(),
                List.of(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> renounceRole(byte[] role, String callerConfirmation) {
        final Function function = new Function(
                FUNC_RENOUNCEROLE,
                Arrays.asList(new Bytes32(role),
                        new Address(160, callerConfirmation)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> revokeRole(byte[] role, String account) {
        final Function function = new Function(
                FUNC_REVOKEROLE,
                Arrays.asList(new Bytes32(role),
                        new Address(160, account)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> savePendingRefund(String card, BigInteger transactionId, BigInteger amount) {
        final Function function = new Function(
                FUNC_SAVEPENDINGREFUND,
                Arrays.asList(new Address(160, card),
                        new Uint256(transactionId),
                        new Uint256(amount)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> securityDelay() {
        final Function function = new Function(FUNC_SECURITYDELAY,
                List.of(),
                List.of(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setFixedFee(BigInteger fixedFee_) {
        final Function function = new Function(
                FUNC_SETFIXEDFEE,
                List.of(new Uint256(fixedFee_)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setPaymentAccount(String card, String paymentAccount) {
        final Function function = new Function(
                FUNC_SETPAYMENTACCOUNT,
                Arrays.asList(new Address(160, card),
                        new Address(160, paymentAccount)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setPaymentReceiver(String paymentReceiver_) {
        final Function function = new Function(
                FUNC_SETPAYMENTRECEIVER,
                List.of(new Address(160, paymentReceiver_)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setRefundAccount(String refundAccount_) {
        final Function function = new Function(
                FUNC_SETREFUNDACCOUNT,
                List.of(new Address(160, refundAccount_)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setSecurityDelay(BigInteger securityDelay_) {
        final Function function = new Function(
                FUNC_SETSECURITYDELAY,
                List.of(new Uint256(securityDelay_)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setSettlementPeriod(BigInteger settlementPeriod_) {
        final Function function = new Function(
                FUNC_SETSETTLEMENTPERIOD,
                List.of(new Uint256(settlementPeriod_)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setVerifiedBalanceFor(String paymentAccount, BigInteger verifiedBalance) {
        final Function function = new Function(
                FUNC_SETVERIFIEDBALANCEFOR,
                Arrays.asList(new Address(160, paymentAccount),
                        new Uint256(verifiedBalance)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> settlementPeriod() {
        final Function function = new Function(FUNC_SETTLEMENTPERIOD,
                List.of(),
                List.of(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Boolean> supportsInterface(byte[] interfaceId) {
        final Function function = new Function(FUNC_SUPPORTSINTERFACE,
                List.of(new Bytes4(interfaceId)),
                List.of(new TypeReference<Bool>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> writeOffDebt(String card, BigInteger amount) {
        final Function function = new Function(
                FUNC_WRITEOFFDEBT,
                Arrays.asList(new Address(160, card),
                        new Uint256(amount)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static class AuthorizationChangeProcessedEventResponse extends BaseEventResponse {
        public String card;

        public BigInteger transactionId;

        public BigInteger amount;

        public BigInteger fee;
    }

    public static class AuthorizationProcessedEventResponse extends BaseEventResponse {
        public String card;

        public BigInteger transactionId;

        public BigInteger amount;

        public BigInteger fee;
    }

    public static class DebtWriteOffProcessedEventResponse extends BaseEventResponse {
        public String card;

        public BigInteger amount;
    }

    public static class FixedFeeSetEventResponse extends BaseEventResponse {
        public BigInteger fixedFee;
    }

    public static class PaymentAccountSetEventResponse extends BaseEventResponse {
        public String card;

        public String paymentAccount;
    }

    public static class PaymentReceiverSetEventResponse extends BaseEventResponse {
        public String paymentReceiver;
    }

    public static class PendingRefundProcessedEventResponse extends BaseEventResponse {
        public String card;

        public BigInteger transactionId;

        public BigInteger amount;
    }

    public static class RefundAccountSetEventResponse extends BaseEventResponse {
        public String refundAccount;
    }

    public static class RefundProcessedEventResponse extends BaseEventResponse {
        public String card;

        public BigInteger transactionId;

        public BigInteger amount;
    }

    public static class RoleAdminChangedEventResponse extends BaseEventResponse {
        public byte[] role;

        public byte[] previousAdminRole;

        public byte[] newAdminRole;
    }

    public static class RoleGrantedEventResponse extends BaseEventResponse {
        public byte[] role;

        public String account;

        public String sender;
    }

    public static class RoleRevokedEventResponse extends BaseEventResponse {
        public byte[] role;

        public String account;

        public String sender;
    }

    public static class SaveRefundProcessedEventResponse extends BaseEventResponse {
        public String card;

        public BigInteger transactionId;

        public BigInteger amount;
    }

    public static class SecurityDelaySetEventResponse extends BaseEventResponse {
        public BigInteger securityDelay;
    }

    public static class SettlementPeriodSetEventResponse extends BaseEventResponse {
        public BigInteger settlementPeriod;
    }

    public static class SettlementProcessedEventResponse extends BaseEventResponse {
        public String card;

        public BigInteger transactionId;

        public BigInteger settlementId;

        public BigInteger amount;

        public BigInteger fee;
    }

    public static class VerifiedBalanceIncreasedForEventResponse extends BaseEventResponse {
        public String paymentAccount;

        public BigInteger increase;
    }

    public static class VerifiedBalanceSetForEventResponse extends BaseEventResponse {
        public String paymentAccount;

        public BigInteger verifiedBalance;
    }
}
