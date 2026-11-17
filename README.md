# AntennaPod Mix

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub Release](https://img.shields.io/github/v/release/dcdude71/AntennaPod)](https://github.com/dcdude71/AntennaPod/releases)

AntennaPod Mix is a personal fork of [AntennaPod](https://github.com/AntennaPod/AntennaPod), the easy-to-use, flexible and open-source podcast manager for Android. I keep it close to upstream and add a few features I wanted for my own listening.

**This is not the official AntennaPod app**, and it is not affiliated with or endorsed by the AntennaPod team. If you are looking for AntennaPod, you will be much happier with the official version:

- Website: [antennapod.org](https://antennapod.org/)
- [Google Play](https://play.google.com/store/apps/details?id=de.danoeh.antennapod) · [F-Droid](https://f-droid.org/app/de.danoeh.antennapod) · [GitHub](https://github.com/AntennaPod/AntennaPod)

## What's different in this fork

### Rule-based playlists
Next to the regular queue, you can create any number of playlists. A playlist is built from rules, one per podcast:

- **How many episodes:** all unplayed episodes, or a limit (for example 3)
- **Which ones:** the most recent episodes, or the oldest unplayed ones
- **Play order:** oldest first or newest first
- **Priority (1-10):** priority 1 podcasts play first, then priority 2, and so on. Podcasts with the same priority follow the order of the rules, which you can change by dragging.

The playlist updates itself as new episodes come in and as you listen.

Example: a "Morning" playlist with the 2 most recent episodes of a news podcast (priority 1), then the next unplayed episode of a series you are catching up on (priority 2).

#### How to use playlists
1. Open the **Playlists** tab and tap **+** to create a playlist.
2. Tap the playlist, then the **pencil** (Edit rules) and **Add podcast**. Choose how many episodes, which ones, the play order and the priority. Swipe a rule away to remove it, or drag it to reorder.
3. Back on the playlist, press the **play** button at the top to play it from the start. To play everything, press **play** at the top of the Playlists tab: it starts with your first playlist that has episodes.

Playlists play from the top only. To hear one particular episode, open its podcast and play it from there (that uses the normal queue).

When an episode ends, the next one comes from the playlist, never from your queue, and playing from a playlist never adds episodes to the queue. Starting an episode from anywhere else (the queue, the inbox, a podcast page) takes you back to the normal queue.

When a playlist runs out, the app moves on to the next playlist in the order of your Playlists tab, skipping empty ones. You can change this in Settings → Playback → **At the end of a playlist**:

- **Continue with the next playlist** (default)
- **Stop**

Either way, playback never falls back to the queue.

#### Automatic downloads for playlists
In a playlist's menu (⋮), turn on **Auto-download episodes** to keep its episodes downloaded, following your usual download settings (Wi-Fi only, battery, and so on). These downloads:

- are not added to the queue,
- are kept when the app cleans up downloads to make room,
- do not count toward the episode limit in Settings → Downloads → Automatic download. That limit is left entirely to your podcasts' own automatic downloads.

To keep a playlist from filling your storage, every rule in a playlist with auto-download needs a number of episodes, up to 10 per podcast. "All unplayed episodes" is not available there. If a playlist already uses it, the app asks you to edit its rules before turning on auto-download.

The fork also fixes a problem where automatic downloads could stop altogether once more episodes were downloaded than the episode limit allows.

#### Playlists in Android Auto
In a playlist's menu (⋮), **Show in Android Auto** adds it to a **Playlists** folder in Android Auto, right after "Continue listening". It is on for new playlists; playlists created before this version start with it off. Starting an episode from that folder plays the playlist, just like on the phone.

#### Playlists on your home screen
- **Playlists widget (4×1):** add it from your launcher's widget list, then pick up to four playlists and drag them into order. Tap a tile to start that playlist from the top; the playing one turns into a pause button. It squeezes four compact tiles onto a narrow screen and uses wider tiles when there is room. Long-press the widget and choose its settings to change the playlists.
- **One playlist as an icon:** in a playlist, open the menu (⋮) and choose **Add to home screen**. Tapping the icon starts the playlist without opening the app.
- **Long-press the app icon** to start one of your first playlists directly. Most launchers only show four shortcuts, and AntennaPod's own Queue and Episodes shortcuts come first, so you will see your first two playlists.

### Advanced episode filter
AntennaPod's own episode filter (in a podcast's settings) looks at titles only and lets you pick either words to include or words to exclude. AntennaPod Mix adds an advanced filter. Turn it on in Settings → Downloads → Automatic download → **Episode filter style → Advanced**. A podcast's **Episode filter** then opens a full screen where you can:

- use words to include **and** words to skip at the same time (skipping wins);
- require any or all of the include words;
- look in titles, descriptions or both;
- match whole words only, so "war" does not match "award";
- set a minimum and a maximum length.

A live preview shows which of the last 50 episodes match, and why. Both filters are kept, so you can switch between Basic and Advanced at any time. The advanced filter starts from the podcast's basic words. The filter decides what downloads automatically, and playlists only pick episodes that pass it, in both modes.

### Playlists with only downloaded episodes
In a playlist's menu (⋮), **Only downloaded episodes** makes its rules pick only from episodes that are already downloaded. Combined with a podcast's episode filter (which decides what gets downloaded), this gives you a playlist of exactly the episodes you want.

### Skip ad button
Many podcasts play ads of about the same length. In a podcast's settings, set an "Ad skip time" (for example 30 seconds). The player then shows a "Skip ad" button for that podcast that jumps ahead by that amount, separate from the regular fast-forward button.

### A modern player
The full-screen player has a new look: smaller artwork, a bold episode title, and large, well-spaced controls (rewind, play, fast-forward) with speed, sleep timer, next episode and shownotes in a labeled row below. Prefer the original? Switch back any time in Settings → User interface → Player style → Classic.

### Made for foldables and big screens
On a wide screen (an unfolded Pixel Fold or Galaxy Z Fold, a tablet, or a phone in landscape), the app uses the extra room. Everything rearranges itself as you fold and unfold, and you stay where you were.

- **Player:** with the modern player, the show notes sit next to the artwork and controls instead of behind the Shownotes button.
- **Tabletop mode:** half-fold the phone on a table (held sideways) and the player puts the artwork on the top half and big controls on the bottom half.
- **Playlists:** your playlists on the left, the selected playlist's episodes on the right. Tap the pencil to edit: the rules move to the left and a live preview on the right updates as you change them (newly added episodes are briefly highlighted). On a narrow screen, the rules screen shows how many episodes the playlist currently has.
- **Episode details beside the list:** tapping an episode anywhere (Inbox, Queue, Episodes, Downloads, Home, a podcast's page) opens its details on the right while the list stays on the left. Close them with ✕ or Back. Fold the phone and the episode goes full screen.

### Settings added by this fork
| Setting | Where | Options |
|---|---|---|
| At the end of a playlist | Settings → Playback | Continue with the next playlist (default) · Stop |
| Player style | Settings → User interface | Modern (default) · Classic |
| Ad skip time | A podcast's settings | Number of seconds, 0 to hide the button |
| Episode filter style | Settings → Downloads → Automatic download | Basic (default) · Advanced |
| Auto-download episodes | A playlist's menu (⋮) | On · Off |
| Only downloaded episodes | A playlist's menu (⋮) | On · Off (default) |
| Show in Android Auto | A playlist's menu (⋮) | On (default for new playlists) · Off |

Tip: if auto-downloaded episodes from your podcasts land in the queue and you would rather they didn't, turn off Settings → Playback → **Enqueue downloaded** (a standard AntennaPod setting).

### Installs next to the official app
The fork uses its own package name (`io.github.dcdude71.antennapodmix`) and shows up as "AntennaPod Mix", with a purple icon and a "MIX" label (visible with themed icons too), so it can be installed alongside the official AntennaPod and told apart at a glance. The two apps do not share data. To try the fork with your podcasts, use "Database export" in AntennaPod (Settings → Backup & restore), then "Database import" in the fork.

### Backups and switching back to AntennaPod
In Settings → Backup & restore:

- **Database export** is your regular backup. It includes your playlists, skip-ad times and advanced episode filters, and restores everything in AntennaPod Mix.
- **Export for official AntennaPod** makes the same backup without the fork's additions (playlists, playlist rules, skip-ad times, advanced episode filters), for switching back to the official app. Import it there with "Database import"; your subscriptions, played episodes, queue and settings carry over.

One caveat: AntennaPod Mix follows AntennaPod's development version. If AntennaPod changes its database format there, Mix can get ahead of the official release for a while, and the official app will refuse to import a backup from a newer version until it is updated too.

## Installing

Signed APKs are published on the [Releases page](https://github.com/dcdude71/AntennaPod/releases). The easiest way to install and stay up to date is [Obtainium](https://github.com/ImranR98/Obtainium): add `https://github.com/dcdude71/AntennaPod` as an app source.

The release APK has been scanned by Google Play Protect, which found no issues. Because it does not come from the Play Store, Android asks you to allow installs from the app you install it with (Obtainium or your browser).

## Please read: what to expect

This is mainly a personal project, maintained in my spare time and shaped by how I like to listen to podcasts. A few honest notes:

- Features may change, or disappear, as I experiment.
- Comments, issues and suggestions are very welcome here, but I may not get to them quickly, and some may never be addressed.
- Please don't report problems with this fork to the AntennaPod team. Their issue tracker is for the official app.
- The fork is provided as is, without any warranty. Use it at your own risk, and keep backups of your data.

If you need something dependable, the official AntennaPod is the way to go. Thank you for understanding!

## Privacy

AntennaPod Mix collects no data and sends nothing to its developer. Like AntennaPod, it only connects to the podcast feeds you subscribe to and to services you choose to use (such as podcast search or synchronization). The fork's added features (playlists, widgets and shortcuts, Android Auto, Skip ad, the advanced episode filter, the foldable layouts) work entirely on your device. For the official app's policy, see [antennapod.org/privacy](https://antennapod.org/privacy/).

## Credits

All the hard work behind this app comes from the [AntennaPod](https://github.com/AntennaPod/AntennaPod) team, its contributors and translators. This fork only adds a few things on top. If you enjoy the app, consider supporting the original project at [antennapod.org](https://antennapod.org/).

## License

Like AntennaPod, this fork is licensed under the GNU General Public License (GPL-3.0). You can find the license text in the [LICENSE](LICENSE) file. The source code of every release is available in this repository.

## Building

You can build the app like any other Android project, for example with `./gradlew :app:assembleDebug`. See [CONTRIBUTING.md](CONTRIBUTING.md) for more details on the project structure (written for upstream AntennaPod).
