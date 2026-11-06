# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

Scope note: this record was written for the Android client in `android-client/`. The Electron desktop client in the repo root shares the product but keeps its own look for now.

## Users
A small circle of friends and subscribers who get access keys from the MPA Telegram bot. Most are not technical. Their job: open the app, press one control, and have the internet work, including Russian sites that must stay direct. Occasionally they add a key, switch server, or exclude an app.

## Product Purpose
MPA (Mariittov Private Access) is a private VPN client on sing-box using VLESS + XTLS-Vision + Reality. Success means a person connects in one tap, understands at a glance whether they are protected and through which server, and never needs to know what VLESS or Reality is.

## Positioning
A private service run by one person for people they know, not a commercial VPN storefront. Smart routing sends Russian sites directly and everything else through the tunnel, so users never toggle the VPN for banks or Gosuslugi.

## Operating Context
Phone, one hand, often in a hurry (before opening a blocked site). Keys arrive as `vless://` links, subscription URLs, activation keys, or QR codes from the Telegram bot. Subscriptions refresh hourly in the background. Updates ship as APKs from GitHub releases and install from inside the app.

## Capabilities and Constraints
- Inputs: `vless://` link, subscription URL, activation key, QR scan.
- Servers grouped by subscription; groups can be renamed and force-refreshed.
- Per-server TCP ping; switching server while connected reconnects.
- Split tunnelling per app (bypass list or only-these list).
- RU rule-set download for smart routing.
- In-app update banner (download + install APK).
- Foreground service notification with Disconnect; auto-start on boot.
- Interface language: Russian.
- Fonts and libbox.aar are not in git; builds fetch them manually (see README).

## Brand Commitments
- Name: MPA. Interface copy is Russian, informal "ты".
- The user asked for a new mobile look (not tied to the desktop palette) and a new launcher icon.

## Evidence on Hand
No testimonials, user counts, speeds, or server locations to claim. Do not invent them.

## Product Principles
1. One tap to protection; everything else is secondary.
2. State is never ambiguous: connected, connecting, off, or failed, with the reason.
3. Hide protocol jargon from people who do not need it; keep details reachable for the owner.
4. Native Android behaviour: Back works, insets respected, standard controls.

## Accessibility & Inclusion
48 dp touch targets, sp text that follows system font scale, state never conveyed by colour alone.
