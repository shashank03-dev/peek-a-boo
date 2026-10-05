# Pro roadmap

Ideas from looking at the Galaxy S26 Ultra Privacy Display, other anti-peep apps on Play, and the
"intruder selfie" category. Ordered by how much a subscriber would notice them for the effort.

## Shipped in 2.0

- **Privacy Shield**: auto-on when someone peeks, or always on. Louver, Dim and Grain looks, strength slider.
- **Blackout on peek**, tap to reveal.
- **Protected apps**: shield only in chosen apps. This is the S26's per-app privacy display, plus it's automatic.
- **Someone-else alert**: logs a photo when a stranger is using the unlocked phone.
- **CSV export**.

## Next

1. **Notification privacy**: when a peek starts, hide notification content (heads-up banners). Needs
   a NotificationListenerService to snooze or re-post. Play allows it with a clear disclosure. The S26
   does "partial" privacy for notifications. This would be our version.
2. **Shield zones**: shield only the bottom half (the keyboard and messages), like the S26's partial mode.
   Easy: same overlay, different bounds.
3. **Quick Settings tile for the shield**: one tap, always-on shield, no app open.
4. **Home-screen widget**: today's peeks plus a shield toggle.
5. **Peeker gallery**: name repeat peekers ("Office guy") and get per-person counts. Builds on the
   face-signature clustering already in `Reports.uniquePeople`.
6. **Smart schedule / places**: guard and shield only on weekday commutes, or only away from home
   Wi-Fi (needs ACCESS_WIFI_STATE; still no internet).
7. **Weekly digest notification**: "14 peeks this week, busiest Tuesday 6 PM". This is a retention driver.

## Considered and parked

- **Wrong-PIN intruder selfie**: needs a device-admin password-failure listener. Device admin is
  heavily restricted on Play and the guard already covers "someone else is using my phone".
- **Auto-lock when a stranger holds the phone**: needs device admin (`lockNow`). Same problem.
- **Going to the home screen on a peek**: needs an accessibility service. Play's accessibility
  policy would almost certainly reject this use. Blackout gives the same result without it.
