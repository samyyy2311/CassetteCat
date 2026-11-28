# Security Policy

## Supported versions

Security fixes go into the latest release of CassetteCat for Android. Older versions don't receive fixes, so please update before reporting.

## Reporting a vulnerability

Please report security problems privately through [GitHub's private vulnerability reporting](https://github.com/samyyy2311/CassetteCat/security/advisories/new). Don't open a public issue, pull request or discussion for them.

A useful report includes:

- what the problem is and what an attacker could do with it
- the steps to reproduce it, or a small proof of concept
- the app version, Android version and device
- the part of the app involved, such as server sign-in, saved credentials, backups, the Listening Room or pairing with a computer

Leave out real passwords, tokens and personal data. Test accounts and sample files are enough.

## What happens next

Each report is read and checked. If the problem is confirmed, it's fixed in a new release, and the advisory is published once that release is out. If you'd like, you'll be credited in the advisory and the release notes.

Please keep the details private until the fix has been released.

## Scope

In scope:

- the CassetteCat Android app in this repository
- how it stores and sends credentials for music servers and scrobbling accounts
- the Listening Room and pairing with CassetteCat on a computer
- backups and restores

Out of scope:

- problems in the services CassetteCat connects to, such as Jellyfin, Navidrome, Last.fm or ListenBrainz, which should go to those projects
- attacks that need a rooted device or an unlocked phone in someone else's hands
- denial of service by flooding a server or the local network
