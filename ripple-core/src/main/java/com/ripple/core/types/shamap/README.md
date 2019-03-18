Rippled NodeStore
-----------------

To understand a ShaMap first you must know about the NodeStore.

```java
/**

 * This is a toy implementation for illustrative purposes.
 */
public class NodeStore {
    /**
    * In ripple, all data is stored in a simple binary key/value database.
    * The keys are 256 bit binary strings and the values are binary strings of
    * arbitrary length.
    */
    public interface KeyValueBackend {
        void   put(Hash256 key, byte[] content);
        byte[] get(Hash256 key);
    }

    KeyValueBackend backend;
    public NodeStore(KeyValueBackend backend) {
        this.backend = backend;
    }
    /**
     * All data stored is keyed by the hash of it's contents.
     * Ripple uses the first 256 bits of a sha512 as it's 33 percent
     * faster than using sha256.
     *
     * @return `key` used to store the content
     */
    private Hash256 storeContent(byte[] content) {
        Hash256.HalfSha512 hasher = new Hash256.HalfSha512();
        hasher.update(content);
        Hash256 key = hasher.finish();
        storeHashKeyedContent(key, content);
        return key;
    }

    /**
     * @param hash As ripple uses the `hash` of the contents as the
     *             NodeStore key, `hash` is pervasively used in lieu of
     *             the term `key`.
     */
    private void storeHashKeyedContent(Hash256 hash, byte[] content) {
        // Note: The real NodeStore actually prepends some metadata, which doesn't
        // contribute to the hash.
        backend.put(hash, content); // metadata + content
    }

    /**
     * The complement to `set` api, which together form a simple public interface.
     */
    public byte[] get(Hash256 hash) {
        return backend.get(hash);

    }
    /**
     * The complement to `get` api, which together form a simple public interface.
     */
    public Hash256 set(byte[] content) {
        return storeContent(content);
    }
}
```

See also:
* [Serialized Types](../../../../../../../../README.md)
* [BinaryFormats.txt (historical)](https://github.com/ripple/rippled/blob/07df5f1f81b0ee1ab641d134ba8e940a90f5297e/BinaryFormats.txt#L2-L6)

Excerpt from BinaryFormats.txt (historical): 

  <blockquote>
  All signed or hashed objects must have well-defined binary formats at the
  byte level. These formats do not have to be the same as the network wire
  formats or the forms used for efficient storage or human display. However,
  it must always be possible to precisely re-create, byte for byte, any signed
  or hashed object. Otherwise, the signatures or hashes cannot be validated.
  </blockquote>

Note that currently (26/Jan/2018) the NodeStore stores it in the hashing form.

What is a ShaMap?
-----------------

A ShaMap is a special type of tree, used as a way to index values stored in a
`NodeStore`

Recall that values in the NodeStore are keyed by the hash of their contents.

But what about identities that change over time? How can you retrieve a certain
version of something? What could be used as an enduring identifier? The value
must have some component[s] that are static over time. These are fed into a
hashing function to create a 256 bit identifier.

But how is this used? You can only query values by `hash` in the NodeStore. The
hash, as a function of a value would obviously change along with it.

The identifier is used as an `index` into a ShaMap tree, which in ripple, is
representative of a point in time. In fact a ShaMap can be hashed
deterministically, thus a point in time can be identified by a `hash`. Where is
a ShaMap actually stored? In the NodeStore, of course.

But the NodeStore only stores binary content you protest! But the ShaMap has a
binary representation! So what are the `contents` of a ShaMap, to be hashed to
be stored in the NodeStore?

Glad you asked. A tree, has a root, and many children, either more branches, or
terminal leaves. The root, and any of its children that have children
themselves, are classed as `inner nodes`.

In a ShaMap these `inner nodes` each have 16 `slots` for children. The binary
representation of these is simply 16 `hash`es, used to retrieve child nodes from
the NodeStore.

An example of an inner node's `contents`

  Empty slots are represented as 32 0 bytes

  ```
  022CC592F5D4ABC3A63DA2A036CDDC0825B30717C78EF287BEF200056133FDA2
  0000000000000000000000000000000000000000000000000000000000000000
  BEE626551799DDFE65BD2D9A0F0EA24D72C93CFD8E083176718D2B079EC60214
  E1B34F1D9209CB668A50CCEE71C8109D140A6D715D923AEE98E6D53015D8B66B
  4C27A856094CFDE37CD2A0EA93DADB595B10CFEC55F816C987A6AC48D13AF5C0
  2F770714A9EF92792F44AA1537C18F68AFE3FFF157FB9088FFE2BDA695C19B71
  C915CA982310CF41CF1266AA43C3B31ACBF4304D05ADB54A352D942C890763A3
  F29FAD442CE204513BEA555A4192E324407444D946449CEA510C37A9BB982134
  0000000000000000000000000000000000000000000000000000000000000000
  0000000000000000000000000000000000000000000000000000000000000000
  44B3BE10744EA2DA010D530C6AE64E3C3984DA7701EE79A66EA429EC11B87D1D
  0C7BE8569E9F08BADDEB91EBE79E5B98BBF245B067B7B83B4A95430CFEC9F7E8
  BB38EA169DB6A020EA820BD1242DDB6250B397A26015507BA3C7F3C041EA683C
  0000000000000000000000000000000000000000000000000000000000000000
  15B98934D22B5CB7233C42CE8DC8DD0D2328AB91CC574332C45D7160BD31D4AD
  1894E389AE4A63BA99C2D0546A58A976ECDAB14C09B98F532999B464696E29E5
  ```

What about those `index` thingies again? Remember, the `index` can't be used to
query something directly in the NodeStore.

First you need a known point in time, which we learned could be defined by a
`hash` of a ShaMap.

* shamap hash: 2F049AEE51C7C96AB911AF86E3278F4F90A38D196422A832617FF0C6F29C3704
* value index: FEEE5CC92B64375C8FEE56D54A82B9965E44FE0DCF673DBF27D0AA93F8AFF4FB

Imagine we have those above. First we query the NodeStore with `2F049A...`

What do we expect back? A ShaMap hash is the hash of the binary representation
of the root node (which is an `inner node`) of the tree, so we'd expect
something in the form shown earlier, with 16 256 bit hashes.

From the NodeStore we retrieve:

  ```
  4D494E00
  3C926652404076CA35EA1D89580975C50DCD0B29EC16079F168BE63BB1D6F237
  F0D3F178C56C438C597D53FF5E3D44EE3865C17A0D25DCC2460F20BE4BEB4B7E
  D65D6CA291E451E6D6566A5431D755A749D9B0450E83B376148A6F93349ACD46
  309B18093008055BE23E118DA4243DFC9AEF66E3260E2008BD6E6E6806F06725
  0000000000000000000000000000000000000000000000000000000000000000
  D7D5A7C89DAE6D7517EDCB0601829CB339376EECD1DE8CA0216B6C18FFD77C32
  7C33AEB525B09A7BC013C01015D59A88129A8A6C18B45E6D7D0CA1F13E6FCB44
  AB35119A7529D4BA57C41D7020833A0E9600895F61E9E9DEABBFDF82A877CBB2
  3BE7A48570263034E6DCBEFA6ED57B63C0592AE145341A79DAF57C3BDF7C5793
  17006AD967A1536E0441517990AD5B42890A3168AC9E045EAF9AC5A06F672561
  94B12B8BB76D525DD9320AF51893A6D69FF730C1E647181E7A7B51FE1FBF369E
  0579A72B3209AE66840F6E8317F422F43937E6AE1A14C8B30243DE016551643F
  B873AA4EF1CDF0332C59BE423256610C4C6A1D32E2A1F8DE748390E29E8FC2D3
  EB03A93BBC95ED56FB73E6FBDAB8F6916BBD1BD358336CBA4DA673558F5B068C
  E64A78A07CF33C3B74AEA9862B5758513677324D4C9D72C69D14B4EB64D4EB02
  4E8C0CE75693B85A4CAC5516B77702E660A55281CF17DBE09552941F3078D81C
  ```

We see the hashes for 16 nodes clear as day, but what is this `4D494E00`
prefixed to the front? Converted to `ascii` letters the hex `4D494E00` is 
`MIN\x00`, meaning sham)ap i)nner n)ode.

The prefix `namespaces` the content, so different classes of objects which
would otherwise have the same binary representation, will have a different
`hash`. These `hash prefixes` serve another useful purpose, as we'll see later.
(Similarly, there are namespacing prefixes for an `index` (created by
 feeding static components of an identity into a hashing function))

Is our value `index` hash amongst those enumerated? No !!! So what do we do with
it? An index, usually means an ordinal, defining a place in an array. The
`index` is actually an index into 64 arrays. Each nibble in the `index` is an
index into the 16 slots in each inner node.

Consider again the value `index`:

  `FEEE5CC92B64375C8FEE56D54A82B9965E44FE0DCF673DBF27D0AA93F8AFF4FB`

To use the `index` we take the first nibble, `F` (yes, we go left to right)

The letter `F` in hex has the ordinal value 15, so we take the 16th branch (0
based indexing)

We select the 16th hash

  `4E8C0CE75693B85A4CAC5516B77702E660A55281CF17DBE09552941F3078D81C`

From the NodeStore we retrieve:

  ```
  4D494E00
  0000000000000000000000000000000000000000000000000000000000000000
  286BC64A4A369857E4B0B5834C54CED797110D06E64F7759FBA2C87D3630D418
  0000000000000000000000000000000000000000000000000000000000000000
  0000000000000000000000000000000000000000000000000000000000000000
  0000000000000000000000000000000000000000000000000000000000000000
  0000000000000000000000000000000000000000000000000000000000000000
  0000000000000000000000000000000000000000000000000000000000000000
  0000000000000000000000000000000000000000000000000000000000000000
  0000000000000000000000000000000000000000000000000000000000000000
  0000000000000000000000000000000000000000000000000000000000000000
  0000000000000000000000000000000000000000000000000000000000000000
  0000000000000000000000000000000000000000000000000000000000000000
  60631B91674954D21314B89CEE3B7F740B4FB1374610498EA1A7A3B79BA6706D
  3B1C36A6311256FE1E72BEDBA2042D67D86F561E51B387CFA680BCE9982CD6DB
  5568E7032EE018CA484C181CD68411B623229D0666AE0038708500C59402A282
  0000000000000000000000000000000000000000000000000000000000000000
  ```

There's that 'MIN\x00` hash prefix again.

In fact, this prefix is how we can deterministically say that this is an
`inner node` and that we can interpret the following bytes as 16 more
`hash`es.

We have descended deeper into the tree, but it seems we need to go deeper. We
are currently at a depth of 2, so to go deeper we need the 2nd nibble.

value index:
  ```
  FEEE5CC92B64375C8FEE56D54A82B9965E44FE0DCF673DBF27D0AA93F8AFF4FB`
   |
    \
     2nd nibble
  ```

The letter `E` in hex has the ordinal value 14, so we take the 15th branch (0
based indexing)

We select the 15th hash:

  `5568E7032EE018CA484C181CD68411B623229D0666AE0038708500C59402A282`

From the NodeStore we retrieve:

  ```
  534E4400
  C11C12000722800000002400AA7BFF201900AA7BFC201B0226FF9464D4461B5C
  A191A906000000000000000000000000455448000000000006CC4A6D023E68AA
  3499C6DE3E9F2DC52B8BA25465400000000861014A6840000000000000C17321
  03CF1DFB34A96363FF2B91638FCE51E6D7B88419729E9A81ABC99A9512FFB9C7
  3374463044022005ED4635AE246A4060378D9396CAA0E42F7E9129D309B2E98B
  DF5CE2352520A002207D4494019EA4327F4087EA34B05CE9E5CD0AE3082BAD77
  CC3698CE5A59C21BD581140BEC53D0830ADCE9E372086E570809916C440E83C3
  F4201C00000033F8E311006F561BF352EEDB9072286286A4BBC8C419C995A171
  0AA0CFA8B0D2B843F07D294F60E82400AA7BFF501090B86A84C7F7843673BCF8
  2E565E69498CAEF463F8055ABA4C04581E76C9270064D4461B5CA191A9060000
  00000000000000000000455448000000000006CC4A6D023E68AA3499C6DE3E9F
  2DC52B8BA25465400000000861014A81140BEC53D0830ADCE9E372086E570809
  916C440E83E1E1E31100645690B86A84C7F7843673BCF82E565E69498CAEF463
  F8055ABA4C04581E76C92700E8364C04581E76C927005890B86A84C7F7843673
  BCF82E565E69498CAEF463F8055ABA4C04581E76C92700011100000000000000
  00000000004554480000000000021106CC4A6D023E68AA3499C6DE3E9F2DC52B
  8BA254E1E1E41100645690B86A84C7F7843673BCF82E565E69498CAEF463F805
  5ABA4C045835BF30CCBFE72200000000364C045835BF30CCBF5890B86A84C7F7
  843673BCF82E565E69498CAEF463F8055ABA4C045835BF30CCBF011100000000
  00000000000000004554480000000000021106CC4A6D023E68AA3499C6DE3E9F
  2DC52B8BA2540311000000000000000000000000000000000000000004110000
  000000000000000000000000000000000000E1E1E411006F56AE4B62A73DC540
  40E523FCE863981AB601B6D6BCD3CFCE303B43A430FC6D4DB1E7220000000024
  00AA7BFC250226FF9133000000000000000034000000000000000055B61A418E
  421021CFBA22B02F4934B4F79B1091F91CF910AEB21DA0975CEC52AB501090B8
  6A84C7F7843673BCF82E565E69498CAEF463F8055ABA4C045835BF30CCBF64D4
  461B7D5C693EDF000000000000000000000000455448000000000006CC4A6D02
  3E68AA3499C6DE3E9F2DC52B8BA25465400000000861014A81140BEC53D0830A
  DCE9E372086E570809916C440E83E1E1E511006456B937CC88FCAF18886CC8D4
  B19A5326F56B0B84E06AE511407B072DE348E05376E722000000003100000000
  0000000032000000000000000058B937CC88FCAF18886CC8D4B19A5326F56B0B
  84E06AE511407B072DE348E0537682140BEC53D0830ADCE9E372086E57080991
  6C440E83E1E1E5110061250226FF915558E2CDAB1D8D5477D8FED6EA5EFD9F9F
  D8FAC56EA907E94197900C6C6175CE9556E9293AF964F2B20467530673C8F327
  41ED03DEF52F6B6625B4D837C155856C26E62400AA7BFF6240000000B60EBC57
  E1E722000000002400AA7C002D000000186240000000B60EBB9681140BEC53D0
  830ADCE9E372086E570809916C440E83E1E1F1031000
  FEEE5CC92B64375C8FEE56D54A82B9965E44FE0DCF673DBF27D0AA93F8AFF4FB
  ```

Well, here's something new. The `hash prefix` is different. This time the
hex decodes as `MLN\x00`, meaning sham)ap l)eaf n)ode.

And what's that at the end? Is that our index? It is!!

Why does it need to be stored? We have only used `FE` to traverse to this
node. Without storing the `index` identifier in the leaf node contents,
there would be no way to be certain that this leaf held the item you wanted.
More importantly, it acts as further name-spacing, to prevent collisions. 
(Technically, you could synthesize the index, by parsing the contents of
the object and recreating it)

Takeaways
---------

* A `hash` keys the NodeStore
* An `index` is a path to an item in a ShaMap
* For communication purposes
  - Always use `hash` when referring to a key for the NodeStore
  - Always use `index` when referring to a key for a ShaMap

Links
-----

* [Rippled Hash Prefix declarations](../../coretypes/hash/prefixes/HashPrefix.java)
