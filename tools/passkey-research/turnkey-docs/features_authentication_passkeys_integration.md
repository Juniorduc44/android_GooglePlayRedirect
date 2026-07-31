# Source: https://docs.turnkey.com/features/authentication/passkeys/integration






Integrating Passkeys - Turnkey





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

Passkeys

Integrating Passkeys

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

Email auth & recovery

Social logins

SMS

OTP migration guide

Passkeys

Introduction to Passkeys

Integrating Passkeys

Passkey options

Native Passkeys

Discoverable vs. non-discoverable

Backend authentication

Bring your own auth

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

Passkey flow

Our SDK can help

Passkey wallets with sub-organizations

Passkeys

Integrating Passkeys

Copy page

Copy page

Copy page

Copy page

​

Passkey flow

A typical passkey flow is composed of 4 main steps, depicted below:

Your app frontend triggers a passkey prompt.

Your end-user uses their device to produce a signature with their passkey, and a signed request is produced.

The signed request is forwarded to your backend. This step is optional, see
“To Proxy or not to proxy”
below for more information.

The signed request is verified within a Turnkey secure enclave.

This flow happens once for
registration
and for each subsequent
authentication
or signature request. The main difference is the browser APIs used to trigger the passkey prompt in step (1):

Passkey registration
uses
navigator.credentials.create
(as described in
this guide
).
navigator.credentials.create
triggers the creation of a
new
passkey.

Passkey authentication
uses
navigator.credentials.get
. See
this guide
for more information.
navigator.credentials.get
triggers a signature prompt for an
existing
passkey.

​

Our SDK can help

Our SDK has integrated passkey functionality, and we’ve built examples to help you get started.

@turnkey/http

has a helper to trigger passkey registration (
getWebAuthnAttestation
). You can see passkey registration in action in our

with-federated-passkeys

example:
example code

@turnkey/webauthn-stamper

is a passkey-compatible stamper which integrates seamlessly with
TurnkeyClient
:

import

{

WebauthnStamper

}

from

"@turnkey/webauthn-stamper"

;

import

{

TurnkeyClient

,

createActivityPoller

}

from

"@turnkey/http"

;

const

stamper

=

new

WebauthnStamper

({

rpId:

"your.app.xyz"

,

});

// New HTTP client able to sign with passkeys

const

httpClient

=

new

TurnkeyClient

(

{

baseUrl:

"https://api.turnkey.com"

},

stamper

);

// This will produce a signed request that can be POSTed from anywhere.

// The `signedRequest` has a URL, a POST body, and a "stamp" (HTTP header name and value)

const

signedRequest

=

await

httpClient

.

stampCreatePrivateKeys

(

...

)

// Alternatively, you can POST directly from your frontend.

// Our HTTP client will use the webauthn stamper and the configured baseUrl automatically!

const

activityPoller

=

createActivityPoller

({

client:

client

,

requestFn:

client

.

createPrivateKeys

,

});

// Contains the activity result; no backend proxy needed!

const

completedActivity

=

await

activityPoller

({

type:

"ACTIVITY_TYPE_CREATE_PRIVATE_KEYS_V2"

,

// (omitting the rest of this for brevity)

})

@turnkey/viem

is a package wrapping all of the above so that you work directly with Viem without worrying about passkeys. See
this demo
.

Regardless of whether you use our helpers and abstractions, take a look at
our registration and authentication options guide
. This will help you choose the right options for your passkey flow.

If you have questions, feedback, or find yourself in need of an abstraction or integration that doesn’t exist yet, please get in touch with us! You can

Create an
issue on our SDK repo

Join our slack community
here

Contact us at
hello@turnkey.com

We’re here to make this as easy as possible for you and your team!

​

Passkey wallets with sub-organizations

If you’re wondering how to create independent, non-custodial wallets for your end-users, head to
Sub-Organizations
. In short: you’ll be able to pass the registered passkeys as part of a “create sub-organization” activity, making your end-users the sole owners of any resource created within the sub-organization (including private keys). Your organization will only have read permissions.

Was this page helpful?

Yes

No

Introduction to Passkeys

Passkey options

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


