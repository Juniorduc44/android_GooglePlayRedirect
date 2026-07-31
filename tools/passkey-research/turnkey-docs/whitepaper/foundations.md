# Source: https://whitepaper.turnkey.com/foundations/










Turnkey Whitepaper

















Index






Principles






Foundations






Architecture






Applications




















Verifiable Foundations

Turnkey Team

January 2025

Abstract

Here we dive straight into the depth of Turnkey's foundations and how they allow for Turnkey applications to be independently verifiable.

We briefly explain what Trusted Execution Environments (“TEEs”) are and how we use them. On top of strong isolation and confidentiality guarantees, TEEs can prove the software they run through remote attestations. These attestations contain signed measurements from the underlying platform provider (“Platform Configuration Registers”, or “PCRs”).

We introduce
QuorumOS
(“QOS”), a new minimal, open-source operating system engineered for verifiability. QuorumOS is the operating system run by TEEs, acting as the glue between provider-specific hardware and provider-agnostic applications. QuorumOS proves that a TEE is running a specific application by providing QOS Manifests to attestations. This lets anyone verify that an enclave is running the correct software (QuorumOS itself), and that QuorumOS is running the correct application binary (as specified in the QOS Manifest).

We explain why remote attestations require reproducible builds and introduce
StageX
, a new Linux distro which powers all secure builds at Turnkey today. StageX provides reproducible builds to ensure application binaries can be reproduced, agreed upon, and certified by multiple parties. StageX guarantees a 1-to-1, immutable relationship between human-readable source code and the resulting machine-executable artifacts running inside of QuorumOS.

Finally we introduce the concept of Boot Proofs and App Proofs and explain how remote attestations, QuorumOS, and StageX combined together yield full verifiability of the software running inside TEEs. The entire operating system (QuorumOS itself) as well as applications within it are verifiable all the way down to the exact source code.

What we present here is a major step forward compared to the current opaque status quo where the security of critical software components can't be proved and relies instead on huge audit and compliance industries.

TEEs and AWS Nitro enclaves

In this section we explain what Trusted Execution Environments (“TEEs”) are and how Turnkey uses them. On top of strong isolation and confidentiality guarantees, TEEs provide access to secure sources of time and entropy. The most interesting feature of TEEs is Remote Attestations, which we introduce in a dedicated section. Remote attestations are how TEEs can prove the software they run, with signed measurements from the underlying platform provider (“Platform Configuration Registers”, or “PCRs”).

Overview

Trusted Execution Environments are dedicated areas within a device or system to provide extra confidentiality and integrity guarantees when running secure workloads. TEEs are paired with a “host” system which calls into the secure area to invoke functionality hosted within it. TEEs are designed to protect against malware and OS-level vulnerabilities on the host system, which means data and computation in TEEs remain confidential and intact.

Historically TEEs have been leveraged for many use cases and they come in a wide variety of hardware. They are governed by many different standards (
TPM
GlobalPlatform
JavaCard
FIPS
). Mobile device authentication on iOS (with the
Apple Secure Enclave
) or Android (with
Trusty
) is the most well-known use case for TEEs because it underpins fingerprint and FaceID authentication, used daily by hundreds of millions of end-users. A good survey of TEEs and their classification is available in
Trusted Execution Environments (Shepherd, Markantonakis)

Turnkey uses
AWS Nitro Enclaves
as its primary platform today


. Over time we'll expand our deployment capabilities to any TPM 2.0 compatible providers such as Google Cloud Platform (
Shielded VMs
), Azure (
Confidential Computing
), or on-prem deployments. For the rest of this document we'll use AWS-specific language to make the explanation more concrete, but the concepts carry over to other vendors.

AWS Nitro enclaves can be visualized in the following diagram:




AWS Nitro enclave components

On the left we have the “network”, which is to say: the outside world. Other applications or humans connect to the Nitro Host over TCP or UDP connections. A Nitro Host is “just” another EC2 instance!

The Nitro Host runs a Host VM, depicted in blue, running its own operating system. What makes a Nitro Host special is its access to the
Nitro Controller and Nitro Hypervisor
. The Nitro Controller lets the Nitro Host issue commands like “start” or “stop” to boot and terminate Nitro enclaves


. New nitro enclaves are created from an Enclave Image File (EIF)


. Booting a new Nitro enclave results in a new isolated VM with its own resources (RAM, CPU), running the specified EIF, on the same physical host. We label the resulting VM “Enclave VM”, in green.

Running heavy workloads in TEEs is generally a challenge because of the physical limitations of secure elements. However, Nitro enclaves can be provisioned with an arbitrary amount of CPU and memory (as long as it's less than the total amount of CPU and memory available on the host). Thanks to this design decision, secure enclaves can run arbitrary applications and consume as much memory and CPU as the host system is willing to allocate.

One crucial security feature is the VSOCK link between the host VM and the enclave VM.
VSOCK
is a socket-like protocol to facilitate VM-to-VM communication. This VSOCK link is the only way in and out of the enclave. Nitro enclaves do not have any other data I/O, and in particular, they do not have any networking capabilities.

Finally, the Nitro Card, in orange, is a physical resource only accessible to the Nitro Enclave. The Nitro Secure Module (NSM) connects to it


to provide a secure source of entropy and time, which the enclave VM can access.

Secure entropy is crucial not only for key generation, but also relevant to generate random nonces at signing time, or random Initialization Vectors (IVs) when encrypting data.

The secure source of time is used to protect against replay and downgrade attacks. It's also used to verify the validity of SSL certificates, for example. Time is a natural monotonically increasing counter so it can serve as a secure nonce or watermark when required.

Summarizing: TEEs are dedicated areas within a device or system to provide confidentiality and integrity guarantees when running secure workloads.

Turnkey uses AWS Nitro enclaves, a specific type of TEEs, which are isolated (virtual) machines provisioned with their own CPU and memory resources. They have the following important properties:

A Nitro enclave is
stateless
and does not have the ability to write to a persistent disk or cache. Its only form of persistence is volatile memory (RAM), cleared on every restart



A Nitro enclave is
not connected to the network
. The only networking element attached to a Nitro enclave is a VSOCK interface to enable communication with the enclave host.

A Nitro enclave has access to an independent
secure source of entropy and time
via the Nitro Security Module (“NSM”)

Finally, it's worth double clicking on one key feature of Nitro enclaves so far:
remote attestations
(AWS specific docs
here
). Within AWS, the Nitro Card measures and signs Platform Configuration Registers (PCRs) with a trusted key to produce an attestation document. This document is what anyone in the world can
verify
, to ensure that a particular enclave is running exactly what it should be running without having physical access to the underlying hardware. Let's dive into remote attestations in more detail.

Remote Attestations

An attestation document, at its core, is
a signed message from an enclave
. The message contains information, and the signature comes from a key pair generated inside the enclave when it boots. Let's dive into the following:

What structure does this “message” have?

Who generates the key pair signing attestation documents? Why should we trust it?

Attestation message

An attestation message is structured binary data


, encoded using
CBOR
. The important fields contained in each attestation are:

PCR0

: Hash of the Enclave Image File (EIF). EIFs are what the Nitro enclaves boot with. Turnkey's enclaves all use
QuorumOS
as an EIF. The recipe to build it is
here

PCR1

: Linux kernel and initial RAM data hash (aka
initramfs

PCR2

: Hash of user applications, without the boot ramfs. For Turnkey enclaves,
PCR2
and
PCR1
measurements are identical because we do not use

pivot_root

, and simply use
initramfs
as our final filesystem.

PCR3

: Hash of the IAM role of the Nitro host. Concretely this is the hash of an
AWS IAM
identifier (for example
arn:aws:iam::123456789012:role/nitrohost
).

certificate

: Contains an X.509 certificate specific to the enclave. It contains an enclave-specific public key. This enclave-specific key pair is generated during boot, and signs the attestation document.

cabundle

: Certificate chain to certify the “certificate” above.

user_data

: User-supplied field. QuorumOS sets it to be the hash of the QOS manifest
here
(more on this later).

nonce

: User-supplied field, unused by QuorumOS at the time of writing.

public_key

: User-supplied field, set to the QOS ephemeral public key (
here
).

All of the fields above are part of the signed payload (our attestation “message”).

Signature and chain of trust

Attestation signatures use P-384, a standard signature scheme defined by NIST (in
FIPS 186-4
). This signature is produced by a
new random key pair created by the enclave when it boots.

This brand new public key is referenced inside of the attestation's
certificate
field. This certificate is signed by another certificate, all the way to a top-level AWS certificate. This certificate “chain” or “bundle” is contained in the
cabundle
field. The same concept of certificate chain underpins TLS: your browser trusts a website's TLS certs because of its certificate chain, going up to top-level Certificate Authorities (CAs) trusted by your browser or trusted by your operating system. AWS is a CA for the Nitro enclaves it owns and operates.

To verify that an enclave certificate originates from Amazon, the certificate chain ascends to the root certificate for the commercial AWS partitions as described
here
. This top-level certificate is stable, can be downloaded, pinned, and checked. Indeed it's valid until
October 28th, 2049
! Here it is in its parsed form (note the
Not After
field):

$ openssl x509 -in ~/Downloads/root.pem -text -noout

Certificate:

Data:

Version: 3 (0x2)

Serial Number:

f9:31:75:68:1b:90:af:e1:1d:46:cc:b4:e4:e7:f8:56

Signature Algorithm: ecdsa-with-SHA384

Issuer: C = US, O = Amazon, OU = AWS, CN = aws.nitro-enclaves

Validity

Not Before: Oct 28 13:28:05 2019 GMT

Not After : Oct 28 14:28:05 2049 GMT

Subject: C = US, O = Amazon, OU = AWS, CN = aws.nitro-enclaves

(...etc)

We trust AWS attestations because we trust Amazon as infrastructure operators. An attestation's CA bundle (in the
cabundle
field) transfers trust from Amazon's top-level certificate to the enclave-specific key which signs the attestation document.

If “→” means “signs”, we can summarize the chain of trust for an AWS remote attestation with:

Root AWS cert → Intermediate cert 1

Intermediate cert 1 → Intermediate cert 2


Intermediate cert N-1 → Intermediate cert N

Intermediate cert N → Enclave X.509 certificate attesting to the enclave public key

Enclave key pair → Remote Attestation document “message”

Running secure applications with QuorumOS

In this section we introduce
QuorumOS
(“QOS”), a new minimal, open-source operating system engineered for verifiability. QuorumOS is our base operating system: it is the Enclave Image File (“EIF”) used to boot Turnkey enclaves.

In the previous section we've established that remote attestations prove that the correct EIF runs inside of an enclave. As a result it's possible for anyone in possession of a remote attestation to verify that an enclave runs the expected version of QuorumOS, by hashing and comparing a local EIF with the
PRC0
measurement contained within the attestation.

QuorumOS was engineered to run any application verifiably, on top of QuorumOS. Applications come in the form of binary artifacts. We'll explain in the sections below how we've designed QOS Manifests to contain not only the expected artifact digest, but also critical configuration such as quorum settings and public keys.

QuorumOS itself provides QOS Manifests as user data when fetching remote attestations. This lets anyone verify that an enclave is running the correct EIF (QuorumOS itself), and that QuorumOS is running the correct application binary (as specified in the QOS Manifest).

We've been using QuorumOS in production for 2+ years and it has gone through multiple rigorous audits, internal and external. We're excited to share this with the larger security community.

What is QuorumOS?

QuorumOS (QOS) is an operating system and set of libraries to boot and provision


applications in AWS Nitro enclaves. The repository, available at
github.com/tkhq/qos
, is organized as a set of Rust crates (software packages) which are compiled into various places to either
boot
an enclave, or
provision
it. We'll see these flows in detail later in this document. For the moment let's look at a simplified diagram showing where each Rust crate fits:




QuorumOS crates and how they relate to enclave boot and provisioning

qos_enclave

contains utilities to boot a new Nitro enclave from a host machine. It calls into the Nitro CLI to boot an enclave with a given EIF. This crate is used by Turnkey's infrastructure (Kubernetes) to boot new enclaves programmatically.

init

defines the enclave's init binary. It is the program which gets executed as PID 1 when QuorumOS boots. This crate is compiled into the QuorumOS base OS. This crate imports

qos_aws

to interact with the NSM to signal readiness, and

qos_system

for lower level functionality.

qos_core

defines a protocol and state machine to boot secure applications. The protocol is very straightforward: it's composed of
messages
that can be received or sent over the VSOCK connection, from host to enclave. The state machine


is built on top of the protocol. This crate is used within application binaries to define the requests they can receive and respond accordingly.

qos_nsm

is a simple wrapper crate to let Rust application code invoke the AWS NSM APIs for secure time or entropy.

qos_client

is a CLI used to parse and verify manifests, and post shares of Quorum Keys into booted enclaves. When a QuorumOS enclave is provisioned with an application binary, a valid QOS Manifest, and enough approvals and shares, the enclave is provisioned. We describe the provisioning process in greater detail in the next section.

qos_host

, not depicted in the diagram, is a library and CLI to build host-side servers which receive requests from the outside and talk to an enclave application to serve these requests.

qos_net

, not depicted in the diagram, defines a VSOCK-to-TCP proxy server. It runs on the host side to provide optional external connectivity to enclave apps which need it, as well as Rust utilities to use it from inside secure applications. This connectivity is one-sided. With
qos_net
secure apps can choose to reach out to the outside, but not the other way around.

Finally,

qos_crypto


qos_p256

and

qos_hex

are libraries and abstraction implementing utility functions imported by previously listed crates.

Now that we've looked at the layout of the code, let's walk through the two important responsibilities fulfilled by QuorumOS: enclave
boot
and enclave
provisioning

Enclave boot

Booting an enclave can be done by humans as a one-off, through the Nitro CLI, but we've automated the process with

qos_enclave

. The main function provided by this crate is

boot()

, which uses
aws/aws-nitro-enclaves-cli
to start a new enclave from a Nitro host, using information passed in as env vars:
EIF_PATH
MEMORY_MIB
, and
CPU_COUNT

In production settings this task is performed by our codified Kubernetes infrastructure.

Note that enclave boot is application agnostic. The process is the same regardless of which application needs to run, because the EIF file is application agnostic and only contains QuorumOS itself. The base operating system is a barebone Debian kernel (built
here
). We have
configured
it to minimize its footprint and attack surface. It secures all applications running on top of it.

The enclave boot process is provider-specific because it needs to interact with the Nitro APIs to launch enclaves, which are AWS specific. The boot process will need to evolve as we expand our deployment targets to Google Shielded VMS, Azure's confidential compute platform, or on-prem TEEs. External contributions are welcome in this area, check out
the QuorumOS repository
if you want to start contributing!

Enclave provisioning

Enclave provisioning is application-specific. It is how we turn a generic QuorumOS-booted enclave into an enclave running a particular binary, provisioned with a particular Quorum Key.

This process is provider-agnostic and largely remains the same regardless of the underlying infrastructure provider. Let's introduce some important terms before diving into the provisioning flow in detail:

QOS Manifest
: a binary file which contains configuration to specify a secure application. It contains the digest of the application binary, the Quorum Key (public key), the Share Set, the Manifest Set, expected PCR measurements, and any command-line arguments necessary to launch the application binary. The full specification is available
here

Quorum Key
: an asymmetric key pair used to authenticate and encrypt data. This key should only ever be reconstructed inside of an enclave. Outside of the enclave, the key is stored as redundant shares (key is split using
Shamir's secret sharing
at genesis


). These shares are encrypted to hardware keys held by humans (Turnkey operators or external operators). Never in plaintext, never outside of secure hardware.

Manifest Set
: collection of public keys and a threshold. The threshold indicates how many members are needed to approve the QOS Manifest. During the provisioning flow, enough members of the Manifest Set must approve the QOS Manifest to provision an enclave.

Share Set
: collection of public keys and a threshold. The public keys represent the members holding a Quorum Key share, and the threshold indicates how many shares are needed to reconstruct the Quorum Key. During the provisioning flow, enough members of the Share Set must post their Quorum Key shares.

Namespace
: a string identifying a group of individual enclaves provisioned with the same application. Enclaves within the same namespace share the same Quorum Key.

Provisioning sequence

When an enclave boots with the QuorumOS EIF, it immediately waits for provisioning to happen

10

. To provision an enclave, three inputs are needed:

The secure application binary

The Manifest Envelope, which is composed of a QOS Manifest and enough approvals by its Manifest Set members

Enough Quorum Key shares to reconstruct the Quorum Key

This is summarized in the diagram below, where we have (as an example) a 2-out-of-3 setting in our Manifest Set, and a 3-out-of-5 setting in our Share Set:




Provisioning inputs: application binary, Manifest Envelope, and encrypted shares

Provisioning is done in 2 steps:

The application binary and the Manifest Envelope (which is a
QOS manifest
and cryptographic approvals bundled together) are sent together in a single request. QuorumOS checks the application binary against the QOS manifest (the digest of the binary needs to match the digest in the manifest), and checks that enough approvals have been provided in the manifest envelope. If everything is copacetic, QuorumOS transitions to the
WaitingForQuorumShards

11

state.

Share Set members post their shares of the Quorum Key. Once enough shares are posted, the Quorum Key is reconstructed, and the enclave is fully provisioned with its core secret. In order to scale Turnkey and run it in modern clouds where underlying hardware can be shut down without notice, we had to design an alternative to this manual, cumbersome process: see
Appendix: scaling provisioning with Key Forwarding

When an enclave is successfully provisioned, QuorumOS starts the application binary. External callers can send requests to the enclave application by sending a QOS
ProxyRequest
messages. These messages contain raw bytes, unwrapped by QOS and passed along to the underlying application binary.

Secure share posting with remote attestations and Ephemeral Keys

We've explained above that the last step in the provisioning process is Quorum Key share posting. Here we answer the following questions:

How do Share Set members know they're posting to the right machine?

How are Quorum Key shares secured in transit?

After QuorumOS receives an application binary and a valid Manifest Envelope, it generates a new Ephemeral Key (a new asymmetric key pair – see code
here
). This Ephemeral Key (public key) is referenced inside of the
public_key
field of AWS attestations. The flow for posting shares can be detailed as follows:

The operator establishes a connection to a candidate enclave (in
WaitingForQuorumShards
state) and requests a remote attestation.

The remote attestation contains a hash of the QOS manifest in the
user_data
field (
code link
), which the operator can compare to a locally computed digest of a copy they've previously reviewed. This ensures the candidate enclave has received the correct application binary and Manifest Envelope.

The remote attestation contains the enclave Ephemeral Key (public key) in the
public_key
field. The operator, using ECDH, encrypts their Quorum Key share to the Ephemeral Key. This guarantees that only this particular enclave (which is provisioned with the correct binary and manifest envelope) is able to decrypt the share. The operator can thus send this encrypted payload over the network safely.

The enclave receives the encrypted Quorum Key share, and decrypts it using their Ephemeral Key.

In practice, Quorum Set members follow strict runbooks when posting shares or approving manifests. For security reasons we mandate that members of Quorum Sets (either Share or Manifest Set) never expose their key material to an online machine. See
Appendix: airgapped workflows for Quorum Set members
for more information.

Summary

QuorumOS provisioning is a crucial part of Turnkey's security because it enforces that the right logic and cryptographic checks are executed. In particular it enforces that the manifest is signed by enough members of the Manifest Set, and that the shares are posted by members of the Share Set who have reviewed the manifest.

All Nitro enclaves boot with a generic QuorumOS EIF. Through the provisioning process they acquire an application binary, a signed manifest (“Manifest Envelope”), and a Quorum Key (reconstructed from shares posted by Share Set members).

Once provisioned with a binary and Manifest Envelope, an enclave's attestations contain the hash of the QOS manifest in its
user_data
field and the enclave ephemeral key in its
public_key
field. This is fundamental to secure share posting.

A point worth re-stating: QuorumOS is responsible for providing the hash of the QOS manifest in the attestation's
user_data
field.
The QOS Manifest is the link between our application-agnostic QuorumOS EIF file and the application binary
. An AWS Nitro attestation contains a digest of the QOS manifest, and the QOS manifest contains a digest of the application binary.

Reproducible builds through StageX

We now explain why reproducible builds are the last, crucial problem to resolve to run software verifiably: they create an immutable link between application binaries and their associated human-readable source code. Because the binary can be reproduced by anyone, human consensus around the
source code
becomes possible. This is the difference between proving “this enclave runs this binary digest” and “this enclave runs this source code”. With verifiability extending all the way to the source code, anyone can independently examine the enclave’s functionality in the finest detail. With a mere binary hash, that would not be possible. This is, unfortunately, the current status quo: Most TEE-based systems are not truly verifiable because they lack reproducible builds.

StageX is our answer to the reproducible build problem. It is a crucial component of Turnkey's verifiable foundations. It is
open-source
for all to see and use.

Why are reproducible builds required?

Humans write code in text format. This is generally referred to as “source code”. For compiled languages, source code is processed by a compiler to produce binary artifacts. These artifacts are the executables run by computers, and our Nitro enclaves are no exceptions: they accept an EIF file which runs QuorumOS, and QuorumOS accepts in turn a manifest and a application binary.

EIF file digests are referenced in AWS attestations, and application binary digests are referenced in QOS Manifests. It is of vital importance that these digests can be verified and mapped to the source code that produced them. As humans we can make sense of the source code but we can't understand binaries directly.

Although one would assume that each run of a compiler produces the exact same binary artifact, this isn’t true. The final binary generally depends on the system architecture, system library versions, system username, hostname, name of the folder you are building in, filesystem, system time, timezone, language, number of cores, core speed, linux kernel version, container runtime version, CPU brand and model, and more. Interestingly, these differences in the binary artifact do not always mean that the program will behave differently. Unfortunately there is no way to tell with certainty whether a difference is a “no-op” or would result in a (potentially malicious!) behavior change. That's because humans can't read binary artifacts directly. Tools like
Ghidra
exist but they aren't suitable for day-to-day use.

Because of this ambiguity (differences in binary artifacts can't be judged “good” or “bad” easily), a non-reproducible build causes an inherent single-point-of-failure: the human or machine building the source code and producing the binary artifact has to be trusted to faithfully compile the code, without modifying it. Everyone else has to trust that it is indeed the case.

Unfortunately the process to go from source code to binary artifacts (“the build”) is generally not well-known or cared for. We've often seen this be run by a single machine or set of machines responsible for other non-critical or outright insecure “development” workloads. This could be in the form of Github CI runners which automatically publish docker images, or self-hosted clusters which populate an internal store with tagged binaries. This build process is a major weak point because
it is an inherent single point of failure
. If the build process is compromised, arbitrary code can be snuck into secure applications. This class of exploits is generally known as “supply-chain attacks” and they're so common it'd be easy to fill pages of examples with real-world examples (here are a few famous ones:
one
two
three
, and most recently
four
). This is not a risk we can tolerate at Turnkey.

Without a reproducible build, the expensive social consensus formed by multiple (human) parties around the security of the
source code
is all for naught: two parties building the same agreed upon code revision will arrive at two different artifacts, with two different digests:




Non-reproducible build, where 2 separate builds result in different digests.

A reproducible build allows linking artifacts back to their source code in an immutable way: a single source code revision always yields the same binary artifact, byte-for-byte. As a result the digest is the same and can be signed by multiple parties:




A reproducible build guarantees separate builds result in the same final digest.

If multiple machines or humans can independently attest to the fact that a set of source files yields the same artifact, and thus the same digest, the single point of failure is gone. As a bonus, this scales well: it's possible to achieve arbitrarily strong consensus about digests: we simply need to ask arbitrarily many parties to reproduce these binary artifacts and cryptographically sign the resulting digest. We use this to our advantage to gain confidence about secure app binaries or EIF files for example.

Reproducible builds in practice

The first version of reproducible builds at Turnkey used Debian containers as a base and
Toolchain
to build them in a reproducible way. The main idea was to abstract away differences between build environments (such as
user and group IDs
number of CPUs
timestamp
and
many others
) with custom environment variables and system configuration baked into build processes via Makefile macros. This came with major downsides that slowed down developer productivity:

Repositories needed to keep costly snapshots of all dependencies in Git LFS or similar to be able to reproduce the exact build container. Otherwise the “latest” packages that would otherwise be downloaded would shift over time. This created a lot of friction for our team having to regularly archive, hash-lock, and sign hundreds of
.deb
files for every project.

Debian has very old versions of Rust, which we rely on heavily. This very frequently caused frustration when trying to upgrade external crates.

The builds themselves relied heavily on Makefile and macros. Most engineers are not familiar with this syntax; as a result debugging builds was really hard.

After a few months with this setup, we concluded that something had to change. Today our secure builds are powered by
StageX
, a new Linux distro focused on immutable, reproducible packages distributed in the form of Docker
OCI
images. It builds on classical Stage 0-3 compiler
bootstrapping
to produce a container-native, minimal, and reproducible toolchain. Curious readers are encouraged to look at
Appendix: Why We Created StageX instead of using X
for details about available Linux distributions and why they ultimately didn't meet our bar.

How StageX works

StageX distributes packages as
OCI
containers. This allows hosting them just like any other images, on DockerHub

12

, and allows for hash-locked pulls out of the gate. OCI is the only well-documented packaging standard with multiple competing toolchain implementations and multiple-signature support. It's the most widely used and understood way to deploy software today.

Because StageX packages are OCI images, using StageX's reproducible Rust is a simple
FROM
away:

FROM stagex/rust@sha256:b7c834268a81bfcc473246995c55b47fe18414cc553e3293b6294fde4e579163

This forces a download of an exact image, pinned to a specific digest (
b7c83426…
). You can see existing signatures for this image at
stagex:signatures/stagex/rust@sha256=b7c83426…
, or reproduce it yourself from source with
make rust
. As a result you can trust that the Rust image you're pulling comes from
this
Containerfile

and contains nothing malicious, even if you pull it from an untrusted source. If the downloaded image is corrupted, its sha256 digest won't match the pinned digest, and the build will error out.

How Turnkey uses StageX

Secure applications at Turnkey are all built this way. As a result anyone can reproduce builds independently, and validate remote attestations meaningfully when we deploy critical software into Nitro enclaves.

As a concrete example, take a look at

linux

: this is the kernel we use inside of QuorumOS (configured with
this kernel configuration
). It is maintained in StageX because Amazon itself does not provide a properly signed, reproducible, source-bootstrapped or even recent kernel for Nitro enclaves. Thanks to StageX, the kernel we use in Turnkey enclaves is built reproducibly and signed by multiple parties, all the way down to a few hundred bytes of assembly (see

stage0

).

We use StageX to power EIF builds and all other Turnkey builds. As a result EIF files are byte-for-byte identical, every time, regardless of who builds them, regardless of which computer in the world kicks off “the build”. And that's no small feat: the AWS developer SDK can't do this for you!

Because we can verify EIF builds, we're able to use remote attestations meaningfully:
PCR0
values can be verified and mapped to source code, which can be independently reviewed and audited by multiple (human) parties.

The invisible hard problems resolved by StageX

The fact that StageX works is a miracle that could not have been possible without relying on other people's work. Here we highlight a few of the big hurdles.

Bootstrapping GCC

This was by far the thorniest issue to resolve. Many individuals and projects have contributed to solving it over the years.
Carl Dong
gave a
talk about bootstrapping
which rallied people to the effort started by the Bitcoin community, Guix recently
proved it could bootstrap a modern Linux distribution
for which the
Stage0
and the
Gnu Mes
teams provided key ingredients, and the
bootstrappable builds
and
live-bootstrap
projects glued it all together.

StageX follows the footsteps of Guix and uses the same full-source bootstrap process, starting from

hex0

, a 190 bytes seed of well-understood assembly code. This seed is used to compile

kaem

, “the world's worst build tool”, in

stage0

. Stage
, and
build on this just enough to build

gcc

, which is used to build many other compilers and tools.

GCC to Golang

It is worth acknowledging the excellent work done by Google. They have
documented this path well
and provide all the tooling to do it. You only need 3 versions of Golang to get all the way back to GCC. See
stagex:packages/core/go

Bootstrapping Rust

A given version of Rust can only ever be built with the immediately previous version. If you go down this chicken-and-egg problem far enough and you realize that in most distros the chicken comes first: most include a non-reproducible “seed” Rust binary presumably compiled by some member of the Rust team, use that to build the next version, and carry on from there. Even some of the distros that say their Rust builds are reproducible have a pretty major asterisk.

Thankfully
John Hodges
created
mrustc
, which implements a minimal semi-modern Rust 1.54 compiler in C++. It is missing a lot of critical features but it
does
support enough features to compile the official Rust 1.54 sources, which can compile Rust 1.55 and so on. This is the path Guix and Nix both went down, and StageX is following their lead, except using
musl
. A
quick patch
did the trick to make
mrustc
work with
musl
. See this in action for yourself at
stagex:packages/core/rust

13


Reproducible NodeJS (!)

NodeJS was never designed with reproducible builds in mind. Through extensive
discussion with the maintainers
and a lot of
effort
, NodeJS is now packaged in StageX:
packages/core/nodejs
. This is (to our knowledge) an industry first.

Boot Proofs and App Proofs

So far we have introduced Turnkey's foundations in pieces: the base layer is composed of AWS Nitro enclaves, then comes QuorumOS which is the glue between TEEs and applications running within them, and finally, StageX is a foundational build system to solve the reproducible build problem. In this section we explain more concretely why and how anyone can verify Turnkey enclaves do what they claim to be doing.

Introduction

We have designed Turnkey's foundations to provide two main types of proofs:

Boot Proofs
are bundles composed of an AWS Nitro Attestations and a QOS Manifests. Together, an AWS Nitro Attestation and a QOS Manifest prove that a given application binary has been provisioned inside of an enclave.

App Proofs
are arbitrary messages signed by an enclave's ephemeral key pair. Recall that a new ephemeral key pair is generated when enclaves boot. As a result, ephemeral keys are globally unique and can be used as enclave identifiers (unlike Quorum Keys, which are stable across enclaves running the same application). The messages in App Proofs are application-specific and can be used to prove arbitrary parts of an enclave's functionality.

These proofs are linked together by the enclave ephemeral key. Boot Proofs attest to boot configuration of an enclave, which includes the QOS Manifest digest and the enclave ephemeral public key. This is where App Proofs come from: application-specific messages are signed by this same ephemeral key. To walk the chain back up, one must:

Obtain an App Proof and verify its validity against the ephemeral public key

Obtain a Boot Proof for this particular ephemeral public key. Because the ephemeral public key is globally unique, there is a single AWS Attestation document and QOS Manifest associated with an ephemeral public key.

The relationship between Boot Proof and App Proof is one-to-many: there are many App Proofs for a single Boot Proof (an enclave may sign many application-specific messages during its lifespan, once booted), but there is a single Boot Proof for every App Proof (an enclave ephemeral public key is globally unique, and specific to an enclave which is provisioned with a particular application binary)

Proven by Boot Proofs

Recall that a Boot Proof is an AWS attestation document bundled with a particular QOS Manifest. A Boot Proof proves the following facts:

The enclave is a legitimate Nitro enclave and the attestation document is valid
. This can be verified through the certificate bundle contained in the AWS attestation document and verifying the signature against Amazon's root certificate, documented
here

The enclave runs a QuorumOS EIF
. This can be verified through the PCR measurements in the AWS attestation document. More precisely:

Anyone can take the Boot Proof's Attestation document and look at its
PCR0
measurement.

Anyone can download QuorumOS's
source code
and build the EIF locally. Because this build process uses
StageX
it is reproducible. It will produce the same EIF, and yield the same digest when hashed.

This is of crucial importance: now anyone can inspect QuorumOS's source code and know that what they read is what runs inside of the enclave.

As a consequence of this
verifiability all the way to the source code
, anyone can verify that QuorumOS does its job, the full job, and
nothing but its job
, by reading its source code independently. The crucial parts include:

Generating a brand new key pair when enclaves boot (the ephemeral key pair), done
here

Passing the ephemeral public key in the
public_key
field of the attestation document, done
here

Passing the QOS Manifest digest in the
user_data
field of the attestation document, done
here

The enclave runs a particular application
. This can be verified in two steps:

First, by hashing the QOS Manifest (in the Boot Proof), anyone can verify that it matches the digest in the
user_data
field of the AWS attestation document. This proves that the Boot Proof's QOS Manifest is indeed the correct manifest, and by extension, proves that the data within it describe the application running inside of QuorumOS. The QOS Manifest includes the application binary hash, the Quorum public key, the Quorum Set member public keys,
and more

Second, anyone can verify that the source code for a particular application produces the correct digest (inside of the QOS manifest) by reproducing it. This is made possible (once again) by
StageX
, which Turnkey uses for all enclave application builds.

Because ephemeral key pairs are unique to each enclave we know that App Proofs come from a particular enclave, running a particular application, itself running within a particular version of QuorumOS. Verifiability down to the source code, all the way down.

Proven by App Proofs

App Proofs are application-specific and prove different classes of facts depending on which enclave application produces them. Recall that an App Proof is always associated with a Boot Proof, which contains the digest of the application binary (in the QOS Manifest) as well as the digest of the QuorumOS EIF (in the AWS Attestation document).

Because applications and QuorumOS use StageX and are built reproducibly, anyone can thus verify the source code for themselves and see where App Proofs originate from exactly. App Proofs are messages signed by an enclave's ephemeral key pair, thus extremely simple and lightweight to verify. We'll see in
Architecture
the different types of enclave applications Turnkey has built so far, and where App Proofs can provide the most value.

Conclusion

We've explained how Turnkey's foundations enable running applications securely and verifiably inside of TEEs. They're composed of three main components: TEEs, QuorumOS, and StageX.

Turnkey uses an application-agnostic QuorumOS EIF to boot enclaves.

Remote attestations provide proof that an enclave is provisioned with the correct EIF, by signing PCR measurements.

QuorumOS is the base operating system running in TEEs and the glue between provider-specific hardware and provider-agnostic applications, written in Rust. We've designed QuorumOS to run secure applications at scale, in modern cloud environments, without any single points of failure (“Quorum” based approach)

QuorumOS works with TEE attestations to prove that a given instance of QuorumOS is provisioned with a specific application. This is done with QOS Manifests, which references the application digest and configuration. This lets anyone verify an enclave booted in the correct datacenter, with the right hardware, provisioned with the correct QuorumOS base image, and running the correct application binary.

StageX provides reproducible builds to ensure binary artifacts and their digests can be reproduced, agreed upon, and certified by multiple parties meaningfully. StageX guarantees a 1-to-1, immutable relationship between human-readable source code and the resulting machine-executable artifacts.

We've also introduced the concepts of Boot Proofs and App Proofs, and explained in detail how anyone can verify what runs in enclaves, all the way down to the exact source code which produced the QuorumOS EIF and the application binary running within it.

Now that we've seen how applications can be run securely and verifiably, it's time to move up a layer and explain how we've architected and built best-in-class key management APIs using our verifiable foundations. See you in
Turnkey's Architecture

Appendix: Why We Created StageX instead of using X

To achieve reliable reproducible builds we took a hard look at the available options around us to avoid building anything from scratch ourselves if we did not have to. This is a list of what we evaluated and why we ultimately rejected those options:

Alpine
is the most popular distro in container-land and has made great strides in proving a minimal
musl
-based distro with reasonable security defaults. It is suitable for most use cases, however in the interest of developer productivity and low friction for contributors, none of it is signed

14


Chainguard
sounds great on paper (container-native!), but on closer inspection they built their framework on top of Alpine which is neither signed nor reproducible and Chainguard image authors do not sign commits or packages with their own keys. They double down on centralized signing with
cosign
and the
SLSA framework
to prove their centrally built images were built by a known trusted CI system. This is however only as good as those central signing keys and the people who manage them which we have no way to trust independently.

Fedora
(and
RedHat
-based distros) sign packages with a global signing key, similar to Chainguard, which is not great. They otherwise suffer from similar one-size-fits-all bloat problems as Debian with a different coat of paint. Their reliance on centralized builds has been used as justification for them to not pursue reproducibility, which makes them a non-starter for security-focused use cases.

Arch Linux
has very fast updates as a rolling release distro. Package definitions are signed, and often reproducible, but they change from one minute to the next. Reproducible builds require pinning and archiving sets of dependencies that work well together for your own projects.

Debian
(and derivatives like
Ubuntu
) is one of most popular options for servers, and also sign most packages. However, these distros are
glibc
-based with a focus on compatibility and desktop use-cases. As a result they have a huge number of dependencies, partial code freezes for long periods of time between releases, and stale packages as various compatibility goals block updates.

Nix
is almost entirely reproducible by design and allows for lean and minimal output artifacts. It is also a big leap forward in having good separation of concerns between privileged immutable and unprivileged mutable spaces, however they don’t do any maintainer-level signing in order to ensure any hobbyist can contribute with low friction.

Guix
is reproducible by design, borrowing a lot from Nix

15

. It also does maintainer-level signing like Debian. It comes the closest to what we need overall (and this is what
Bitcoin settled on
!), but lacks multi-sig package contributions as well as minimalism. The dependency tree is large because of glibc.

Summarizing the above in a table:






















Distro



OCI support



Signatures



Libc



Reproducible



Bootstrapped







Alpine



Published



None



musl



No



No







Chainguard



Native



1 Bot



musl



No



No







Fedora



Published



1 Bot



glibc



No



No







Arch



Published



1+ Human



glibc



Partial (
90%



No







Debian



Published



1 Human



glibc



Partial (96%)



No







Nix



Exported



1 Bot



glibc



Partial (95%)



Partially







Guix



Exported



1+ Human



glibc



Partial (90%)



yes








StageX





Native





2+ Humans





musl





Yes (100%)





yes





This should speak for itself: these candidates didn't quite meet our bar. We wanted the musl-based container-ideal minimalism of Alpine, the obsessive reproducibility and full-source supply chain goals of Guix, and a step beyond the single-party signed packages of Debian or Arch.

Appendix: airgapped workflows for Quorum Set members

This appendix discusses the airgap process we've designed to provision enclaves with a particular application. Recall from the
Enclave provisioning
section that we've split the provisioning process into two parts, involving two different Quorum Sets.

Manifest approval: in this phase, enough Manifest Set members must approve the QOS Manifest with their private key.

Share posting: in this phase, enough Share Set members must post their share of the Quorum Key to the enclave. We have seen in
Secure share posting with remote attestations and Ephemeral Keys
that Share Set members use their private keys to decrypt and re-encrypt Quorum Key shares.

In both cases we are performing critical operations:

Manifest approval requires the use of a private key to sign

Share posting requires the use of a private key to decrypt and re-encrypt Quorum Key shares. A decrypted share lives in memory for a brief period of time.

For this reason we've designed an airgapped workflow based on
AirgapOS
, an open-source OS built for these critical use-cases. Operator key material is always held on secure hardware and connected to an offline machine. This drastically reduces the odds of compromise.

Both workflows have the same goal: ensure that no private key material is ever exposed to an online environment. We accomplish this by dedicating a device (the “offline” device) to the critical operations (signing, decryption, encryption). Inputs to these critical operations are transported from a standard machine (the “online” device) to the offline device in a one-way fashion (see
data diode
).

Offline and online devices can be separate physical laptops or machines, but can also be implemented as separate Qubes (in
QubesOS
). For data diodes, we use SD cards physically written to at the source, and transported to the destination device. If using Qubes,
qvm-copy
can be used to safely transport files from one Qube to another.

Manifest approval




Airgapped manifest approval workflow

This workflow is performed by Manifest Set members.

[online device] Each member downloads the to-be-approved Manifest and software to parse and approve it. This software is built with StageX, reproducibly.

[online device] Each member reproduces the application binary to ensure its digest matches the application digest in the QOS Manifest.

[one-way transport] QOS Manifest and software moves to offline device

[offline device] Operator connects their hardware key

[offline device] The verification software parses the QOS manifest and prompts the operator to manually verify its attributes such as: command line argument to run the application, AWS region, and so on.

[offline device] Once the operator finishes manual verification, they sign the manifest using their connected hardware key. This produces the manifest approval.

[one-way transport] Manifest approval moves to the online device

[online device] Manifest approval is persisted in a git repository. Once enough manifest approvals are persisted, the manifest is considered approved.

Quorum Key share posting




Airgapped share posting workflow

This workflow is performed by Share Set members.

[online device] Each member downloads software, the to-be-posted encrypted share which belongs to them, as well as a copy of the QOS manifest and its approvals. This ensures share posting can't take place unless the QOS manifest is approved.

[online device] Each member also obtains a live attestation document coming from the to-be-provisioned enclave. This attestation contains a reference to the QOS manifest as well as an ephemeral public key specific to the enclave.

[one-way transport] QOS Manifest, manifest approvals, encrypted share, and software moves to the offline device.

[offline device] Operator connects their hardware key

[offline device] The verification software parses the QOS manifest, approvals, and attestations and prompts the operator to manually agree to the configuration. This ensures the operator has full context about the application being provisioned.

[offline device] Each member decrypts the encrypted share and re-encrypt it to the enclave's ephemeral public key. This produces an encrypted share (encrypted to a particular enclave).

[one-way transport] The encrypted share moves to the online device

[online device] The encrypted share is then posted to the target enclave. Once enough shares are posted, the enclave is fully provisioned.

Appendix: scaling provisioning with Key Forwarding

Provisioning enclaves as explained above might seem “good enough” but in practice we've found a few problems when running at scale in cloud environments:

When running on cloud machines there is no guarantee that the underlying hardware won't be taken away or rebooted. Amazon provides a generous
2 minutes warning
. A replacement machine cannot be automatically provisioned without human involvement given Quorum Key shares have to be posted by Share Set members. This is a massive problem during off-hours periods (nights and weekends)

When an application is under heavy load, provisioning extra capacity requires human involvement for the same reason (Share Set members need to post Quorum Key shares).

If there is a bug in an application and a new version needs to be rolled out, a completely new fleet of enclaves needs to be manually provisioned with the updated code. This requires Manifest Set members and Share Set members involvement.

To solve the provisioning problem at scale we've designed Key Forwarding[^19] as a solution. Key forwarding allows booting new QOS nodes without manually submitting Quorum Key shares. Instead, a new QOS enclave receives a Quorum Key from an already-provisioned enclave after proving it runs the same application (new QOS nodes must be in the same namespace as the one they are requesting the Quorum Key from).




Sequence diagram for Key Forwarding

Key forwarding involves 3 parties: an already-provisioned enclave (“Old”) with access to a live Quorum Key, a freshly booted enclave (“New”), and a Client. The Client's only role is to facilitate communication between old and new enclaves: indeed enclaves do not know about each other and have no way to issue outbound requests. Here's how a Key Forward provisioning flow works:

First, the client makes a Key Forward provisioning request to the New Enclave. This is similar to a normal provisioning request: it contains the Manifest Envelope and the associated application binary. Instead of being in
WaitingForQuorumShards
state, our New enclave transitions to
WaitingForForwardedKey
. It creates a brand new Ephemeral Key, similar to the standard provisioning flow.

An attestation document containing the New enclave's Ephemeral Key (public key) is returned to the Client.

The Client connects to the Old enclave and makes an Export Key request which contains the New enclave's attestation. Many cryptographic and consistency checks happen, among which: validity of the attestation, old and new Quorum Set and Share Set must be identical, and PCR values must be identical. For the full specification see

KEY_FORWARDING.MD

. Once the verification is complete the Old enclave is “convinced” that the New enclave is a peer, running the same secure application, maintained by the same groups of operators. It encrypts its Quorum Key to the New Enclave's Ephemeral Key.

The encrypted Quorum Key is returned to the Client

The Client makes an Inject Key request with the encrypted Quorum Key. The New enclave decrypts it, verifies the public key matches with the expected value in the manifest, and signals success. The New enclave has been provisioned successfully.

As a result of Key Forwarding, enclave fleets can be scaled up or down without human involvement. They can also recover from cloud terminations: as long as at least one healthy enclave remains in the fleet, more enclaves can be spun up without manual share posting. Key forwarding has been an invaluable solution to scaling Turnkey.


For a complete overview, check out
this video


A single Nitro Host can start multiple enclaves if the underlying hardware has enough CPU and RAM to accommodate them.


See
https://github.com/aws/aws-nitro-enclaves-image-format
for the format definition.


The Nitro Secure Module is a driver loaded inside of the base OS, connecting to the physically separate Nitro Card via a PCIe interface. Loading this driver is done in the StageX Containerfile,
here
. For more information about the AWS Nitro system, check out their
whitepaper


In case you are wondering “is using RAM safe?”: RAM is never shared between enclave requests, and we write applications in Rust to guarantee memory safety within a single request.


The set of fields is documented by AWS
here
for the general structure and
here
for PCRs specifically


By “provision” here, we mean provisioning an enclave with its final application binary and Quorum Key (stable secret). See later sections for details.


The set of possible states for an enclave is listed
here


In case you're wondering: the genesis ceremony also happens inside of an enclave. See
genesis.rs

10

Confusingly, this initial state is called WaitingForBootInstruction in the code. The enclave is booted, waiting for
provisioning
instructions. We might rename this at some point in the future.

11

The place where this is declared is
here
. Each ProtocolRoute takes “Ok phase” and “Err phase” as the 2nd and 3rd argument.

12

We're thankful for the Docker team's help: they advised on finding a path to fully reproducible OCI images and offered unlimited free bandwidth to upload and host StageX images.

13

Because of this 1 step version-to-version process in rust, when reproducing StageX from stage0 to stage3, building up all the rust versions up to 1.81 is often the longest part of the build process.

14

By “none of it is signed” we mean that published artifacts aren't signed by the maintainer or contributor who produced them. In the absence of code signing the publisher is potentially misrepresented because it is easy to impersonate.

15

Turns out Guix is not 100% reproducible either and is in a similar position to Nix. Packages that include binary blobs like the firmware blobs are just copied directly. Hitting 100% reproducibility may take ages, particularly with no forcing function.

16

See
this documentation
for more information.





















