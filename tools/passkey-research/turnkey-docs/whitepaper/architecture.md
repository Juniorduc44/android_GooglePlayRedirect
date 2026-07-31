# Source: https://whitepaper.turnkey.com/architecture/










Turnkey Whitepaper

















Index






Principles






Foundations






Architecture






Applications




















Turnkey's Architecture

Turnkey Team

January 2025

Abstract

In
Verifiable Foundations
we explained how Turnkey runs binaries inside of Trusted Execution Environments (TEEs). StageX provides reproducible builds to go from source code to stable binaries, and QuorumOS provides a base secure OS to provision StageX binaries into cloud TEEs. We now take an engineering-focused tour of Turnkey as it is running today. We explain the roles and responsibilities of the different applications running inside secure enclaves, as well as what's needed
around
these applications to run them at scale and expose them to the outside world securely.

Below we detail our ambitious threat model: verifiable components are trusted, everything else isn't. We've built Turnkey to be a provably secure key management system, where anything touching user private key material is implemented within secure enclaves, in trusted space, hence verifiable. Our production infrastructure runs five main types of enclave applications (jump to
our architecture diagram
if you want to skip to the final picture):

The Policy Engine enclave is responsible for end-user request parsing, authentication, and authorization. For maximum flexibility we've implemented authorization with a policy engine and a simple yet expressive language.

The Notarizer


enclave guarantees the integrity of critical user data.

The Signer creates cryptographic keys and signs transactions.

The Parser extracts metadata from unsigned transactions.

The TLS Fetcher fetches content securely via TLS and produces portable proofs such that other enclaves can trust the TLS responses. The TLS Fetcher is the only enclave with access to external connectivity (at layer 4), because it uses

qos_net


By the end of this document you should understand how Turnkey implements key management in practice, from bottom to top, and why its verifiable foundations are so vital to its design. With this in mind you'll be able to jump to
Applications Beyond Key Management
where we explain where Turnkey goes from where it is today from a technology and product perspective.

Threat model

Turnkey was built with a wickedly simple threat model:

We consider enclave applications and their Quorum Sets
trusted

Everything else is considered
untrusted

This threat model minimizes our Trusted Computing Base (
TCB
): the less we have to trust the easier it is to reason about security, and the easier it is to strengthen over time.

Note that it
does not mean untrusted components are insecure
. For example, we consider our database untrusted. As a result, even a malicious AWS admin or Turnkey admin modifying data doesn't break our security model! Another example is our DDoS protection measures: they are enforced outside of our secure enclaves, at the ingress layer, which we consider untrusted. This means Turnkey is secure even without these protections. We have them in place because
Defense in Depth
is a critical strategy in any security system, but do not rely on them to guarantee the security of enclave-held private keys.

The main difference between code running within enclave applications and code running outside is the provable nature of it. By leveraging remote attestations, anyone can verify that security measures (in the form of software checks) are indeed in place where they should be when they are implemented within secure enclaves. This isn't the case when they are implemented in software running outside of secure enclaves: one must rely on formal security audits and insiders to get an accurate picture of the system and its inner workings. Another major difference between enclave code and non-enclave code is the set of constraints around it: enclave applications are self-contained binaries with no outside dependencies. We choose to write all enclave applications in Rust. This helps reduce their surface dramatically.

So how do we choose what must and must not be run within secure enclaves? We've architected Turnkey to be a provably secure
key management
system. Our security north star: if something can potentially impact user private keys, it must be implemented within secure enclaves, in
trusted
space. Otherwise, it can be done outside, in
untrusted
space.

System Topology

Turnkey is an API product


. When clients send requests to Turnkey they hit an API Gateway component over a secure connection. Requests to Turnkey are standard HTTP requests.

Once a request ingresses into Turnkey infrastructure the rest of the components use gRPC to communicate internally: the API Gateway converts HTTP requests into gRPC requests, and the Coordinator routes requests to enclave applications. If multiple enclaves need to be called, the coordinator handles this as well, by calling the first enclave, then the second enclave. Enclaves cannot send requests to other enclaves. Topologically they're leaves of the call graph.




Turnkey topology with enclaves as leaves

The Coordinator is also responsible for DB interactions: it can read and write to a datastore as needed.

Enclave apps are highlighted in green to show that they're the only verifiable (and thus, trusted) components in this diagram. At this point you may be wondering:

How can user requests travel through untrusted components? Can't they be modified by the API Gateway or the coordinator before reaching enclaves?

Is it safe to persist data in an untrusted component? How can we prevent data from being modified by an administrator for example?

We answer these questions and more in the rest of this document.

Organization

Organizations are data containers encapsulating signing resources (Wallets and Private keys) as well as data relevant for
authentication and authorization
of their usage (e.g. Users, Policies). Concretely, Organizations are objects with the following top-level fields:

version
: organization data schema version. This version changes when the organization data schema changes. For example, adding an “email” field to each user.

uuid
: a unique identifier (we use
UUIDv4

name
: name of the organization

users
: a list of Users. Each User has one or more ways to authenticate into the organization. See
Authentication
below.

rootQuorum
: a list of user IDs and a threshold which defines the Root Quorum. See
Root Users and Quorum

invitations
: a list of pending Invitations into the organization.

policies
: a list of Policies.

tags
: a list of Tags. Tags can apply to Private Keys, Wallets, or Users and are a convenient way to group resources to be able to refer to them consistently in Policies.

privateKeys
: collection of Private Keys.

wallets
: collection of Wallets. These are HD wallets compliant with BIP39



When an Organization is stored we serialize it to JSON


and it looks like the following:


"version": "15.0",

"uuid": "d12b...7f6b",

"name": "My Organization",

"users": [...],

"rootQuorum": {...},

"invitations": [...],

"policies": [...],

"tags": [...],

"privateKeys": [...],

"wallets": [...]


The rest of this document will reference Organizations and their data fields with a capital letter to distinguish them from the common nouns. For example, if you read about Wallets, we're referring to Turnkey Wallets defined inside of Organization data. The term “wallet” without a capital letter can then refer to an external user wallet such as Metamask, or a physical wallet holding cash or cards.

Activities and Queries

Now that we've introduced the concept of an Organization, let's talk about the two classes of requests supported by the Turnkey API: Activities and Queries.

Queries are read-only requests made to Turnkey. They are considered non-critical because they do not mutate the state of the organization, and do not allow usage of Wallets or Private Keys. All queries can be executed outside of secure enclaves. Examples of queries are “get user details” or “list policies”.

Activity requests represent critical actions within an Organization. There are roughly three categories to consider:

Genesis: creation of a new organization.

Resource management: creation, update, or deletion of Users, Policies, Tags, Private Keys or Wallets.

Signing resource usage: produce new signature, import/export Private Keys, import/export Wallets.

Activities define atomic operations with typed inputs (we often talk about activity “params”), and outputs (activity “results”). To allow safe upgrades over time in either params or result shapes, Activities are versioned


. Because of how critical activities are, they must be verifiably processed, in trusted space, within secure enclaves. We explain how we pull this off below.

Authentication and Authorization

Request Authentication (and Authorization) are core to the security of Turnkey. Request
Authentication
answers the question “who made this request?” while
Authorization
answers the question “is this authenticated User authorized to perform this operation?”.

Authentication

The threats we want to defend against in the context of request
authentication
are:

MITM attack: an attacker with control of an untrusted component modifies authenticated requests bodies before they reach enclaves

Request forgery: an attacker with control of an untrusted component creates new requests from within Turnkey's infrastructure

Replay attacks: an attacker captures and replays previous legitimate requests

To defend against MITM attacks and request forgery, requests to Turnkey are
signed by user-held key pairs
. These key pairs can be pure P256 key pairs (we call these “API keys”) or webauthn authenticators in the form of passkeys or hardware security keys (we call these “authenticators”) or secp256k1 key pairs associated with a crypto address (we call these “external wallets”). These signatures sign over the full request body and are attached to HTTP requests as HTTP headers. We refer to these signatures as “stamps”.

To defend against replay attacks we mandate that each request to Turnkey must contain a timestamp as part of the signed payload. Enclaves check this timestamp against their internal secure source of time and reject requests if their timestamp is too old or in the future.

In summary HTTP requests to Turnkey look like the following:

POST /api/v1/submit/create_users

Host: api.turnkey.com

X-Stamp: <signature over body>


"timestampMs": "<timestamp>",

"organizationId": "<organization ID>",

"type": "ACTIVITY_TYPE_CREATE_USERS",

"params": {

<activity-dependent params>



We're left with an important question: how do enclaves determine which key pairs are user-held?

The answer to this question is our concept of Organization which we've introduced above. Turnkey Organizations hold the definition of Users and Policy. Given a request and the correct Organization data (which can be loaded by ID, since the request contains an Organization ID), an enclave can independently determine who the request is from by verifying the request signature and looking up the associated public key inside of Organization data. If the public key is unknown, the request is denied. If the public key is attached to an existing organization User, the request is authenticated successfully.

Authorization

Once a request is authenticated, we have answered “who requested this?”. The answer comes in the form of a User ID within a Turnkey Organization. Once we've established that a particular User has placed a request, authorization needs to take place and answer the question: “can this user perform this action?”. The answer needs to be a definite “yes” or “no”. This is where Policies come in


. We've designed Turnkey to be flexible enough to support granular, programmatic permissions.

Policies

Turnkey Policies are part of Organization data. Concretely a policy looks like the following:


"policyName": "Allow user MY_USER to create wallets",

"condition": "activity.resource == 'WALLET' && activity.action == 'CREATE'",

"consensus": "approvers.any(user, user.id == 'MY_USER')",

"effect": "EFFECT_ALLOW"


policyName
is a field with a human-readable description of what the policy does or why it was put in place.

Condition
is an expression to codify the scope of the policy. In other words: when it applies. If this expression evaluates to true, the policy applies. In the example above, the policy applies when the activity is a “create wallet” activity.

consensus
is an expression codifying “who” must sign an activity for the policy to apply. In the example above the policy applies if the user
MY_USER
approves the activity.

effect
indicates the effect of the policy.
EFFECT_ALLOW
will allow the activity to be processed,
EFFECT_DENY
will reject it.

The expression language we chose for Turnkey policies (
condition
and
consensus
fields) is a simple yet expressive language based on
Google's CEL
to provide the convenience of common infix operators such as
, or
==
. A full list of operators and grammar functions can be found
in our documentation

Policies can be managed programmatically: creating, updating or deleting a policy is done with a Turnkey Activity. All policies are Organization resources and all of them are evaluated when a request is processed. Note that Policy security is extremely important because it is an easy attack vector for Users of an Organization. If strict policies can be weakened or removed, the integrity of authorization falls apart. We'll see in later sections how verifiable Activity processing is architected and ensures the integrity of Policies and other Organization resources.

Combining policies

Because we have multiple types of outcomes (
ALLOW
DENY
) it is possible for two policies to evaluate to different, conflicting outcomes. For example:

ALLOW
policy with condition
activity.resource == 'WALLET' && activity.action == 'SIGN'

DENY
policy with condition
wallet.id == 'MY_WALLET'

If a request to sign with
MY_WALLET
is evaluated, both policies will trigger: the first one results in
ALLOW
, the second in
DENY

To resolve conflicts we have the following rules in place:

An Activity is not allowed unless explicitly
ALLOW
ed by one or more Policies

Any explicit
DENY
outcome is final and results in the denial of the Activity, even if it is explicitly
ALLOW
ed by other policies in the Organization. In other words,
DENY
takes precedence over
ALLOW

In the example above, an attempt to sign with any wallet would succeed (because the first policy
ALLOW
s it), but fails when the wallet ID is
MY_WALLET
(the two policies conflict, and
DENY
takes precedence).

Consensus

Consensus is an important feature of the policy engine because it enables multiple parties who do not trust each other to collaborate on critical actions. For example, a user can give permission to a trading bot, via policies, to perform signing activities. Without consensus this is potentially risky because the trading bot gains unilateral control of the signing resource. If it goes rogue, funds can be lost. A safer setup is a consensus setup where both the bot and the user have to agree on a signing action before it executes.

As its name indicates the
consensus
field is built to support consensus use-cases:

approvers.any(user, user.id == 'HUMAN_USER_ID') && approvers.any(user, user.id == 'BOT_USER_ID'
) would fulfill the trading bot use-case above

approvers.count() > 2
is a looser policy which triggers when any 2 distinct users approve an activity

To approve an activity, a user can either sign the activity payload directly, or use the
APPROVE_ACTIVITY
activity, which references an activity by its fingerprint (unique sha256 digest). Both methods are equivalent from a consensus point of view, and count as a single approval.

Root Users and Quorum

Organizations are initialized with a single User at creation time. This user must be able to perform activities within the organization to set it up correctly: invite other Users, add API Keys, create new Wallets, and so on. Given we deny activities unless explicitly allowed, we need a solution to facilitate the initial setup. This solution is the concept of Root Quorum. When initialized, the organization starts with a Root Quorum of exactly 1 user, the first User:

"rootQuorum": {

"threshold": 1,

"userIds": ["a241...4277"]


When Root Quorum is reached, the policy engine is bypassed (Organization policies are not evaluated) and the activity is automatically allowed. With Root Quorum approvals, any Turnkey activity can be performed, similar to a root user on a Unix system.

As the organization grows and matures the Root Quorum should evolve to be a multi-party quorum. Below, an example of a 2-out-of-3 Root Quorum.

"rootQuorum": {

"threshold": 2,

"userIds": ["a241...4277", "078d...14ea", "4af2...98cb"]


Two Users must approve unilateral actions, which is a much safer setup because it removes any single-point-of-failure: if a single User goes rogue, they're not able to take action, and if a single User loses access to their API keys or authenticators, the Organization still has enough Users left to approve critical activities, including activities to update the Root Quorum itself.

The Policy Engine Enclave

The critical nature of Authentication and Authorization is obvious: if something goes wrong there, the security of Turnkey is compromised. It should be no surprise that we build this in trusted space, within secure enclaves. More specifically, authentication and authorization logic is contained within our Policy Engine. Its only responsibility is a simple but crucial one: given a user request and a snapshot of Organization data, return a “yes” or “no” answer. This is where Policies are evaluated.




Policy Engine enclave

In the diagram above we depict an Activity request (with its signature, or “Stamp”), and an Organization (with its resources: Users, Policies, Wallets, Private Keys, etc) being fed as input to the Policy Engine, which responds with a signed decision. We call this decision a
Ruling
. The signature is produced with the Policy Engine's Quorum Key. A Ruling contains:

The organization ID and digest

The decision itself (
ALLOW
or
DENY

The Activity request (type, params)

A timestamp

A Ruling is tied to a particular point-in-time snapshot of an organization: indeed if a policy is later removed or added, or if a User rotates their API Key, the Ruling might change! Everything needed to make a decision is injected as input, which means the Policy Engine itself can be stateless


and run within a secure enclave.

Because Rulings are signed by a stable Quorum Key (the Policy Engine's), other components can hardcode the Policy Engine's Quorum Public Key and rely on Rulings and their signatures as proof that a request was successfully authenticated and authorized. This is a powerful building block: we've isolated the problem of “auth” to a single stateless computation which produces portable proofs. We'll see in the following sections how Rulings are used as inputs to other secure enclaves.

Organization Data Notarization

So far we've assumed that Organization data is fed to the Policy Engine faithfully. This isn't a correct assumption given both the database (which stores Organization data) and the Coordinator (which loads data from the database and calls enclave applications) are assumed “untrusted” in our threat model. In other words, we should consider the following attack scenarios:

Data tampering: a database admin modifies organization data directly

Downgrade attack: a database admin rolls back organization data to a previous point-in-time

Carried out successfully, these attacks would completely undermine authentication and authorization. With a successful tampering attack, an admin would be able to arbitrarily insert their public key in the organization data, gaining signing access to its resources (Wallets, Private Keys). With a downgrade attack, revoked API Keys or deleted Users get their previous level of access back.

To guarantee the integrity of organization data, an enclave must sign it. We call these signatures
Notarizations
. To guarantee that data is recent and thwart downgrade attacks we periodically re-sign Organization data to generate fresh Notarizations. Any old Notarization is considered invalid when it is older than a static threshold.

The enclave application creating, updating, and signing organization data is called the
Notarizer
. Requests to the Notarizer are authorized with Policy Engine Rulings:

Create and Update operations: The Notarizer enclave expects a valid Ruling to create or update organization data and produce new Notarization.

Refresh operation: The Notarizer expects a valid, existing Notarization when generating new Notarization.

Summarizing this section with a few diagrams, the Notarizer has three operations: Create, Update, and Refresh.




Notarizer Enclave (create operation)

The “Create” operation produces a new Organization and its associated Notarization.




Notarizer Enclave (update operation)

The “Update” operation is also authorized with a Policy Engine Ruling. Note the sha256 update: this means something changed inside of the organization data. In the example above a new User is added.




Notarizer Enclave (refresh operation)

Finally, the “Refresh” operation takes as input:

Organization data

A valid past Notarization (labelled “Old”) for this data. Remember: a notarization is a signature of the Organization data digest which includes a timestamp.

The output of the “Refresh” operation is the same, unmodified Organization data (note the sha256 does not change) and a new valid Notarization, labelled “New” because its timestamp is the current timestamp.

Refresh operations need to happen periodically for all inactive Organizations. Indeed, unless an Organization mutates its data periodically (resulting in updated Organization data and Notarization), the Notarization associated with an Organization becomes stale, and thus invalid once it is past our replay protection threshold.

The number of Refresh operations scale linearly with the number of Organizations in our system, which is unsustainable at scale. To address this challenge we have designed a Merkle-tree based validation system for Notarizations and repudiation of old data. See
Appendix: scaling Verifiable Data
for more details.

Tackling organization data growth

Because Organization data is a monolithic object and used as input/output value in/out of enclave applications, an Organization cannot grow indefinitely (see
Resource Limits
).

To allow Organizations to scale to millions of Private Keys and Wallets we've introduced the concept of
Sub-Organization
. A Sub-Organization is just like an Organization: it's an independent data container with the same set of fields as a normal Organization. The only difference is a
parentOrganizationUuid
field which links the Sub-Organization to its parent Organization


. This is a useful primitive to model end-users: when a business uses Turnkey as a wallet provider, each end-user can be provisioned with its own Sub-Organization. The business can scale to millions of users and wallets without continuously growing its parent Organization data size. Another crucial aspect of this design: end-users are guaranteed that their Private Keys and Wallets are self-contained, kept intact, and isolated from the business or other end-users. See
wallet.tx.xyz
for a live demonstration.

Verifiable Activity processing

We now have the necessary knowledge to fully grasp Activity processing: it is based on a bi-directional dependency


between the Notarizer and Policy Engine enclaves.




Verifiable activity processing

The Policy Engine relies on the Notarizer to ensure the integrity of organization data it uses as input. A valid and current Notarization is a portable proof that Organization data is intact and current.

The Notarizer relies on the Policy Engine for authentication and authorization. A valid and current Ruling is a portable proof that an Activity was authenticated and authorized at a given point in time, for a particular snapshot of an Organization's data.

Finally, the Notarizer relies on itself as well: it requires a valid and current Notarization to process activities.

In the picture below we show the sequence of events from “genesis” of an Organization: the first activity is a “Create Org” activity, resulting in “Org State 0”. A new activity (“Activity 0”) is then requested and results in “Org State 1”. And so on: “Activity N” and "Org State N" result in “Org State N+1” after processing.




Organization state transition through activities, from genesis

It's worth noting that all of the data in the diagram above is signed by trusted entities

10


Organization data is signed by the Notarizer

Rulings are signed by the Policy Engine

Activities are signed by known User public keys, which we trust because they are part of signed Organization data.

By running the Notarizer and Policy Engine enclaves in trusted space we have verifiable authentication, verifiable authorization, and verifiable Organization data. We now introduce a third, very important enclave: the Signer.

The Signer enclave

Turnkey is a key management solution and so far we have not talked about Wallets or Private Keys much. We have mentioned that they are part of Organization data, and that should scare you: are we storing private keys and wallet seeds in plaintext inside of Organization data? How can this be safe?

The Signer enclave creates new Private Keys and Wallets, and produces signatures. These actions are implemented as Activities (create wallet, create private key, sign payload), secured using the previously introduced enclaves.

Key generation or usage requires a valid Policy Engine Ruling and a valid, notarized Organization. This ensures all Signer actions are user-initiated and based on valid and recent snapshot of an Organization.

Key Generation

Key generation requires the use of the Nitro NSM

11

to use secure entropy. Once key material is generated (in the form of a raw Private Key, or a Wallet seed), it is
encrypted to the Signer's Quorum Key
and returned. Because the Signer Quorum Key is never reconstructed outside secure enclaves, the key material never exists in plaintext form outside our trusted, verifiable boundaries. The Notarizer inserts this encrypted key material inside of Organization data following its normal requirements: a valid Ruling and Organization are required. Once Organization data is updated, the key material is ready to be used.

Key generation is crucial and thus verifiable. Because the Signer runs on top of Turnkey's
Verifiable Foundations
, it is possible to walk back from a
Boot Proof
all the way to the source code to verify entropy sourcing and encryption are performed correctly.

A visual diagram of key generation is given below, showing the signer producing key material, and the Notarizer using this signed output to update organization data.




Signer enclave (key generation)

It's worth noting that because all enclave outputs and inputs are signed with Quorum Keys, it's safe for e.g. “Encrypted Key Material” to transit in untrusted space. The Notarizer enclave will reject encrypted key material with an invalid Signer signature, or if the key material wasn't created for the correct Ruling.

Key Usage (signing payloads)

Once an Organization contains encrypted key material, producing signed payloads is straightforward. The Signer is given a Ruling and an Organization containing the encrypted key material. The Signer verifies the validity of the Policy Engine Signature and Notarization, and decrypts the key material. The Policy Engine Ruling contains the activity params and thus, the payload to sign. The signed payload is produced and returned to the user.




Signer enclave (signing)

The output (“Signed Payload”) is not signed by the Signer Quorum Key because it is self validating: end-users can trivially verify the validity of signature against the message (provided as input) and the expected public key (previously generated by the Signer Enclave).

Key Import and Export

Because no untrusted component in our system can see imported or exported keys in plaintext, we've designed these flows with one-time encryption keys derived with HPKE (
RFC 9180
).

To import a key, the Signer creates a new one-shot encryption keypair, to which the user encrypts the imported key material.

To export a key, the reverse happens: the user creates a one-shot encryption keypair client-side

12

, to which the Signer encrypts the exported key material.

See our documentation on
import
and
export
for more details.

Key Deletion

Deletion is a critical action which modifies Organization data. It is implemented as a standard Turnkey activity and requires the same authentication and authorization checks as any other critical actions.

Transaction-aware Policies

We've seen that our Policy language supports the ability to restrict what Users can and can't do by activity type. Our “sign payload” activity can thus be broadly allowed or denied based on policies. But what if more granularity is needed? Common examples are deny-list based on sanctioned addresses (
DENY
if transaction targets a sanctioned address, otherwise
ALLOW
), or amount-based policies to reduce risk (
ALLOW
if transaction amount is under an arbitrary threshold, otherwise
DENY
).

Because we want to keep enclave applications focused and minimal, we decided to introduce a new enclave to solve this particular problem: the Parser enclave.

The Parser enclave

The Parser enclave is a simple enclave to parse unsigned transactions and extract important metadata. We currently support Solana and EVM transaction parsing.

The Parser Quorum Key signs the resulting metadata, which can then transit to the Policy Engine to inform its decisions. The signed transaction metadata returned by the Parser becomes extra “context” data which can be used inside of the Policy Engine.




Parser enclave (parse operation)

Organization Policies can use this metadata through dedicated namespaces to build transaction-aware policies. A namespace is nothing more than a unique keyword, associated with data inside of it. We have chosen
eth.tx
and
sol.tx
as the top-level namespaces containing metadata for Ethereum and Solana parsed transactions, respectively. Refer to
our Policy Engine language reference
for a complete inventory of the available metadata inside of each of them.

A concrete example: an
ALLOW
policy with the condition
eth.tx.chain_id == 11155111
would allow transactions as long as they're
Sepolia
transactions. For more policy examples, see
our documentation

Flexibility and extensibility of the policy language

We've seen above that our Parser enclave provides extra verifiable metadata to the Policy Engine, in the form of another namespace which policies can use. We envision many other types of data to be relevant and useful to write fine grained policies. We detail this in
Applications beyond Key Management

Verifiable TLS

Solving the external connectivity problem

Secure enclaves do not have the ability to contact the outside world directly. In
Verifiable Foundations
we've seen that enclaves are connected to their host by a

VSOCK

interface, but have not explained what
VSOCK
is, really. Put simply, a
VSOCK
is similar to a UNIX domain socket (
UDS
) but is used to communicate between hosts and virtual machines. A
VSOCK
connection has a context ID and a port. The context ID is analogous to an IP address in TCP/IP, and ports work as you would expect.




Enclave
VSOCK
connection

Typically an enclave application binds to its context ID and a chosen port, listening for host connections and requests. The host client forwards requests it receives from the network to the enclave application by connecting to the right context ID and port.

To make outbound requests from inside an enclave application we need to do this in reverse: the host has to listen for requests made by the enclave application. When the enclave makes a request to open a connection, the host-side proxy can connect to the right target because it has network access. Similarly, read or write requests can be made by the enclave, and the proxy will act on the enclave's behalf and call read or write on the real TCP connections it holds.




Enclave
VSOCK
proxy

The proxy component in the diagram above provides external connectivity
at the TCP level only
(aka “layer 4” or
“transport layer”
). More exactly, the proxy’s interface is composed of three operations:

Open
connection: Open a new TCP connection to a target IP address

Read
from connection: Read N bytes from an existing TCP connection

Write
to connection: Write N bytes to an existing TCP connection

TLS on top of TCP

All enclave applications at Turnkey are written in Rust. In Rust,
Read
and
Write
are standard library traits:

std::io::Read


std::io::Write

. The most popular pure-Rust TLS crate,
Rustls
, works with these traits to implement TLS: users of the library provide a connection object which implements
Read
and
Write
traits, and Rustls uses that connection object to implement TLS handshakes, request encryption, and response decryption on top. This is explained in more detail
in their documentation
. We use this to our advantage by implementing these traits with a custom struct: upon receiving a call to
read
or
write
our trait-implementing struct calls the host-side proxy to read or write from already-established TCP connections.

If the proxy isn’t honest (this is in our threat model because the proxy runs outside of the secure enclave, in untrusted space), the TCP packets could be routed to the wrong remote host, but that would cause TLS certificate verification to fail. The proxy can also choose to censor and refuse to forward packets. This will be detected by the enclave as well. Finally, we’ve already seen that the proxy cannot decrypt or change TCP packets because TLS encrypts traffic with session keys, and these session keys are created and kept in our secure enclave.

The TLS fetcher enclave

We've named the secure enclave capable of making verifiable TLS requests “TLS fetcher”. Its interface is very minimal: given a method (
POST
GET
) a host, a path, a collection of headers, and a request body, return a response (status, body, headers) and a timestamp.




TLS Fetcher enclave

The response is signed by the TLS Fetcher Quorum Key. It is a portable proof that a given URL returned some content at a specific time, which neatly resolves the long-standing challenge of providing non-repudiation to TLS responses

13


We use the TLS Fetcher to verifiably fetch OIDC configuration (see
this blog post
) and envision it to be an important building block for our future roadmap, which we talk about in
Applications Beyond Key Management

Complete Architecture Diagram

We've now completed our tour and introduced all the components inside of the trusted boundary. In this section we work up to a diagram showing all enclave applications and their connections in one picture, as well as a few new components which are outside of the trusted boundary.

Enclave trust relationships

We've seen in the previous sections that enclaves are provisioned with their own Quorum Key, and a common pattern for enclave-to-enclave communication is to return signed structures which can be used as portable proofs fed to other components, including other enclaves. Below we summarize the trust relationships between enclaves:

The Notarizer trusts the Policy Engine's “yes” or “no” decisions as well as key material generated by the Signer.

The Policy Engine trusts the Notarizer to create and update Organization data. It also trusts metadata coming from the Parser enclave, and TLS responses coming from the TLS Fetcher.

The Signer trusts the Policy Engine (for its decisions) and the Notarizer (for its notarizations of Organization data).

These trust relationships are only symbolic and do not mean that enclaves contact each other directly. Rather, it means that enclave applications hardcode each other's Quorum public keys, in order to ensure signed payloads are intact and authentic. For example, the Policy Engine's decisions come in the form of signed Ruling payloads. The Notarizer, by hardcoding the Policy Engine's Quorum public key, can check these Ruling and only accept the legitimate ones. It guarantees that a component in untrusted space such as the Coordinator can't modify a Ruling payload (it would invalidate the signature) or create a fake Ruling (the coordinator doesn't have access to the Policy Engine's Quorum Key).

These relationships are summarized in the diagram below:




Enclave trust relationships

Other important components

Notifier

This service powers our webhooks feature. See
our Activity webhook documentation

Heartbeat

This service schedules Organizations for a “refresh” operation when their notarization gets stale. The Heartbeat service enqueues “refresh tasks” in Redis, and the Updater picks them up.

Redis

A fast, lightweight datastore used to contain the set of refresh tasks from the heartbeat service. Tasks are dequeued by the updater. We use a redis
ZSET
sorted set
) to atomically add and pop refresh tasks.

Updater

This service is connected to trusted enclaves in the same way that the Coordinator is. It polls from SQS in case there are Activities to retry, and executes refresh tasks pushed to Redis by the Heartbeat service.

Complete diagram




Turnkey Architecture (full diagram)

Utility of App Proofs

In
Foundations
we've introduced the concept of Boot Proofs and App Proofs. Now that we've introduced the enclaves Turnkey has built we can talk about use-cases for App Proofs.

Note that at the time of writing (2025-02-03), Turnkey uses Boot Proofs internally to power enclave provisioning, but they are not exposed to external customers yet. We plan to produce and expose App Proofs in the near future. Please
get in touch with us
if you'd like to be a part of this effort or help us beta test this important feature.

In the table below we lay out some proof types along with the originating enclave and their purpose. This list isn't exhaustive!





Proof type





Originating Enclave





Purpose









SIGNATURE




Signer



Prove that a signature was performed within Turnkey.








ADDRESS_DERIVATION




Signer



Prove that an address originates from within Turnkey. Also known as “provenance proof”.








IMPORT_BUNDLE




Signer



Prove that an import bundle originates from a legitimate signer.








EXPORT_BUNDLE




Signer



Prove that an export bundle originates from a legitimate signer.








POLICY_OUTCOME




Policy Engine



Prove that an activity request was authorized or denied by Turnkey's Policy Engine.








NOTARIZATION




Notarizer



Prove that our notarizer enclave produced a particular snapshot of Organization data.








FETCH




TLS Fetcher



Prove that our TLS fetcher enclave fetched content from the given URL.








TRANSACTION_PARSING




Parser



Prove that a transaction was parsed into a given set of metadata.




Conclusion

Turnkey represents a significant advancement in key management. For the first time, the critical security claims of a key management provider are verifiable without audits or intermediaries. This is possible because Turnkey is built on verifiable foundations (see
Verifiable Foundations
). We've chosen the strictest threat model possible: anything that can touch funds stored on user key material needs to be built in trusted space. We trust what is verifiable, and do not trust what can't be.

This overarching principle led us to design key management around enclave applications responsible for well-defined functionality:

The Policy Engine authenticates and authorizes user requests

The Notarizer produces and modifies Organization data

The Signer creates and uses Private Keys and Wallets

The Parser extracts metadata from unsigned transactions

The TLS Fetcher makes secure requests to remote hosts

Each enclave application contributes to a unified system where authentication, authorization, and data integrity are not just ensured but verifiable by design. We now discuss the transformative possibilities of this architecture and its underlying foundations in
Applications Beyond Key Management

Appendix: Scaling Verifiable Data

Because Turnkey stores millions of Organizations, the load on the Notarizer enclave is significant: this enclave application (and the system around it, namely: the heartbeat service, Redis, and the Updater) must periodically refresh the Organizations which have not mutated their data recently.

To address this challenge we've developed a better system based on Merkle trees. Organization notarizations are part of a Merkle tree, and the root of this Merkle tree is signed (with a timestamp) periodically.

An organization can prove it is valid when it presents a recent-enough Notarization, or a notarization which is included into a fresh enough Merkle tree (with a fresh-enough root).

When an organization goes through long periods of inactivity, its membership into the Merkle tree remains valid, which means we do not need to periodically refresh this org's notarization.

For normally-active organizations, nothing changes: activity processing will result in updated Organization data and fresh notarizations. In other words, the Merkle tree is “additive” and is simply here to provide another, more efficient way to prove organization data freshness for inactive organizations.

More formally, the logic to check Organization data freshness has two steps:

Notarization Freshness Check
- If the latest Notarization for an Organization is sufficiently fresh, we are done, the data is considered fresh.

Merkle Tree Inclusion & Freshness Check
- If the Notarization Freshness Check fails, then the following check is performed:

Check if the Merkle Tree Inclusion Proof is valid. This proves the Notarization is part of the Merkle Tree

Check the signature payload of the root node of the Merkle tree, and ensure its timestamp is fresh enough.

The key advantage of this system is that these two mechanisms exhibit a symbiotic relationship, wherein the burden of one is greatly eased by the role of the other:

Introducing the Merkle Tree as an archival mechanism for stale Notarizations negates the need to perform continuous refreshes. Only the Merkle tree root node needs to be periodically timestamped and signed.

Keeping the Notarization Freshness check as a “first-pass check” allows to shield the Merkle Tree from the responsibility of accommodating Organizations which mutate at a very frequent rate. This works much like a
low-pass filter
in electronics, which filters out high-frequency signals.


We named this enclave application “Notarizer” because it acts as the software version of what a human notarizer does. It reviews, and provides a seal of authenticity verifiable by other parties.


We have a dashboard for admin purposes, hosted at app.turnkey.com. But this isn't where most of the production traffic flows.


You're probably wondering where wallet accounts are stored. Because wallet accounts are deterministically derived from seed, we only store the seed in organization data and re-derive accounts pre-signing.


We chose to use JSON for human-readability but we had to deal with non-determism: because of field ordering and whitespace, there are multiple valid JSON strings for a single in-memory JSON object. This is okay in most cases: as long as the signed serialized JSON can be parsed, two components can load JSON strings, verify signatures, and extract data. When deterministic serialization is needed internally we use
Borsh
instead of JSON.


For example,
ACTIVITY_TYPE_CREATE_USERS_V2
is the third version of our
CREATE_USERS
activity. The previous activity types were
ACTIVITY_TYPE_CREATE_USERS
and
ACTIVITY_TYPE_CREATE_USERS_V1
and had a different structure to their parameters or results. We support many activity versions into the past, maintaining a significant level of backwards compatibility.


Policies only apply to Activities. For read-only Queries, which are non-critical requests, authorization is much simpler: they're allowed as long as Authentication succeeds, which means any user within an organization has read-only access to its data. Over time we intend to create RBAC-style roles to scope visibility of organization resources when required.


Time is an exception: the Policy Engine must use its secure source of time to reject expired Activity requests (we consider any request older than 1hr expired). Injecting time as input would be insecure because we do not trust the coordinator in our threat model. Time needs to come from the NSM.


There are other non-data differences at the authorization level: (1) parent orgs can have read-only access to sub-org, (2) parent orgs can initiate authentication or recovery activities for their sub-organizations, (3) parent orgs are allowed to create sub-orgs while sub-orgs can't create sub-orgs (the organization “tree” has max depth of exactly one)


This “dependency” is implicit in the system design, rather than an outright API, or library type dependency. In this section we say “dependency” when we mean “trust relationship”. The actual mechanism for this is pinning of the Quorum public key: an enclave A trusts another enclave B when it pins B's Quorum public key and uses it to verify its responses. From an infrastructure or system point of view, enclaves cannot communicate with one another: enclave B's response has to be injected into requests for A by the coordinator.

10

The only exception to this is the initial Create Organization activity. It is signed by an untrusted key because it's signed by the Organization's first Root user (new by definition). This is a standard Trust-On-First-Use (TOFU) solution where we assume the initial User to be legitimate. This is safe because an Organization starts completely empty. If the first activity is indeed malicious, the attacker does not gain anything aside from access to brand new, empty Organization.

11

NSM stands for Nitro Secure Module. We built our initial implementation on top of AWS Nitro but hope to move towards being able to execute enclaves in any TPM 2.0 environment. TPM 2.0 has a well-defined API to source entropy inside of the secure environment:

getrandom


12

This can be done entirely with vanilla Javascript: these key pairs are standard P-256 key pairs. We also offer
SDK
abstractions to do this for apps and users who do not want to write code themselves.

13

Providing non-repudiation via other means has been tried before but has failed thus far. See
TLS Evidence
and
TLS sign





















