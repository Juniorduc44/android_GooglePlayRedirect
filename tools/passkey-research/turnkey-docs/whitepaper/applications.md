# Source: https://whitepaper.turnkey.com/applications/










Turnkey Whitepaper

















Index






Principles






Foundations






Architecture






Applications




















Applications beyond Key Management

Turnkey Team

January 2025

Abstract

While Turnkey has chosen key management as the first application on top of its verifiable foundations, we envision it to be relevant well beyond that.

First, we highlight some compelling use-cases and products built on top of Turnkey as it stands today and explain why they benefit from the foundations we provide.

Next, we imagine what could be unlocked by adding to or enhancing Turnkey. We look at what happens when QuorumOS can run on any secure hardware equipped with a TPM, when our policy engine is expanded a few steps beyond where it is today, when the flexibility of Quorum Sets is leveraged to its fullest extent, or when builders can write their own enclave applications running on Turnkey's
Verifiable Foundation

Finally we discuss what decentralizing Turnkey could look like. We envision that tasks such as reviewing critical source code and its dependencies, reproducing secure builds, and providing compute resources can be incentivized with common crypto-native mechanisms. A lot of public goods can come out of this. As an industry we must demand better tools to thwart supply chain risks. We believe a decentralized, censorship-resistant Turnkey will result in safer foundations to build critical software going forward, in or out of crypto. Security is a universal, growing need for all software developers who care about their end-users.

What Turnkey solves today

In this section we zoom out to explain what Turnkey solves today. We conclude this section with a list of products built on top of Turnkey and why these applications benefit from Turnkey's offering.

Offchain smart-contract-like authorization

One under-appreciated aspect of the Turnkey Policy Engine is its chain-agnostic language. For example, requiring two or more approvers for Turnkey activities is similar to an on-chain multi-sig setup where multiple keys share ownership of blockchain funds, except the functionality is chain-agnostic. If there are no smart contracts or multisig capabilities available (or if they are prohibitively expensive to use), Turnkey fills this gap.

There are also privacy and upgradability advantages to using Turnkey: modifying Turnkey policies (to require three approvers instead of two, for example) does not require any on-chain action. It is cheap and completely private. A Turnkey wallet looks like any other wallet on-chain, even though it may be controlled by multiple parties underneath. This concept is talked about at length in
Liquefaction
, where the authors introduce the concept of “key encumbrance” to describe the enforcement of “an access control policy over the key’s full lifecycle”. This is possible with Turnkey given the flexibility of its Policy Engine. A User or set of Users can be granted granular access to a wallet without having full knowledge of the underlying private key.

This off-chain authorization composes very well with existing on-chain primitives. For example, Ethereum Account Abstraction Wallets (using
EIP 4337
, also known as AA Wallets) can designate Externally Owned Accounts (EOAs) as signers. We see Turnkey customers use Turnkey-controlled EOAs for this purpose to minimize the number of on-chain actions and enhance privacy when users need to add or remove authenticators. Another big advantage of a Turnkey-controlled EOA is that it can be updated once, within Turnkey, and used across many Ethereum L2s. Creating AA wallets across many L2s would otherwise require many on-chain actions upon credential update


(one per AA wallet)

Cross-chain tooling: escrow, swaps, and more

Concrete use-cases that benefit from flexible and cross-chain authorization are escrow and swaps platforms. With Turnkey it is possible to build a robust off-chain swap workflow. For example:

Configure a new Turnkey Organization with two Root Users (Root Quorum configured with 2-out-of-2) and two distinct new wallets. These two users are the users who want to swap assets.

Both parties use consensus to create two policies which allow their individual User to unilaterally sign transactions for their own wallet. This is safe to do: the wallets are empty.

Users send funds to their designated wallets. This is safe because they can claw back their funds if something goes wrong, thanks to the policies introduced in the previous step.

Using consensus, users create 4 new policies such that they lose access to their original wallet and gain access to their new (swapped) wallet at the same time:

The first two policies explicitly
DENY
signing access to the original owners, negating the policies already in place (
DENY
takes precedence over
ALLOW

The other two policies
ALLOW
unilateral signing access for the new owners

This action is atomic:
CREATE_POLICIES
is a single activity on Turnkey which either succeeds in creating all 4 policies at once, or fails.

The two parties then transfer funds out of Turnkey at their own convenience. They know that no further critical action can be taken on this Turnkey Organization because of the Root Quorum setting.

This swap workflow doesn't depend on chain-specific functionality. As long as Turnkey supports the underlying cryptographic curve, swapping is possible. One can also envision a verifiable bridge built on Turnkey using these types of primitives. Because Turnkey is programmatic, the parties swapping assets do not have to be humans. Machines or agents can use it in the same way.

Non-custodial wallets and keys

Non-custodial wallets and keys are a special case of offchain authorization, but deserve to be highlighted because it is such a common use-case: how can a business create wallets or keys for its end-users without being exposed to the private keys themselves or be in a position to unilaterally use them? Turnkey solves this problem with Sub-Organizations and the flexibility of the Policy Engine. As outlined in
Embedded Wallets
, a Sub-Organization can be structured with exclusive access (the end-user is the only Root User, ensuring complete control and ownership) or joint access (the business and the end-user have control over one Root User each, and they each must sign Turnkey Activities for them to be processed). Of course, Turnkey also allows for traditional custodial setups where a business creates and controls Sub-Organizations on behalf of their end-users programmatically.

Scoping permissions for AI agents

As with every credential, developers should follow the
Principle of Least Privilege
when building applications powered by agents — in most cases, an agent should be capable of taking specific, limited actions with a crypto wallet instead of having complete control. Turnkey's policy engine is a perfect tool for this.

Using Turnkey, an application developer can generate an API key that has scoped permissions. Specifically, an API key can be permissioned to only initiate transactions that interact with specific contracts or trade against certain asset pairs. Furthermore, the policy engine can be used to require additional approvals on a signature request. These approvals can come from humans or other agents.

Additionally, as our policy engine functionality continues to grow, it can be leveraged as a defensive mechanism against an AI Agent taking action beyond its allowed risk scope, for example limiting the total number of transactions in a time span or account value as stop-gaps.

As of the time of writing, Turnkey has broad support for EVM- and Solana-specific operations. We plan to expand functionality as necessary within these ecosystems and into many others.

Flexibility

Our data model doesn't make assumptions about the relationship between resources (Wallets, Private Keys) and Users. Policies are the link between the two and as a result this relationship can take the shapes you need. As evidenced with the previous swap example, there are a lot of workflows unlocked by this flexibility.

We've broken down Turnkey's functionality in atomic Activities. These Activities have an “action” type as well as a “resource”, similar to REST semantics (see
this breakdown
). This allows the creation of more succinct policies. For example:

A group of Users can create resources (Wallets, Private Keys) but not delete or use them

Another group of Users (which could be machines or agents) is exclusively granted privilege to sign payloads with existing Wallets and Private Keys

A third group is responsible for “disaster recovery”-style capabilities and is part of the Root Quorum of the organization

A Turnkey Organization can be home to:

a sophisticated VC fund securing millions of dollars of crypto assets with a strict Root Quorum and many redundant authenticators for each Root User,

a degen AI agent trading many low value memecoins as the sole Root User,

or a DAO coordinating on-chain voting or upgrading smart contracts.

Your imagination is the limit.

Verifiable transaction metadata available in Policies

For several ecosystems, Turnkey can parse unsigned transactions into metadata that can be leveraged inside of our policy engine (see
Ecosystem Overview
). This is relevant for authorization when it requires context about the to-be-signed transaction.

A couple of examples where this is relevant:

An organization wants to authorize a user or set of users to sign a transaction only if its amount is less than 10 SOL.

An organization wants to authorize a trader to sign a transaction so long as it is a
swap
call to Uniswap.

An organization wants to deny any transaction going to a list of known bad actors.

This need is most acute for payments integrations and wallet use-cases.

Best-in-class performance and scalability

As detailed in our
Architecture
, the Signer enclave is responsible for producing signatures. When a fleet of signers is provisioned, they can process many signatures in parallel, at near-native speed, because the key is reconstructed in enclave memory. We thus unlock use-cases which would otherwise be hard or impossible to build with current on-chain or MPC solutions. Thanks to the verifiable nature of Turnkey's enclaves, security and performance aren't at odds with each other.

Turnkey is capable of scaling to millions of wallets and keys because it is horizontally scalable: a bigger fleet of Signer enclaves can process more signing Activities. It is possible to process signing Activities in parallel because signatures do not mutate organization data, as discussed in
Architecture
. This is crucial to scale Turnkey going forward when we look to more complex setups where Turnkey runs in multiple geographies and jurisdictions.

Import and Export

We've chosen to offer secure key import and export functionality in Turnkey because we believe that vendor lock-in is predatory. Both are implemented as Turnkey activities and secured with asymmetric cryptography: we perform a Diffie-Hellman key exchange between secure enclaves and user-held keys (following HPKE aka
RFC 9180
). This is critical for users who come in with existing wallets, or users who wish to take their wallets with them. More about this in our documentation:
Import
and
Export

Built with Turnkey

The table below contains a list of verticals and a few products using Turnkey. We highlight the main reason why they picked Turnkey as their foundation.





Vertical





Customers





What Turnkey solves








Consumer Social




Droppp




Flexibility, performance (throughput)







Defi




Infinex
Trojan
Moonshot
Tria
Azura




Smooth UX with passkey auth, performance, cross-chain.







Dev tooling




Alchemy
Dynamic
ZeroDev
OneBalance




Low-level abstractions, performance







Gaming




Parallel
Faraway




Auth flexibility, API performance.







Music / NFTs




Medallion
Magic Eden




Flexibility







Payments




Bridge
Mural
Squads




Security (policy engine), API performance and reliability







Protocols




Mysten Labs
Aptos




Security (policy engine)







DePIN




DIMO




Flexible auth, security







RWAs




Superstate
Heron Finance
Superform




Security







Wallets




TipLink
Beranames




Multi-chain UX and security (policy engine)







AI agents




Spectral




Flexible API and authentication, bot-friendly, autonomous deployment







Prediction markets




Polymarket




Security (policy engine)




Sensitive workloads beyond key management

In this section we highlight what becomes possible with a few upgrades to Turnkey. Strap in!

Verifiable compute platform

Turnkey runs
QuorumOS
internally as a solution to run applications in secure enclaves. Today we do not let users run their own applications. Only Turnkey-authored applications (Policy Engine, Signer, Notarizer, Parser, TLS Fetcher) are exposed to our customers.

We have plans to open up our foundations and let anyone provide StageX-built binaries and run them. This opens up the design space for builders massively: the key-management-specific applications remain usable, but others can be built. This goes beyond key management.

Some examples of sensitive workloads that could benefit from moving to attestable environments are in the table below.





Critical software





Advantages to verifiability








AI Inference



Avoid
“Wizard of OZ”
agents (who are human actors pretending to be real AI agents).







Chain abstraction



Trustworthy cosigners for cross-chain resource locks (e.g.
OneBalance
).







Transaction construction



Users know that the unsigned transaction bytes are legitimate.







Transaction parsing



Provide accurate metadata about the effects of a transaction. This is critical for trusted wallet UX.







Oracles (data fetching)



Leverage external data without the overhead of full decentralization, on-chain or off-chain. Replace economic incentives with verifiability.







Blockchain nodes



Allow for private balance lookups, verifiable mempool inclusion, and more.







Blockchain L2 sequencers



Prove correct behavior and eliminate the need for challenge periods and economic incentives around them.







Identity verification



Prove no identity is leaked as part of the verification process.







VPN nodes



Guarantees privacy by proving that forwarded traffic isn't logged anywhere.







Exchanges



Ensure no malicious behavior (frontrunning), and create verifiable order books.







Web2 bridges



Prove the state of web2 (exchange balance, credit scores, X follower count) in web3.







PII Processing



Prove that processing does not leak or misuse PII (Personally Identifiable Information).




TPM 2.0 support

The Turnkey secure application stack is currently dependent on Nitro Enclaves but will soon target other TEEs. Many TPM 2.0 devices have direct network access, which means applications running on this type of hardware will have the ability to connect to remote hosts out-of-the-box. Any executable that targets Linux and can be built reproducibly will be able to be run directly into an attestable environment without modification.

With this in place, virtually all applications on the Internet would benefit from the increased transparency and verifiability afforded by the Turnkey stack.

Expansion of the policy engine

The Turnkey policy engine is already best-in-class but we are planning to expand beyond where we are today to unlock new use cases and product features:

Ability to fetch and expose external data such as price: enables notional value policies.

Time locks: enables dead-man switch policies.

Ability to provide a WASM blob run by the policy engine instead of writing in Turnkey's language: enables builders to leverage code they have already written.

Deeper ecosystem integrations: today our Parser can parse EVM and Solana transactions. We will expand the number of ecosystems over time.

Organization-specific provisioning

Using the flexibility of Quorum Sets, control over enclave application deployment and provisioning can be distributed across geographies and jurisdiction. Turnkey is well-positioned to enable complex customer-specific setups in the near future.

We've seen in
Foundations
that QuorumOS applications are deployed with configuration: each application has a QOS Manifest which describes it fully. There are two important Quorum Sets in a QOS Manifest:

The Manifest Set is a set of parties who must approve QOS manifests.

The Share Set is a set of parties who must post their share into a booted enclave, to provision it with its Quorum Key. The Quorum Public Key is also part of the QOS Manifest.

Using the flexibility offered by these quorum settings one can picture a wide variety of setups with different trade-offs:

Individual customers could choose to be part of the Manifest Set to ensure they review each QOS Manifest before it is deployed. We envision this to be a good fit for e.g. well-known code auditors or parties who attest to the build.

Individual customers could choose to be part of the Share Set to be part of the provisioning process. We envision this to be a good fit for parties who already have experience holding key material securely.

Multiple independent parties could participate in the process of approval or provisioning. This decentralization can happen across geographies, jurisdictions, and span multiple archetypes of parties (companies, individual developers, DAOs). Similar to Security Councils built around multisig wallets, a Turnkey application can distribute control because of the flexibility of Quorum Sets.

Using Quorum Sets to their fullest extent is only one way to distribute control. Another idea to accomplish customer-specific ciphertext is to do it at a level higher. Our Signer application could encrypt key material to an external key known to the customer only. This key material would have to be encrypted and injected into the Signer enclave once provisioned. The advantage of this scheme is its simplicity: one signer enclave can serve multiple customers. We essentially create a customer-specific provisioning process which can be verified by all.

Maximal crypto toolkit

As crypto matures and moves towards post-quantum crypto, more primitives will become available and adopted. We envision Turnkey to be a complete cryptography toolkit providing support for new MPC protocols, ZK proving, encryption, hash-based signatures, and more.

Execution in TEEs nicely complements on-chain primitives: on-chain protocols can verify TEE attestations, and TEEs can provide compute services to on-chain protocols. This symbiotic relationship unlocks an experimental mindset where builders can unblock themselves and try new things using TEEs first, then enshrine these primitives in distributed protocols if they are met with broad adoption. We're excited to provide this missing critical playground for the industry.

Thwarting supply chain risks once and for all

Several portions of the Turnkey stack should be distributed across a network of decentralized parties over time. Each of these is aimed at improving the security of the software supply chain, from code review, to builds, to runtime. Our end-game: thwart supply-chain risk once and for all.

Reviewing source code and dependencies

Modern application developers depend on external software dependencies to accelerate the development process and use well-vetted & secure methods of performing sensitive tasks. Unfortunately, there are simply too many dependencies to review well. As a result, most companies either:

Accept serious risks and don’t properly review their dependencies, or

Slow down their development by limiting the number of new dependencies that can be taken.

Fortunately, many organizations have significant overlap in the dependencies they rely on. As a result, reviews can (and should!) be shared widely & performed in public. We think that incentivizing this behavior via a decentralized network is a critical initiative for the security of crypto at large.

Code review of internal applications is another critical behavior. Verifiable software (via remote attestation) is critical to ensuring transparent, high-security applications. But if those applications themselves simply contain malicious or accidental security issues, what good is verification?

It seems foolish to continue with the current model where every critical piece of software incurs a heavy tax (both in terms of money and time) to be “trusted”. Very often this comes in the form of expensive certification processes and lengthy audits. We can cut the middleman, improve efficiency, and provide a lot more transparency.

Reproducing builds

We envision a network of builders who are incentivized to compile and sign artifacts which are eventually executed on a verifiable stack. A diverse array of builders attesting that a particular bundle of source code (and its dependencies) yields an artifact with a particular hash will be an important vote of confidence, and critical to the verifiability of Turnkey-deployed applications.

Providing compute resources

Individual consumers of verifiable applications likely have different opinions on:

Which geographies and jurisdictions they want to be deployed in.

Which types of TEEs or hardware providers they trust.

Which operators they trust.

For this reason, we expect to have a wide array of compute providers running one or more verifiable applications on a specific kind of hardware in a specific location. These providers are one side of a market; the other side is composed of builders looking to use verifiable compute resources.

Conclusion

Unlike most whitepapers, we are not talking about hypotheticals. Turnkey has been operating for 2+ years, securing millions of wallets. Ready to build?
Sign up for Turnkey
, check out
our documentation
, and give us a whirl.

If you want to say hello, that's okay too:
hello@turnkey.com




While not yet practical at the time of writing (Feb 2025), solutions to the problem of cross-L2 credential updates exist in prototype form. See Vitalik's description of
keystores
. One can imagine a rollup dedicated to credential management (see
Dedicated minimal rollup for keystores
and
key.space
) or a dedicated keystore contract on L1 read by other L2s (see

L1SLOAD

). These solutions require deployment and wide adoption so they're not practical today, but may become practical soon given the fast developments in the interoperability space.





















