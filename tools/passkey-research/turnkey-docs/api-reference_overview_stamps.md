# Source: https://docs.turnkey.com/api-reference/overview/stamps






Stamps - Turnkey





Documentation Index

Fetch the complete documentation index at:

/llms.txt

Use this file to discover all available pages before exploring further.




Skip to main content


Turnkey
home page

Search...

⌘
K

Ask Assistant

Support

Blog

Contact us

Get started

Get started

Search...

Navigation

REST API

Stamps

Home

Solutions

Documentation

API & SDK reference

Security



REST API

Introduction

Stamps

Errors

Activities

Queries

Auth Proxy

SDK reference

Introduction

React wallet kit

React Native wallet kit

TypeScript core

Flutter

Swift

Kotlin

TypeScript server

Go

Ruby

Rust

Python

Web3 libraries

Advanced

Changelogs

SDK changelogs

API changelog

TVC changelog







On this page

API keys

WebAuthn

Stampers

REST API

Stamps

Copy page

Copy page

Every request made to Turnkey must include a signature over the POST body attached as a HTTP header. Our secure enclave applications use this signature to verify the integrity and authenticity of the request.

Copy page

Copy page

​

API keys

To create a valid, API key stamped request follow these steps:

1

Sign the JSON-encoded POST body with your API key to produce a
signature
(DER-encoded)

2

Hex encode the
signature

3

Create a JSON-encoded stamp:

publicKey
: the public key of the API key. Turnkey supports multiple API key curves:
API_KEY_CURVE_P256, API_KEY_CURVE_SECP256K1, API_KEY_CURVE_ED25519

signature
: the signature produced by the API key

scheme
: the signature scheme used to sign the request, matching the curve of the
publicKey
. The supported schemes are:
SIGNATURE_SCHEME_TK_API_P256, SIGNATURE_SCHEME_TK_API_SECP256K1, SIGNATURE_SCHEME_TK_API_ED25519, SIGNATURE_SCHEME_TK_API_SECP256K1_EIP191

4

Base64URL encode the stamp

5

Attach the encoded string to your request as a
X-Stamp
header

6

Submit the stamped request to Turnkey’s API

​

WebAuthn

To create a valid, WebAuthn authenticator stamped request follow these steps:

1

Compute the WebAuthn challenge by hashing the POST body bytes (JSON encoded) with SHA256. For example, if the POST body is
{"organization_id": "1234", "type": "ACTIVITY_TYPE_CREATE_API_KEYS", "params": {"for": "example"}}
, the WebAuthn challenge is the string
7e8b4653fc7e51dc119cea031942f4693b4742ceca4dda269b925802b38b2147

2

Include the challenge amongst WebAuthn signing options. Refer to the existing stamper implementations in the
following section
for examples

Note that if you need to pass the challenge as bytes, you’ll need to utf8-encode the challenge string (in JS, the challenge bytes will be
TextEncoder().encode("7e8b4653fc7e51dc119cea031942f4693b4742ceca4dda269b925802b38b2147")
)

Additional note for React Native contexts: the resulting string should then additionally be base64-encoded. See
implementation

3

Create a JSON-encoded stamp:

credentialId
: the id of the WebAuthn authenticator

authenticatorData
: the authenticator data produced by the WebAuthn assertion

clientDataJson
: the client data produced by the WebAuthn assertion

signature
: the signature produced by the WebAuthn assertion

4

Attach the JSON-encoded stamp to your request as a
X-Stamp-Webauthn
header

Header names are case-insensitive (so
X-Stamp-Webauthn
and
X-Stamp-WebAuthn
are considered equivalent)

Unlike API key stamps, the format is just JSON; no base64URL encoding necessary! For example:
X-Stamp-Webauthn: {"authenticatorData":"UaQZ...","clientDataJson":"eyJ0...","credentialId":"Grf...","signature":"MEQ..."}

5

Submit the stamped request to Turnkey’s API. If you would like your client request to be proxied through a backend, refer to the patterns mentioned
here
. An example application that uses this pattern can be found at wallet.tx.xyz (code
here
)

​

Stampers

Our
JS SDK
and
CLI
abstract request stamping for you. If you choose to use an independent client, you will need to implement this yourself. For reference, check out our implementations:

API Key Stamper

WebAuthn Stamper

React Native Stamper

iFrame Stamper

Telegram Cloud Storage Stamper

IndexedDb Stamper

CLI

Wallet Stamper

Our CLI has a
--no-post
option to generate stamps without sending anything over the network. This is a useful tool should you have trouble with debugging stamping-related logic. A sample command might look something like:

turnkey request --no-post --host api.turnkey.com --path /api/v

1

/sign --body '{

"payload"

:

"hello from TKHQ"

}'

{

"curlCommand"

:

"curl -X POST -d'{

\"

payload

\"

:

\"

hello from TKHQ

\"

}' -H'X-Stamp: eyJwdWJsaWNLZXkiOiIwMzI3YTUwMDMyZTZmMDYzMWQ1NjA1YjZhZGEzMmI3NzkwNzRmMzQ2ZTgxYjY4ZTEyODAxNjQwZjFjOWVlMDNkYWUiLCJzaWduYXR1cmUiOiIzMDQ0MDIyMDM2MjNkZWZkNjE4ZWIzZTIxOTk3MDQ5NjQwN2ViZTkyNDQ3MzE3ZGFkNzVlNDEyYmQ0YTYyNjdjM2I1ZTIyMjMwMjIwMjQ1Yjc0MDg0OGE3MmQwOGI2MGQ2Yzg0ZjMzOTczN2I2M2RiM2JjYmFkYjNiZDBkY2IxYmZiODY1NzE1ZDhiNSIsInNjaGVtZSI6IlNJR05BVFVSRV9TQ0hFTUVfVEtfQVBJX1AyNTYifQ' -v 'https://api.turnkey.com/api/v1/sign'"

,

"message"

:

"{

\"

payload

\"

:

\"

hello from TKHQ

\"

}"

,

"stamp"

:

"eyJwdWJsaWNLZXkiOiIwMzI3YTUwMDMyZTZmMDYzMWQ1NjA1YjZhZGEzMmI3NzkwNzRmMzQ2ZTgxYjY4ZTEyODAxNjQwZjFjOWVlMDNkYWUiLCJzaWduYXR1cmUiOiIzMDQ0MDIyMDM2MjNkZWZkNjE4ZWIzZTIxOTk3MDQ5NjQwN2ViZTkyNDQ3MzE3ZGFkNzVlNDEyYmQ0YTYyNjdjM2I1ZTIyMjMwMjIwMjQ1Yjc0MDg0OGE3MmQwOGI2MGQ2Yzg0ZjMzOTczN2I2M2RiM2JjYmFkYjNiZDBkY2IxYmZiODY1NzE1ZDhiNSIsInNjaGVtZSI6IlNJR05BVFVSRV9TQ0hFTUVfVEtfQVBJX1AyNTYifQ"

}

Was this page helpful?

Yes

No

Introduction

Errors

⌘
I

x

github

slack

linkedin

Powered by

This documentation is built and hosted on Mintlify, a developer documentation platform

Assistant

Responses are generated using AI and may contain mistakes.


