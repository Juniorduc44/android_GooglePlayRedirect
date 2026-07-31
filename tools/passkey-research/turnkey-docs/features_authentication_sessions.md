# Source: https://docs.turnkey.com/features/authentication/sessions






Sessions - Turnkey





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

Sessions

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

What is a session?

How can I create a session?

Read-only sessions

Parent organization access

Client side access

Read-write sessions

Creating a read-write session

Mechanisms

IndexedDB (web only):

SecureStorage (mobile only)

LocalStorage:

Sessions FAQ

Authentication

Sessions

Copy page

Copy page

Turnkey sessions allow a user to take multiple, contiguous actions in a defined period of time.

Copy page

Copy page

​

What is a session?

Such actions can be divided into two buckets:

Read operations: Retrieving data (e.g., viewing wallet balances)

Write operations: Modifying data or performing sensitive actions (e.g., signing transactions)

​

How can I create a session?

​

Read-only sessions

In terms of end-user experience, a read-only session might make sense in low-touch applications where users are primarily reading data (think viewing wallets and their balances). As for implementation, there are a few ways a developer can achieve read-only access on behalf of a user. Note: an end-user (sub-organization) falls hierarchically under the developer (parent-organization).

​

Parent organization access

By default, a parent organization has read access to all of its sub-organizations’ data. This means you can set up a federated model where the client makes requests to a backend (containing the parent organization’s API key credentials), the backend populates the requested data, and returns it back to the client. From an implementation perspective, each read request (i.e.
get
or
list
) requires an
organizationId
parameter. Populate that field with the sub-organization’s ID in order to get its data.

​

Client side access

Separately, if you would like the client to have all read requests encapsulated (instead of reading data via a proxy like in the previous approach), the client can initiate a read-only session via a
CreateReadOnlySession activity
. This activity returns a session string that, if passed into an HTTP request via
X-Session
header, gives permission to perform reads. Note that because this is an activity performed by an end user, it still requires authentication (for example, via passkey).

Turnkey’s current SDKs, including
@turnkey/react-wallet-kit
and
@turnkey/core
, do not support read-only sessions. These SDKs are designed around cryptographically stamped, read-write sessions and do not expose the divergent request paths required for token-based read-only authentication.
If you need read-only session support, you must either interact with the Turnkey API directly and attach the session token to requests manually, or use older / legacy SDK
implementations
that still support token-based read-only sessions.

For most applications, read-write sessions are recommended. By default, a parent organization already has read access to all of its sub-organizations’ data, allowing clients to fetch read-only data via a backend using the parent organization’s API credentials, without requiring a separate read-only client session.

​

Read-write sessions

In contrast to read-only sessions, a read-write session makes sense when a user would like to make several authenticated write requests in a window of time. There are a few ways to achieve this:

​

Creating a read-write session

There are several mechanisms to obtain read-write sessions: OTP, OAuth, Passkey sessions, and Session refreshing

Our SDK contains several abstractions that manage authentication. You can checkout all of our examples leveraging these examples
here

Note:
The session JWT is only metadata signed by Turnkey that references the client side stored API keypair, and is useful for verifying the session server-side or associating metadata, but it cannot be used to authenticate requests to Turnkey’s API.
Only the session keypair can be used to create valid
x-stamp
signatures for API requests to Turnkey. In other words, solely the session JWT cannot be used to stamp requests outside of the client context.

​

Mechanisms

There are two primary mechanisms we offer that provide client side key generation and signing to support read-write sessions.

​

IndexedDB (web only):

For web apps that want stronger session persistence without relying on iframes or exposing credentials to your app’s JavaScript runtime, Turnkey supports using the
SubtleCrypto
API to generate unextractable asymmetric key pairs and store them securely in the browser’s IndexedDB.

This approach enables long-lived, client-held sessions that survive page reloads, tab closures, and even browser restarts –
without ever exposing the private key
to your JavaScript code.

Turnkey’s SDK provides helpers to:

Create a new session by generating a P-256 key pair via
crypto.subtle.generateKey()

Sign requests

Store and retrieve the key using IndexedDB under a given session ID

Abstractions built on top of the
IndexedDBStamper
that simplify authentication flows

This is currently the
most persistent
session model for modern browsers that support WebCrypto. It is especially valuable in Progressive Web App (PWA) contexts or when iframe and Local Storage approaches are insufficient.

To see the IndexedDB-backed session mechanism in action, check out our Turnkey react-wallet-kit
playground
or dedicated authentication examples like
oauth
,
email OTP
, and
external wallet authentication
.
Our
Web demo application
hosted at
wallets.turnkey.com
showcases complete end-to-end authentication flows and client-side session persistence backed by IndexedDB.

​

SecureStorage (mobile only)

Secure Storage operates essentially the same as IndexedDB with respect to authentication flows for Turnkey except it is mobile native and keys are generated using @turnkey/crypto rather than WebCrypto.

​

LocalStorage:

Another option is to create an API key and store it directly within Local Storage. However, this is a riskier setup than IndexedDb/SecureStorage as anyone who is able to access this client-side API key has full access to a User.

​

Sessions FAQ

How can I refresh a session?

Once a user has a valid session, it is trivial to use that session to create a new session. The
refreshSession
abstraction will create a brand new session and automatically store the resulting new session in local storage.

How can I delete a session?

In order to delete a session, simply remove all user-related artifacts from Local Storage.

/**

* Clears out all data pertaining to a user session.

*

*

@returns

{Promise<boolean>}

*/

logout

=

async

()

:

Promise

<

boolean

>

=>

{

await

removeStorageValue

(

StorageKeys

.

Client

);

await

removeStorageValue

(

StorageKeys

.

Session

);

return

true

;

};

How long are sessions?

The expiration of session keys can be specified to any amount of time using the
expirationSeconds
parameter. The default length is 900 seconds (15 minutes).

How many session keys can be active at once?

A user can have up to 10 expiring API keys at any given time. If you create an expiring API key that exceeds that limit, Turnkey automatically deletes one of your existing keys using the following priority:

Expired API keys are deleted first

If no expired keys exist, the oldest unexpired key is deleted
If you are looking to invalidate existing sessions, you can use the
invalidateExisting
parameter for all
_LOGIN
activities. This will clear all existing session keys.

Can I use the same sessions implementation for web and mobile?

Absolutely! Through leveraging IndexedDb you can handle sessions in the same way for Web and PWAs. However, for React Native/Mobile applications you will need to leverage Secure Storage.

Was this page helpful?

Yes

No

Auth Proxy

Proxying signed requests

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


