# SerializedTypes Overview

Ripple uses a node store, where objects are keyed by a 32byte hash value.

These keys are created by hashing a binary representation of the whole object
prefixed with a name-spacing sequence of bytes, unique to each class of object.
As such, a method of consistently producing a binary sequence from a given
object was required.

Objects have fields, which are a pairing of a type and a name. Names are simply
ordinals used to look up preassigned names in a per type table. The type ordinal,
similarly, is used to lookup a given class.

In ripple-lib-java the type equates to a `SerializedType`, with types such as 32
bit unsigned integer, variable length byte strings and even arrays of objects.
The fields have an ordinal quality, and can be deterministically sorted,
important for consistent binary representation.

There are also container types: An STObject, short for `SerializedType Object`,
is an associative container of fields to other SerializedTypes (even an STObject
itself). An STArray is an array of STObjects with a single Field, mapped to an
STObject, containing an arbitrary amount of Field -> SerializedType pairs. (See
notes below on differences to C++ implementation)

## Types

Following is a survey of some of the classes, sorted somewhat topologically.

The following json representation of metadata related to a finalized transaction
can be used as a concrete example and will be referenced later.

```java
/**
 *
 * # Transaction Meta JSON
 *
 * {
 *   "TransactionResult": "tesSUCCESS",
 *
 *      {@link com.ripple.core.types.known.tx.result.TransactionResult}
 *      {@link com.ripple.core.fields.Field.TransactionResult}
 *      {@link com.ripple.core.coretypes.uint.UInt8}
 *
 *   "TransactionIndex": 0,
 *
 *      {@link com.ripple.core.types.known.tx.result.TransactionMeta#transactionIndex}
 *      {@link com.ripple.core.coretypes.uint.UInt32}
 *
 *   "AffectedNodes": [
 *
 *      {@link com.ripple.core.types.known.tx.result.TransactionMeta#affectedNodes}
 *
 *     {

 *
 *          {@link com.ripple.core.types.known.tx.result.AffectedNode}

 *
 *         "NewFields": {
 *
 *              {@link com.ripple.core.types.known.tx.result.AffectedNode#nodeAsFinal()}
 *
 *           "Sequence": 103929,
 *
 *              {@link com.ripple.core.fields.Field#Sequence}
 *
 *
 *           "TakerGets": {
 *
 *              {@link com.ripple.core.coretypes.Amount}
 *
 *             "currency": "ILS",
 *
 *                  {@link com.ripple.core.coretypes.Currency}
 *
 *             "value": "1694.768",
 *
 *                  {@link com.ripple.core.coretypes.AccountID}
 *
 *             "issuer": "rNPRNzBB92BVpAhhZr4iXDTveCgV5Pofm9"
 *
 *                  {@link java.math.BigDecimal}
 *
 *           },
 *
 *
 *           "Account": "raD5qJMAShLeHZXf9wjUmo6vRK4arj9cF3",
 *
 *              {@link com.ripple.core.coretypes.AccountID}
 *
 *           "BookDirectory": "62A3338CAF2E1BEE510FC33DE1863C56948E962CCE173CA55C14BE8A20D7F000",
 *
 *              {@link com.ripple.core.coretypes.Quality#fromBookDirectory}
 *
 *           "TakerPays": "98957503520",
 *
 *              {@link com.ripple.core.coretypes.Amount}
 *              {@link com.ripple.core.coretypes.Amount#isNative()}
 *
 *           "OwnerNode": "000000000000000E"
 *
 *              {@link com.ripple.core.types.known.sle.entries.Offer#ownerNodeDirectoryIndex}
 *         },
 *
 *         "LedgerIndex": "3596CE72C902BAFAAB56CC486ACAF9B4AFC67CF7CADBB81A4AA9CBDC8C5CB1AA",
 *
 *              {@link com.ripple.core.coretypes.hash.Hash256#index}
 *
 *         "LedgerEntryType": "Offer"
 *
 *              {@link com.ripple.core.serialized.enums.LedgerEntryType}
 *              {@link com.ripple.core.types.known.sle.entries.Offer}
 *       }
 *     },
 *
 *
 *     {

 *
 *          {@link com.ripple.core.types.known.tx.result.AffectedNode}

 *
 *         "NewFields": {
 *
 *              {@link com.ripple.core.types.known.tx.result.AffectedNode#nodeAsFinal()}
 *
 *           "RootIndex": "62A3338CAF2E1BEE510FC33DE1863C56948E962CCE173CA55C14BE8A20D7F000",
 *
 *              com.ripple.core.coretypes.hash.Hash256#index
 *
 *           "TakerGetsIssuer": "92D705968936C419CE614BF264B5EEB1CEA47FF4",
 *
 *              {@link com.ripple.core.coretypes.Currency}
 *              {@link com.ripple.core.coretypes.hash.Hash160}
 *              {@link com.ripple.core.fields.Field#TakerGetsIssuer}
 *
 *           "ExchangeRate": "5C14BE8A20D7F000",
 *
 *              {@link com.ripple.core.coretypes.Quality#fromBookDirectory}
 *
 *           "TakerGetsCurrency": "000000000000000000000000494C530000000000"
 *
 *              {@link com.ripple.core.coretypes.Currency}
 *              {@link com.ripple.core.coretypes.hash.Hash160}
 *              {@link com.ripple.core.fields.Field#TakerGetsCurrency}
 *
 *         },
 *
 *         "LedgerIndex": "62A3338CAF2E1BEE510FC33DE1863C56948E962CCE173CA55C14BE8A20D7F000",
 *
 *              {@link com.ripple.core.coretypes.hash.Hash256#index}
 *
 *         "LedgerEntryType": "DirectoryNode"
 *
 *              {@link com.ripple.core.types.known.sle.entries.DirectoryNode}
 *              {@link com.ripple.core.serialized.enums.LedgerEntryType#DirectoryNode}
 *       }
 *     },
 *
 *     {
 *       "ModifiedNode": {
 *
 *          {@link com.ripple.core.types.known.tx.result.AffectedNode}
 *          {@link com.ripple.core.types.known.tx.result.AffectedNode#isModifiedNode()}
 *
 *         "FinalFields": {
 *
 *              {@link com.ripple.core.types.known.tx.result.AffectedNode#nodeAsFinal()}
 *
 *           "RootIndex": "801C5AFB5862D4666D0DF8E5BE1385DC9B421ED09A4269542A07BC0267584B64",
 *
 *              {@link com.ripple.core.types.known.sle.entries.DirectoryNode#rootIndex}
 *
 *           "Flags": 0,
 *           "Owner": "raD5qJMAShLeHZXf9wjUmo6vRK4arj9cF3",
 *
 *              {@link com.ripple.core.coretypes.hash.Index#ownerDirectory}
 *
 *           "IndexPrevious": "0000000000000000"
 *
 *              {@link com.ripple.core.types.known.sle.entries.DirectoryNode#hasPreviousIndex}
 *              {@link com.ripple.core.types.known.sle.entries.DirectoryNode#prevIndex}
 *         },
 *
 *         "LedgerIndex": "AB03F8AA02FFA4635E7CE2850416AEC5542910A2B4DBE93C318FEB08375E0DB5",
 *
 *         "LedgerEntryType": "DirectoryNode"
 *       }
 *     },
 *     {
 *       "ModifiedNode": {
 *
 *          {@link com.ripple.core.types.known.tx.result.AffectedNode}
 *          {@link com.ripple.core.types.known.tx.result.AffectedNode#isModifiedNode()}
 *
 *         "FinalFields": {
 *
 *              {@link com.ripple.core.types.known.tx.result.AffectedNode#nodeAsFinal()}
 *
 *           "Sequence": 103930,
 *           "Flags": 0,
 *           "Account": "raD5qJMAShLeHZXf9wjUmo6vRK4arj9cF3",
 *           "OwnerCount": 9,
 *           "Balance": "106861218302"
 *
 *              {@link com.ripple.core.coretypes.Amount}
 *         },
 *
 *         "LedgerIndex": "CF23A37E39A571A0F22EC3E97EB0169936B520C3088963F16C5EE4AC59130B1B",
 *
 *              {@link com.ripple.core.coretypes.hash.Index#accountRoot}
 *
 *         "LedgerEntryType": "AccountRoot",
 *
 *              {@link com.ripple.core.types.known.sle.entries.AccountRoot}
 *              {@link com.ripple.core.serialized.enums.LedgerEntryType#AccountRoot}
 *
 *         "PreviousFields": {
 *
 *              {@link com.ripple.core.types.known.tx.result.AffectedNode#nodeAsPrevious()}
 *
 *           "Sequence": 103929,
 *           "OwnerCount": 8,
 *           "Balance": "106861218312"
 *         },
 *
 *         "PreviousTxnID": "DE15F43F4A73C4F6CB1C334D9E47BDE84467C0902796BB81D4924885D1C11E6D",
 *
 *              {@link com.ripple.core.types.known.tx.Transaction#hash}
 *
 *         "PreviousTxnLgrSeq": 3225338
 *
 *              {@link com.ripple.core.coretypes.uint.UInt32}
 *       }
 *     }
 *   ]
 * }
*/
```

* Note that ALL field names are, by convention, upper case (in fact field names (index, hash) may be
lower cased but are not serialized)

* The `AffectedNodes` is an STArray. As stated, the immediate
children each contain only a single key (or [Field](src/main/java/com/ripple/core/fields/Field.java#L138-L140))

  * ModifiedNode

Moving on.

```
com
└── ripple
    ├── serialized
        ├── core
            │
            ├── enums
            │   ├── LedgerEntryType
            │   ├── EngineResult
            │   └── TransactionType
```

* In the json above look at the [TransactionResult](src/main/java/com/ripple/core/fields/Field.java#L164) field.
  Note that it has a Type of of UINT8, yet clearly it's represented in json as a string.

#### com.ripple.core.fields.Type

  This is a simple Java enum(eration) of the various types. eg.

    UINT32(2)

  This definition implies giving the static ordinal `2` to the UINT32 type.

#### com.ripple.core.fields.Field

  Consider the following definition of a field

    QualityIn(20, Type.UINT32)

  As stated before a Field has name and type ordinals, but it also has an
  implied symbolic string representation (as seen used in the json above)

  The string name can be looked up via a `code`, which is an integer created by
  shifting the type ordinal 16 bits to the left and ORing it with the name.

  See: [com.ripple.core.fields.Field#fromCode](src/main/java/com/ripple/core/fields/Field.java)

#### com.ripple.core.fields.HasField

  This is simply an interface for returning a Field. We know that a Field
  implies a Type and a name and there's a set amount of them. For each concrete
  class implementation of a given Type, we create a XXXfield class that
  implements HasField

  eg.

  ```java
  protected abstract static class STArrayField implements HasField{}
  public static STArrayField starrayField(final Field f) {
      return new STArrayField(){ @Override public Field getField() {return f;}};
  }
  ```

  Then we can create static members on the concrete class

  ```java
  static public STArrayField AffectedNodes = starrayField(Field.AffectedNodes);
  static public STArrayField Signatures = starrayField(Field.Signatures);
  static public STArrayField Template = starrayField(Field.Template);
  ```

  Later this is used create an api that looks as so

  ```java
  if (transactionType() == TransactionType.Payment && meta.has(STArray.AffectedNodes)) {
      STArray affected = meta.get(STArray.AffectedNodes);
      for (STObject node : affected) {
          if (node.has(STObject.CreatedNode)) {
              STObject created = node.get(STObject.CreatedNode);
  ```

  This is implemented by overloading get()

  ```java
  public STArray get(STArray.STArrayField f) {
      return (STArray) fields.get(f.getField());
  }
  ```

### com.ripple.core.serialized

#### com.ripple.core.serialized.SerializedType

```java
public interface SerializedType {
    Object toJSON();
    byte[] toBytes();
    String toHex();
    void toBytesSink(BytesSink to);
    Type type();
}
```

#### com.ripple.core.serialized.BytesList

A dynamic array of byte[]. Used by TypeTranslators to avoid needless 
copying (see fromParser(parser, hint)).

#### com.ripple.core.serialized.BinaryParser

Responsible for decoding Fields and VL encoded structures.

#### com.ripple.core.serialized.TypeTranslator

Handles converting a SerializedType instances to/from json, binary and other non
SerializedType values.

Has methods like fromHex, fromBytes, which delegate to fromParser.

#### com.ripple.core.serialized.BinarySerializer

Responsible for encoding Fields/SerializeType into binary.

## Notes

* In the C++ implementation of serialized objects, an STObject can, itself, be
  assigned a Field, which is stored outside of the associative structure.

  This is problematic when storing as json. Consider this pseudocode.

    ```python
    >>> so = STObject()
    >>> so.name = "FieldName"
    >>> so["FieldOfDreams"] = "A Kevin Costner Movie"
    >>> sa = STArray([so])
    ```

  How could `sa` be declared as json?

    ```json
    >>> [{"FieldName" : {"FieldOfDreams": "A Kevin Costner Movie"}}]
    ```

  This is in fact how rippled works. There is no 1:1 mapping of STObject to {}

  In ripple-lib(-java)? there is, and single key children of STArrays are enforced.

* Ripple uses 32 byte hashes for object indexes, taking half of a SHA512 hash,
  which is ~%33 faster than SHA256.

* Simply using google protocol buffers was considered inadequate [link](https://github.com/ripple/rippled/blob/ee51968820fc41c5aeadf2067bfdae54ff21fa66/BinaryFormats.txt#L16)