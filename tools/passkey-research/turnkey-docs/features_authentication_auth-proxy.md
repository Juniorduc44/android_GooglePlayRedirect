# Source: https://docs.turnkey.com/features/authentication/auth-proxy






Auth Proxy - Turnkey





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

Authentication

Auth Proxy

Home

Solutions

Documentation

API & SDK reference

Security



Get started

About Turnkey

Account setup

Using LLMs

Code examples

Going live

Features

Organizations

Users

Authentication

Overview

Auth methods

Auth Proxy

Sessions

Advanced

Wallet and key management

Chain support

Policies

Transaction management

Turnkey Verifiable Cloud

IP Allowlist

Webhooks

Reference

Resource limits

Migrating to Turnkey

FAQ







On this page

Overview

How it works

Authentication & headers

Auth flows

OTP (email or SMS)

OAuth (OIDC providers — Google, Apple, etc.)

OAuth (OAuth 2.0-only providers — Discord, X/Twitter, etc.)

Signup (create sub-organization)

Account lookup

Wallet Kit config

Dashboard configuration

Authentication

Auth Proxy

Copy page

Copy page

Use Turnkey’s managed Auth Proxy to securely run OTP/OAuth/signup flows without standing up your own backend.

Copy page

Copy page

​

Overview

The
Turnkey Auth Proxy
is a managed, multi-tenant service that signs and forwards authentication requests to the Turnkey API on your behalf — no backend required for auth flows. It handles sub-organization creation, OTP (email/SMS), and OAuth login.

Host:

https://authproxy.turnkey.com

What it does:
Validates origin, looks up your org’s proxy config, signs the request with a proxy-scoped API key, and forwards it to the Turnkey API.

What it doesn’t do:
It cannot authenticate users without their participation (OTP code entry, OAuth consent). It has no access to funds or broader org operations.

Enable and configure the Auth Proxy in
Dashboard → Embedded Wallets → Configuration
.

​

How it works

Enable in Dashboard
— Toggle Auth Proxy ON. Turnkey creates a proxy user and proxy API key, stored encrypted in your org’s auth proxy config.

Configure allowed origins
— Only requests from these origins may call the proxy (CORS + origin validation). Defaults to
*
. Exact URLs only — partial wildcards like
https://*.myapp.com
are not supported.

Your frontend calls the Auth Proxy
— Pass your
X-Auth-Proxy-Config-Id
header with every request.

Proxy signs and forwards
— The proxy decrypts your proxy key in-memory (per request only), signs the activity, and forwards it to the Turnkey API.

Response returned
— The proxy returns the result (e.g.,
organizationId
,
session
) directly to your frontend.

Security notes:

Proxy keys are HPKE-encrypted inside Turnkey’s enclaves and decrypted per-request only, in-memory.

Strict separation from Turnkey’s core backend — communicates via public API only.

The Auth Proxy passes App Proofs through to the caller without verifying them. End-users (SDKs) are expected to perform App Proof verification. See
Turnkey Verified
.

​

Authentication & headers

Auth Proxy Config Id
(required) — identifies your org’s proxy config. Found in
Dashboard → Embedded Wallets → Configuration
.

X-Auth-Proxy-Config-Id: <your-auth-proxy-config-id>

CORS & Origin
— requests must originate from a whitelisted origin set in the dashboard.

​

Auth flows

​

OTP (email or SMS)

Used to authenticate users with a one-time code sent to their email or phone number. For a walkthrough of the user experience, see
Email auth — User experience
.

Flow:

Init OTP


POST /v1/otp_init_v2
— Send an OTP code to the user’s email or phone. Returns an
otpId
and an
otpEncryptionTargetBundle
.

otpEncryptionTargetBundle
is a TEE-signed bundle containing a target P-256 encryption key. The client uses it to encrypt the OTP code before submission, so the code is only decryptable inside the enclave. If you’re using
react-wallet-kit
this is handled automatically — see the
otp-auth/without-backend
example.

Verify OTP


POST /v1/otp_verify_v2
— Submit the OTP code along with the
otpId
. Returns a
verificationToken
— a short-lived, enclave-signed token proving the user owns the contact.

OTP Login


POST /v1/otp_login_v2
— Exchange the
verificationToken
+ a client
publicKey
+
clientSignature
for a session JWT. The session is scoped to the sub-org matching the verified contact.

clientSignature
is the client’s signature over the
verificationToken
using the private key corresponding to
publicKey
. This binds the session to the client’s keypair and prevents replay attacks.

Pre-verified signup:
You can also pass a
verificationToken
directly to
Signup
to create a new sub-org for a user who has already verified their contact via OTP — skipping a separate OTP round-trip on registration.

​

OAuth (OIDC providers — Google, Apple, etc.)

Used when your app receives an OIDC token directly from a provider like Google or Apple.

OAuth Login


POST /v1/oauth_login
— Submit the
oidcToken
and a client
publicKey
. Returns a session JWT for the sub-org that has a matching OAuth provider (
iss
,
sub
,
aud
).

​

OAuth (OAuth 2.0-only providers — Discord, X/Twitter, etc.)

Used for providers that don’t issue OIDC tokens natively. Turnkey acts as an OIDC wrapper.

OAuth2 Authenticate


POST /v1/oauth2_authenticate
— Submit the OAuth 2.0
authCode
,
redirectUri
, PKCE
codeVerifier
,
nonce
, and
clientId
. Turnkey exchanges the code, calls the provider’s user-info endpoint, and returns a Turnkey-issued OIDC token.

OAuth Login


POST /v1/oauth_login
— Use the Turnkey-issued token from step 1, same as a standard OIDC flow.

​

Signup (create sub-organization)

Signup


POST /v1/signup_v2
— Create a new sub-organization for an end user. Optionally creates a wallet and sets up credentials (API keys, passkeys, OAuth providers) in a single call.

Notable fields:

verificationToken
— if provided, the sub-org is created with the user’s contact (email/phone) marked as verified (requires a prior OTP verify step).

oauthProviders
— accepts either
oidcToken
(verified) or
oidcClaims
(
{iss, sub, aud}
, verified against an accompanying token). See
Multi-platform OAuth identities
.

wallet
— if provided, a wallet with the specified accounts is created atomically with the sub-org.

clientSignature
— optional client-side signature binding the signup request to a keypair.

​

Account lookup

Get Account


POST /v1/account
— Look up a sub-org by
filterType
(
EMAIL
,
PHONE_NUMBER
,
CREDENTIAL_ID
,
OIDC_TOKEN
, etc.) and
filterValue
. Returns the
organizationId
of the matching sub-org, or an empty response if none exists. Used to check whether a user already has an account before deciding to call signup vs. login.

​

Wallet Kit config

Get Wallet Kit Config


POST /v1/wallet_kit_config
— Returns the auth method toggles and session configuration for the calling org. Used by
react-wallet-kit
to determine which auth methods to show in the modal.

​

Dashboard configuration

Each auth method can be independently enabled or disabled — including email OTP, SMS OTP, individual social providers (Google, Apple, X, Discord), passkeys, and external wallets.

Setting

Description

Enable/Disable

Toggle the Auth Proxy on or off for your org

Config ID

Your
X-Auth-Proxy-Config-Id
value

Allowed Origins

Exact frontend URLs allowed to call the proxy. Defaults to
*

Session expiration

Default session lifetime in seconds (default: 900)

OTP length & type

Code length (6–9 digits) and character set (numeric or alphanumeric)

Email customization

Application name and logo URL shown in OTP emails

OAuth redirect URL

Redirect URI registered in your OAuth provider’s console

Social logins

Per-provider toggles (Google, Apple, X, Discord) with client IDs

Social linking

Google client IDs allowed to auto-link a Google account to an existing email OTP account with the same email

Passkey / Wallet

Show or hide passkey and external wallet options in the Wallet Kit modal

Require verification token for account lookups

Gate the
/account
endpoint behind a prior OTP verification

Was this page helpful?

Yes

No

Bring your own auth

Sessions

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


