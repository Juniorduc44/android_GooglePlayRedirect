# Source: https://whitepaper.turnkey.com/principles/










Turnkey Whitepaper

















Index






Principles






Foundations






Architecture






Applications




















Key Management Re-Imagined from First Principles

Turnkey Team

January 2025

Abstract

Every transaction in crypto starts and ends with a private key. Creating, storing, managing, and using private keys is an extremely hard problem to solve. Since digital asset ownership and usage is all rooted in public key cryptography, solving this problem is an essential need for the industry.

Turnkey is a novel approach to key management built for the next wave of crypto applications and users. Our team is composed of industry veterans


who spent years in the trenches managing keys through multiple bull and bear cycles, securing hundreds of billions of dollars worth of crypto. The pain of struggling with non-optimal solutions has given our team the grit and hunger needed to invent a better solution for the future. We are now building the foundation we wish we had during those years.

Turnkey is low-level key management infrastructure that can be used by a business to automate transactions at scale, or to provide non-custodial wallets for end users. This includes everything from high-throughput payments, to smart contract management, to cross-chain consumer wallets, and more. The foundations we've laid are useful even beyond crypto. From the outside, this may seem like a simple product, but the power of Turnkey is in the details. We've built Turnkey to avoid many of the pitfalls of existing key management solutions. In this document we lay out the principles we've used to architect it:

Meet users where they are
: Most insiders pride themselves on being able to deal with the friction of crypto UX, as a badge of honor to distinguish themselves from the mainstream. We're betting on familiar authentication standards like passkeys, email auth, and OAuth to meet users where they are and drive the next wave of adoption.

Build on shared cryptographic primitives
: Transactions and assets are the wrong level of abstraction to start at in modern crypto. We've chosen to bet on curve-level operations and signing schemes because this is where the shared cryptographic fundamentals live.

Engineer for speed and scale
: High-throughput payments, cross-chain abstractions, and AI agents interacting with crypto demand readily available keys and lightning fast signing. We've designed Turnkey as a pure key management product to avoid doing anything superfluous. We deliver sub-100ms signing latency and scale to millions of wallets.

Assume everything is compromised until proven secure
: All sensitive actions run within our trusted, verifiable environment. We are securing the entire perimeter of the signing process rather than just key material. All requests to modify data are signed by a user-held authenticator, verified and processed entirely within secure enclaves.

Don't trust, verify
: We divided Turnkey's infrastructure into “trusted” and “untrusted” spaces. The Trusted space, where all critical software runs, is verifiable with remote attestations. This is an industry first.

Be pragmatic, not dogmatic
: Decentralizing portions of Turnkey will make sense in the long run. In the meantime, our priority is builders and their users.

Build a library, not a framework
: We built Turnkey as a library full of modular building blocks, rather than a restrictive framework. It is the Unix philosophy applied to key management.

By the end of this document you should be able to understand the motivations behind Turnkey's
Verifiable Foundations
and
Architecture
, and be ready to jump to our technical designs with that context in mind.

1. Meet users where they are

We will not onboard the next billion users or trillion machines with seed phrases. Despite improvements to wallets over the years, the cryptocurrency space is fundamentally limited by its ability to onboard new users


. Both novice and experienced users encounter issues with wallet creation and private key material management, in raw or mnemonic form. These challenges can lead to errors and unacceptable financial losses. Key management is anxiety-inducing for experienced users who are aware of the security pitfalls, and a source of friction and unknown risks for novice users.




Onboarding flow for a new Metamask user

While most insiders agree that crypto onboarding is too hard, they pride themselves on being able to deal with that friction. It's a badge of honor to distinguish themselves from the mainstream. Crypto veterans can bear the burden; others can't. Crypto veterans can reason about self-custody and security, others can't. Dealing with the stress and fear is just the “price to pay” to do business in crypto. Now is the time to evolve and get rid of this piece of our identity. Crypto shouldn't be hard to use — let's bring crypto mainstream.

For a long time the crypto industry struggled to scale. Transaction fees were an issue because of scarce block space, and apps struggled to offer consistent user experiences as a result. We now have robust decentralized infrastructure for transaction processing, and plenty of block space to use. Throughput has gotten better as well (Solana, ETH L2s). The next thing to scale is crypto's adoption.

Turnkey meets users where they are: we're betting on familiar authentication standards like
passkeys
email auth
, and
OAuth
. We're leaving passwords out for security reasons. Turnkey, as a principle, does not store any user authentication secret. Our authentication methods rely on public/private key cryptography. We store public keys only, and authentication happens with cryptographic signatures verified against them. To bridge traditional authentication protocols like email and OAuth to strong cryptographic key pairs we leverage Trusted Execution Environments (TEEs) to perform a key exchange. More on this in
Architecture

Turnkey is built to onboard users and machines alike. Passkeys, email and Oauth are designed for human end-users, but API keys are a great fit for machines and agents. Over time we expect servers, AI agents, and other kinds of infrastructure to authenticate to Turnkey and adopt crypto as their primary rails.

2. Build on shared cryptographic primitives

Traditional key management products struggle to keep up with new chains, ecosystems, and use-cases because they have overfit their APIs to asset-specific, transaction-level primitives. As a result, builders are hobbled by their custodians or key management providers. Although these may be simple abstractions for developers, it isn't possible to keep up with the explosion of ecosystems and use-cases without crumbling under technical debt and maintenance burden.

Our diagnosis: transactions and assets are the wrong level of abstraction to start at in modern crypto. A system designed specifically for Bitcoin might work well for handling Bitcoin transactions but will require significant rework to support Ethereum or Cosmos because of the differences in how transactions are structured and encoded. Instead, Turnkey operates at the curve level. Since Bitcoin, Ethereum, and Cosmos all use the Secp256k1 curve, Turnkey can support key generation and signing for these ecosystems without requiring asset-specific integrations.

We've chosen to bet on curve-level operations and signing schemes because this is where the shared cryptographic fundamentals live. Bitcoin, Ethereum, and Cosmos sign with Secp256k1. Solana, Polkadot, Stellar, Sei and Sui all sign using Ed25519. With two curves we cover the vast majority of crypto assets. We have the ability to easily add new curves, and we can support virtually all of them with fewer than 10 distinct curves total.

That said, to make integration easier, builders need more than curve-level operations to offer applications to their users. We approach asset and ecosystem-specific work with a
tiered approach
to make popular integrations easier. Higher tiers provide more robust abstractions, allowing for easier integration.

Tier 1
: Curve-level support. Cryptographic curves are our fundamental primitive, allowing Turnkey private keys to store and sign for any cryptocurrency that uses a supported curve. We currently support Secp256k1 and Ed25519 curves.

Tier 2
: Address derivation. Turnkey abstracts address generation, automatically deriving addresses for supported cryptocurrencies.

Tier 3
: Client-side SDK for transaction construction and signing. Our SDK provides tools and scripts to help in constructing and signing basic transactions, enabling an even smoother integration.

Tier 4
: Transaction parsing and policy creation. At our highest level of support, Turnkey offers the ability to parse transactions and define custom policies based on transaction parameters.

Curve-level support lets our customers move fast; you'll never be hard-blocked by Turnkey’s engineering team. Turnkey is built as extension-friendly infrastructure. We're betting on safe, broadly applicable cryptography first. We'll continuously go after more primitives as they become available and mature over time.

3. Engineer for speed and scale

Key management providers have historically cornered themselves because they’ve designed for low-throughput use cases. The emergence of high-throughput payments, cross-chain abstractions, and AI agents interacting with crypto demands readily available keys and lightning fast signing. Cold storage won't cut it. The largely “buy and hold” era of crypto is over.

We've designed Turnkey as a pure key management product to avoid doing anything superfluous. User private keys are generated and used inside attestable secure enclaves running
QuorumOS
, a minimal Linux kernel (to read more about our foundations, see
Verifiable Foundations
). Our enclave applications are written in Rust, and deployed on modern, secure hardware. They run unhampered, as close to native performance as it gets. As a result, we deliver sub-100ms signing latency, and this number includes all the ingress networking overhead, going in and out of our edge infrastructure.

Turnkey was built on modern infrastructure to horizontally scale to the needs of our customers. We designed key generation to be a low overhead, synchronous operation. Keys are created and ready to use in 100-200ms, end-to-end. As a result Turnkey is able to onboard end-users or machines at breakneck speeds and scales to hundreds of millions of wallets. This is what modern crypto infrastructure should feel like.

4. Assume everything is compromised until proven secure

Turnkey does more than just protect from private key theft. Aside from the flexibility and crypto-friendly design of our APIs, we offer markedly better security. The overwhelming majority of platforms that use words like “HSM”, “Enclave”, or “TEEs” are using off-the-shelf services like
SafeNet HSMs
AWS KMS
or
Google KMS
. These offer decryption or signing as a service without any validation or context about what is being decrypted or signed. A traditional Linux server makes requests to these secure services, telling them what to decrypt or sign, and they will always decrypt or sign. Hence an attacker doesn't need to compromise these secure services: compromising the traditional server is enough!

Modern crypto key security requires secure key storage of course, but we must extend security more broadly to key
usage
. Attackers do not need to steal private keys to steal crypto assets: malicious authentication, transaction parsing, or policy evaluation are enough to drain wallets.

While these off-the-shelf services will most likely stop private keys from being exfiltrated, they will not stop wallets from being drained. Once the funds are gone, the key being private no longer matters. It's too late: no use in closing the barn door after the horse has already run off.

One of our core beliefs is that the trusted environment must be smart enough to say “yes” or “no” with the full context of the end-user request. Otherwise we've simply shifted trust from the machine holding the key to a machine that communicates with it. And unfortunately, attackers always go after the weakest link.

Turnkey secures all sensitive actions with the same level of paranoia: the services generating private keys, producing signatures, parsing transactions, or evaluating policies all run within our trusted, verifiable environment (our
Trusted Computing Base
, or TCB, see next section for details). We are securing the entire
perimeter
of the signing process. An enclave verifies passkey signatures on user requests. An enclave parses unsigned transactions to extract metadata. An enclave enforces policies. An enclave signs with your private key. Data defining organizations, users, policies, wallets, and private keys are cryptographically signed so they cannot be modified or rolled back to a previous point in time. All requests to modify data (Turnkey “activities”) are signed by a user-held authenticator, verified and processed entirely within secure enclaves.

Our top-level goal is to guarantee no private key or wallet can be accessed maliciously. Software outside of our TCB must not be able to modify data, and must not be able to modify or trigger user activity requests. We assume it can be compromised.

We have engineered Turnkey organization data and activities to be unforgeable and immutable. We'll see in
Turnkey's Architecture
how this all comes together in greater detail.

5. Don't trust, verify

The crypto industry and the key management space in particular is full of wild and unverifiable security claims. We built Turnkey with transparency in mind: our security claims are real, tangible, and verifiable. We can prove we have nothing up our sleeves.

The standard approach to key management is to generate and manage keys hidden behind redundant layers of defense. Key management software and its operators are typically shrouded in secrecy and unaccountable for their actions.

Turnkey sets itself apart with an ambitious threat model which considers everything potentially compromised
by default
. We decide to trust only components that can be externally verified, and place our trust in
quorums
of operators to eliminate classic single-of-point-of-failure risks, all too common in our industry.




Trusted vs. Untrusted spaces

In order to achieve this, we divided Turnkey's infrastructure into “trusted” (Verifiable) and “untrusted” (Unverifiable) spaces.

“Untrusted” space exists as a bridge from the outside world to the trusted space. Our threat model assumes anything within this space (software, hardware, individuals) can be compromised. This includes edge infrastructure, ingress services, databases, configuration services, CI pipeline, deployment tooling, and so on. Untrusted space is secured in the traditional way: with best practices and defense-in-depth. Auditors and insiders can verify it runs correctly and safely with the right level of access, but regular users can't.

“Trusted” space contains critical software to create keys, authorize key usage, and use keys. Application binaries running in this space must be compiled with trusted tools, from trusted code, and approved by a quorum of operators. We secure the trusted domain with Trusted Execution Environments (“TEEs” aka “secure enclaves”), and anyone can verify what runs in Turnkey's secure enclaves through the process of remote attestation. Remote attestations are signed measurements provided by the underlying hardware. This allows anyone with an attestation to know with confidence that a specific piece of software is running on a specific machine. It is the basic building block for
verifiability
. We cover this in greater detail in
Verifiable Foundations

This “trusted” space is Turnkey's
Trusted Computing Base
(TCB). The ambitious bet we're making with Turnkey is to make our TCB open and verifiable. This is an industry first: the goal is for anyone to be able to verify key generation and usage independently. When in doubt, don’t trust—verify.

6. Be pragmatic, not dogmatic

Crypto moves slowly in the name of decentralization. Consensus needs to form across people and teams to alter how blockchains work at the protocol level. This is often the right process: enshrining something at the base layer requires careful design. In many ways it's irreversible, akin to etching a circuit during chip manufacturing.

Turnkey is a bet on verifiability as a supplement to full decentralization. A great example of this is authentication into Ethereum: Smart Wallets are now native (
EIP4337
), but it'll take a while before passkey verification (
RIP7212
) is supported natively across all Ethereum rollups and its base layer. And how long before passkey signature verification is available across Bitcoin, Solana, and others? In the meantime, Turnkey unlocks passkey authentication in a chain-agnostic way and powers crypto apps which would otherwise not exist.

The need for decentralization is a spectrum. It is low at the start and grows steadily as usage and value accrues.

We're pragmatic and not dogmatic about this. Decentralizing portions of Turnkey will make sense over time. And we track native primitives to offer the best integration possible where it makes sense (see our
AA guide
for example). In the meantime, our priority is builders and their users. We hope Turnkey can be leveraged as a lab for the crypto industry to gauge which primitives are the most useful to end-users, and thus worthy of the effort to enshrine them in base protocols.

7. Build a library, not a framework

Turnkey is engineered to be usable infrastructure. We're operators, not custodians. We allow developers to create and manage cryptographic private keys, rather than opening accounts and managing funds for them.

Only the person or machine in possession of authenticators (in the form of passkeys or bare API keys) can sign Turnkey activities and use the associated wallets or private keys. This setup is flexible enough to create experiences where:

End-users hold authenticators directly and are in full control of their wallets (non-custodial).

A business creates and holds authenticators on behalf of its end-users or itself (custodial setup).

Business and end-users must cooperate on signing activities, leveraging the consensus features of our policy engine.

We do not make assumptions about who holds credentials and how they use Turnkey. Our goal is to provide safe, flexible, and performant foundations to build in crypto.

In terms of approach, we built Turnkey as a library full of modular building blocks, rather than an all-encompassing framework you have to fit into. You can use Turnkey standalone, or combine Turnkey with other ecosystem primitives, or even other providers. Turnkey can be combined with AA wallets, Gnosis safes, or a Bitcoin multisig. Turnkey can be used to deploy smart contracts. It can be used to sponsor gas, bridge funds, or safeguard domain names.

We provide the ability to
import
and
export
your keys so you're free to come and go anytime. We envision Turnkey to be useful, but we don't mandate it to be used the way we like. You can use Turnkey the way you intend to, using the parts of our API that make the most sense for your use case.

Another way to think about Turnkey: it is the Unix tool philosophy applied to key management. We've gone to great lengths to solve key management the best we can, and can't wait to see what gets built on top, crypto or not: The verifiable foundations we've laid to build Turnkey are useful infrastructure even beyond crypto. More on this in
Applications Beyond Key Management

Conclusion

We started by looking at the UX problem in crypto and saw why now is the time to re-imagine key management with end-users and machines in mind. We've placed our bet on asymmetric cryptography, passkeys, email and OAuth to meet users where they are, removing decade-old friction and stigma associated with crypto onboarding.

We've detailed our ambitious security model and explained our belief that transparency and multi-party controls are vital to secure the critical portions of Turnkey. We've also seen why we've chosen TEEs as building blocks: the process of remote attestation is how you can keep us accountable.

Design-wise we believe curve-level operations are the correct building blocks for modern crypto-asset operations. Turnkey is built with low-latency and high-throughput in mind to enable bleeding-edge crypto apps that would otherwise not exist.

Turnkey operates on top of, and composes with, existing decentralized infrastructure. Our loyalty lives with builders and their users. We hope Turnkey can be used as a lab to determine which primitives are worth enshrining in existing decentralized protocols.

Finally, we're an infrastructure platform, not a custodian. Turnkey is a sharp, focused set of tools to build in crypto. Think library, not framework.

Retrofitting our vision into existing key management systems is near-impossible. This is why we're on this journey. With that, let's dive into the nitty-gritty details of our foundations with
Verifiable Foundations


The Turnkey team includes people who have worked at Coinbase, Kraken, BitGo, Fireblocks, and within the US government defense industry.


See
this
this
, or
this
for academic papers on the topic.





















