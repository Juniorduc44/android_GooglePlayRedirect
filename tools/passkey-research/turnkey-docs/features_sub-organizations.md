# Source: https://docs.turnkey.com/features/sub-organizations






Sub-organizations - Turnkey





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

Organizations

Sub-organizations

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

Overview

Sub-organizations

Users

Authentication

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

Creating sub-organizations

Using sub-organizations

Deleting sub-organizations

Organizations

Sub-organizations

Copy page

Copy page

Using Turnkey’s flexible infrastructure, you can programmatically create and manage sub-organizations for your end-users. Sub-organizations aren’t subject to size limits: you can create as many sub-organizations as needed. The parent organization has
read-only
visibility into all of its sub-organizations, and activities performed in sub-organizations roll up to the parent for billing purposes.

Copy page

Copy page

We envision sub-organizations being very useful to model your end-users if you’re a business using Turnkey for key management. Let’s explore how.

​

Creating sub-organizations

Creating a new sub-organization is an activity performed by the parent organization. The activity itself takes the following attributes as inputs:

organization name

a list of root users

a root quorum threshold

[optional] a wallet (note: in versions prior to V4, this was a private key)

Root users can be programmatic or human, with one or many credentials attached.

​

Using sub-organizations

You can use this primitive to model end-user controlled wallets or custodial wallets. If you have another use-case in mind, or questions/feedback on this page, reach out to
welcome@turnkey.com
!

​

Deleting sub-organizations

To delete a sub-organization, you can use the
delete sub-organization activity
.
Before proceeding, ensure that all private keys and wallets within the sub-organization have been exported to prevent any loss of funds.
Alternatively, you can set the
deleteWithoutExport
parameter to
true
to bypass this requirement.
By default, the
deleteWithoutExport
parameter is set to
false
.

This activity must be initiated by a root user in the sub-organization that is
to be deleted. A parent org cannot delete a sub-organization without its
participation.

Was this page helpful?

Yes

No

Overview

Overview

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


