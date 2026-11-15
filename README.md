# AntennaPod Fork

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub Release](https://img.shields.io/github/v/release/stephanemoulec/AntennaPod)](https://github.com/stephanemoulec/AntennaPod/releases)

This is a personal fork of [AntennaPod](https://github.com/AntennaPod/AntennaPod), the easy-to-use, flexible and open-source podcast manager for Android. I keep it close to upstream and add a few features I wanted for my own listening.

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

The playlist updates itself as new episodes come in and as you listen. Press play on a playlist and it plays through its episodes and stops at the end, without touching your queue. Starting any other episode takes you back to the normal queue.

Example: a "Morning" playlist with the 2 most recent episodes of a news podcast (priority 1), then the next unplayed episode of a series you are catching up on (priority 2).

Ideas for later include automatic downloads for playlists and showing playlists in Android Auto. No promises on timing.

### Installs next to the official app
The fork uses its own package name (`com.stephane.antennapod`) and shows up as "AntennaPod Fork", so it can be installed alongside the official AntennaPod. The two apps do not share data. To try the fork with your podcasts, use "Database export" in AntennaPod (Settings → Backup & restore), then "Database import" in the fork.

## Installing

Signed APKs are published on the [Releases page](https://github.com/stephanemoulec/AntennaPod/releases). The easiest way to install and stay up to date is [Obtainium](https://github.com/ImranR98/Obtainium): add `https://github.com/stephanemoulec/AntennaPod` as an app source.

## Please read: what to expect

This is mainly a personal project, maintained in my spare time and shaped by how I like to listen to podcasts. A few honest notes:

- Features may change, or disappear, as I experiment.
- Comments, issues and suggestions are very welcome here, but I may not get to them quickly, and some may never be addressed.
- Please don't report problems with this fork to the AntennaPod team. Their issue tracker is for the official app.
- The fork is provided as is, without any warranty. Use it at your own risk, and keep backups of your data.

If you need something dependable, the official AntennaPod is the way to go. Thank you for understanding!

## Credits

All the hard work behind this app comes from the [AntennaPod](https://github.com/AntennaPod/AntennaPod) team, its contributors and translators. This fork only adds a few things on top. If you enjoy the app, consider supporting the original project at [antennapod.org](https://antennapod.org/).

## License

Like AntennaPod, this fork is licensed under the GNU General Public License (GPL-3.0). You can find the license text in the [LICENSE](LICENSE) file. The source code of every release is available in this repository.

## Building

You can build the app like any other Android project, for example with `./gradlew :app:assembleDebug`. See [CONTRIBUTING.md](CONTRIBUTING.md) for more details on the project structure (written for upstream AntennaPod).
