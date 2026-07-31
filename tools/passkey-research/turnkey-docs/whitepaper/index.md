# Source: https://whitepaper.turnkey.com/










Turnkey Whitepaper

















Index






Principles






Foundations






Architecture






Applications




















Turnkey: a Verifiable Key Management Solution

Turnkey Team

January 2025

Turnkey is the first verifiable key management system of its kind. This live system has operated for 2+ years and secures millions of wallets and private keys for a wide variety of use cases including embedded wallets, smart contract deployments, payments, treasury management, and AI deployments.

Structure of this whitepaper

There is a lot to explain so we've split the content in multiple documents to make it easier to consume.

Key Management Re-imagined from First Principles

This document details Turnkey's drastically different vision. After explaining why we think now is the time to re-imagine key management, we talk about our ambitious security model, our bet on asymmetric cryptography for authentication, and our belief that curve-level operations are the correct building blocks for modern crypto-asset operations. By the end of this document you'll understand the motivations behind Turnkey and should be ready to jump to our technical design with that context in mind.

Verifiable Foundations

This document explains the foundations on which Turnkey is built. We'll see what Trusted Execution Environments (“TEEs”) are and how we use them. We'll introduce QuorumOS (“QOS”), a new minimal, open-source operating system engineered for verifiability which runs inside of all Turnkey enclaves. We also introduce StageX, a new Linux distro which solves the reproducibility problem and supports all secure builds at Turnkey today. Finally we'll see how these components prove the software running inside of TEEs all the way down to the application source code.

Turnkey's Architecture

We offer an engineering-focused tour of our system as it is running today, with a focus on the enclave applications Turnkey runs internally. We'll see the challenges that emerge from running on top of TEEs, the main one being: TEEs do not store state. We'll also talk about our API design and digital signature scheme for authentication. By the end of this document you'll have a full understanding of Turnkey's design and the crucial role that enclave applications play within it.

Applications Beyond Key Management

In this last document we focus on what's possible to build today and tomorrow. While Turnkey has chosen key management as the first class of applications on top of its verifiable foundations, we envision it to be relevant well beyond that. We'll explain why we think QuorumOS is a valuable platform to run any application, provisioned with centralized or decentralized quorum sets, held by humans, servers or AI agents.

Acknowledgements

I’d like to sincerely thank Jack Kearney, Bryce Ferguson, Hannah Arnold, Michael Avrukin, Sarah Lu, Zane Kharitonov, Carolyn Philip, Raheel Ahmed, Samuel Ebstein, Hao Su, Andrew Min, Mark Nesbitt, Brian Esler, Lance Vick, Seán McCord, Robin Arenson, Mohammed Odeh, and Amir Cheikh for their invaluable feedback, thoughtful insights, and unwavering support throughout the writing of this whitepaper. Their suggestions have significantly enhanced its clarity, precision, and depth.

Beyond those named, I am especially grateful for the big ideas that exist out in the open, waiting to be captured, shaped, and implemented. Thank you to the anonymous and generous minds who spend their time and energy sharing knowledge publicly. That spirit of openness and collaboration has been instrumental in bringing Turnkey, and this whitepaper, to life. I hope others are inspired to build upon what is presented here in the same way.

Arnaud Brousseau, Founding Engineer, Turnkey



















