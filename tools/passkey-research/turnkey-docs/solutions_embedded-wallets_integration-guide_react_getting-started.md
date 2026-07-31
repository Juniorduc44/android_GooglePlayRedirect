# Source: https://docs.turnkey.com/solutions/embedded-wallets/integration-guide/react/getting-started






Getting started with Turnkey's Embedded Wallet Kit - Turnkey





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

React

Getting started with Turnkey's Embedded Wallet Kit

Home

Solutions

Documentation

API & SDK reference

Security



Overview

Introducing Turnkey Solutions

Embedded Wallets

Overview

Solution

Quickstart

Integration guide

Overview

React

Overview

Getting started

Authentication

Using embedded wallets

Using external wallets

Signing

UI customization

Sub-organization customization

Advanced API requests

Advanced backend authentication

Troubleshooting

Migrating from @turnkey/sdk-react

React Native

TypeScript

Flutter

Swift

Kotlin

Company Wallets

Overview

Solution

Quickstart

Integration guide

Key Management

Overview

Solution

Cookbooks

Overview

Yield

Swaps & bridges

Payments & infrastructure







On this page

Turnkey organization setup

Installation

Provider

Client readiness

ClientState values

Why client readiness matters

How to check client readiness

Next steps

React

Getting started with Turnkey's Embedded Wallet Kit

Copy page

Copy page

Learn how to set up the Embedded Wallet Kit (EWK) in your React application. This page will guide you through the initial setup, including enabling Turnkey’s Auth Proxy, installing the SDK, and configuring your app.

Copy page

Copy page

​

Turnkey organization setup

To start, you must create a Turnkey organization via the
Turnkey dashboard
. The steps to do so are described in the
Account Setup
section.

For this setup, we will be using Turnkey’s Auth Proxy to handle authentication. We can enable and configure this through the Turnkey dashboard.

1

Enable Auth Proxy

Navigate to the
Embedded Wallets → Configuration
section in the Turnkey Dashboard and enable the

Auth Proxy
.

2

Customize auth methods

You can choose which auth methods to enable and customize various options from this screen. For this quickstart, let’s enable
email OTP
and
passkeys
. When you’re done, click
Save
.

3

Finish up

Once you’re finished with the auth proxy setup, you can copy the
auth proxy config ID

and your
organization ID
from the dashboard.

These will be used in the next steps to configure your app.

​

Installation

You can use
@turnkey/react-wallet-kit
in any React based web application.

For this guide, let’s create a new
Next.js
app. If you already have an existing app, you don’t need to do this.

npx

npx

create-next-app@latest

Now, install the Turnkey React Wallet Kit package:

npm

pnpm

yarn

npm

install

@turnkey/react-wallet-kit

pnpm

add

@turnkey/react-wallet-kit

yarn

add

@turnkey/react-wallet-kit

Finally, create a
.env
file within your app directory, and populate it with the
IDs
from before

.env

NEXT_PUBLIC_ORGANIZATION_ID

=

"here"

NEXT_PUBLIC_AUTH_PROXY_CONFIG_ID

=

"and_here"

​

Provider

Wrap your app with the
TurnkeyProvider
component, and import
"@turnkey/react-wallet-kit/styles.css"
to include styles for the UI components.

For Tailwind CSS V3 users:

Please refer to the
Tailwind V3 error
in the
troubleshooting
section for specific instructions on how to import the styles correctly.

You can continue with the guide as normal if you are using
Tailwind CSS V4
or
not using Tailwind CSS at all
.

With Next.js App Router, keep
app/layout.tsx
as a server component and create a separate
app/providers.tsx
client wrapper. This is necessary if you want to pass callbacks (e.g., onError), which must be defined in a client component.

app/providers.tsx

"use client"

;

import

{

TurnkeyProvider

,

TurnkeyProviderConfig

,

}

from

"@turnkey/react-wallet-kit"

;

const

turnkeyConfig

:

TurnkeyProviderConfig

=

{

organizationId:

process

.

env

.

NEXT_PUBLIC_ORGANIZATION_ID

!

,

authProxyConfigId:

process

.

env

.

NEXT_PUBLIC_AUTH_PROXY_CONFIG_ID

!

,

};

export

function

Providers

({

children

}

:

{

children

:

React

.

ReactNode

}) {

return

<

TurnkeyProvider

config

=

{

turnkeyConfig

}

callbacks

=

{

{

onError

:

(

error

)

=>

console

.

error

(

"Turnkey error:"

,

error

),

}

}

>

{

children

}

</

TurnkeyProvider

>

;

}

In case anything goes wrong, we’ve added an
onError
callback to the
TurnkeyProvider
to catch any errors.

Then, use the
Providers
component to wrap your app in
app/layout.tsx
.

app/layout.tsx

import

"@turnkey/react-wallet-kit/styles.css"

;

import

"./globals.css"

;

import

{

Providers

}

from

"./providers"

;

export

default

function

RootLayout

({

children

,

}

:

{

children

:

React

.

ReactNode

;

})

{

return

(

<

html

lang

=

"en"

>

<

body

>

<

Providers

>

{

children

}

</

Providers

>

</

body

>

</

html

>

);

}

Why this pattern?

Callbacks (and other interactive bits) must be declared in a client component.

Keeping layout.tsx as a server component maintains optimal rendering and avoids unnecessarily making your entire app client-side.

Centralizing Turnkey setup in app/providers.tsx keeps configuration, styles, and callbacks in one place.

​

Client readiness

The
ClientState
enum tracks the initialization status of the Turnkey client. Before performing any auth or wallet operations, you must wait for the client to reach it’s
Ready
state

​

ClientState
values

export

enum

ClientState

{

Loading

=

"loading"

,

Ready

=

"ready"

,

Error

=

"error"

,

}

​

Why client readiness matters

The client performs several asynchronous operations during initialization:

Configuration Loading - Fetches auth proxy config if configured

TurnkeyClient Initialization - Sets up the HTTP client, passkey, api key & wallet stampers, and wallet providers

Session Restoration - Loads and validates existing sessions from storage

Wallet Provider Setup - Initializes wallet connection listeners

Calling methods before Ready can cause:

Race conditions with session management

Missing configuration errors

Failed client operations

​

How to check client readiness

Access
clientState
via the
useTurnkey
hook:

import

{

ClientState

,

AuthState

,

useTurnkey

}

from

"@turnkey/react-wallet-kit"

;

function

MyComponent

() {

const

{

clientState

,

authState

}

=

useTurnkey

();

// Show loading spinner while client initializes

if

(

clientState

===

undefined

||

clientState

===

ClientState

.

Loading

) {

return

<

LoadingSpinner

/>;

}

// Handle client initialization errors

if

(

clientState

===

ClientState

.

Error

) {

return

<

ErrorMessage

/>;

}

// Client is ready - render your UI

return

<

Content

/>;

}

The client starts as undefined, moves to Loading during initialization, then settles on either Ready (success) or Error (failure).

​

Next steps

Ready to start building your app? Check out the
Authentication
guide to learn how to set up login or signup with just one line of code!

Was this page helpful?

Yes

No

Overview

Authentication

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


