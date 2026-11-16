# Fork notes

Handoff notes for working on this fork (on any machine, with or without Claude Code). Upstream `AGENTS.md` still applies.

## Repo setup
- Fork: `stephanemoulec/AntennaPod` (`origin`). Upstream: `AntennaPod/AntennaPod` (add it as `upstream`).
- All fork work is on `develop`, rebased on upstream `develop`. The old `fork-setup` and `playlists` branches are gone; their commits are on `develop`.
- Application id: `com.stephane.antennapod`, so it installs next to the official app.
- Releases: push a `v*` tag and `.github/workflows/fork-release.yml` builds and signs the APK. The keystore and passwords are GitHub secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), so nothing needs to be copied between machines.
- `local.properties` and `.idea/` are machine-specific and not committed.

## Keep the diff against upstream small
The fork is rebased on upstream regularly, so:
- Put new code in new, fork-only classes and files. Only add small hooks to upstream files.
- Never bump the database version. Fork tables are created with `CREATE TABLE IF NOT EXISTS` from `PodDBAdapter`'s helper `onOpen` (`PlaylistDbAdapter.createTables`, `FeedAdSkipDbAdapter.createTables`).
- `git diff 1719c6d37~1 HEAD --stat` lists everything the fork touches.

## Rule-based playlists
Approved design (open to adjustment once used in practice):
- Playlists exist alongside the untouched queue. No master playlist, no manual "add episode to playlist", no per-podcast default playlist.
- Playlist = name + rules + auto-download + show in Android Auto.
- Rule = podcast, count (0 = all), pick from most recent / oldest, play order oldest / newest first, priority 1-10. Same priority: rule order (drag to reorder).
- "Most recent N" = the N newest episodes, only the unplayed ones among them (does not walk into the back catalog). "Oldest N" = the next N unplayed.
- The playlist is recalculated live after each episode. Playing from a playlist never adds to the queue; starting any other episode goes back to the normal queue.
- Playlist mode starts from the toolbar "Play playlist" button and from the play button of any episode row in the playlist screen (hook: `EpisodesListFragment.onEpisodeBound`). Playing from the episode details page does not start playlist mode.
- At the end of a playlist: setting `prefPlaylistEndAction` (Settings → Playback), "Continue with the next playlist" (default, follows Playlists tab order, skips empty ones, stops after the last) or "Stop". Never falls back to the queue. Read via `PlaylistPreferences`.
- Playlist auto-downloads follow the normal download settings, never enqueue, and playlist episodes are protected from download cleanup.
- Playlist downloads do not count toward the global episode limit (`AutomaticDownloadAlgorithm`, `EpisodeCleanupAlgorithm.getNumEpisodesToCleanup`). To keep them bounded, rules in an auto-download playlist need a count of 1-10 (`PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES`); the rule dialog disables "All" and the auto-download toggle refuses while an "All" rule exists. `PlaylistReader.limitForAutoDownload` caps legacy rules anyway. The same cap applies to cleanup protection in every playlist: only the first 10 episodes per rule are protected and left out of the limit, so a playlist with an "All" rule cannot keep unlimited downloads.
- Fixed (also an upstream bug): when downloaded episodes exceeded the episode limit, `episodeSpaceLeft` went negative and `subList` threw inside an executor, silently skipping all auto-downloads. Now clamped to 0.
- Upstream clears an episode's auto-download flag after its first successful download (`MediaDownloadedHandler`) or a cancelled download, so a deleted episode is never auto-downloaded again. This applies to playlist downloads too.
- Code: `model/.../playlist/`, `storage/database/.../Playlist*` (DbAdapter, Engine, Reader, Writer, Playback), `app/.../ui/screen/playlist/`. Tests in `storage/database/src/test/.../Playlist*Test.java`.

Phases:
1. Data model and engine: done
2. Screens and rule editor: done
3. Playback: done
4. Auto-download and cleanup protection: done
5. Android Auto: built, not yet tested in a car or the Desktop Head Unit. A "Playlists" folder appears after "Continue listening" when at least one playlist has "Show in Android Auto" ticked (playlist screen ⋮ menu; on by default for new playlists, off for existing ones). Starting an episode from it starts playlist playback. Code: `PlaylistMediaLibrary` plus small hooks in `MediaLibrarySessionCallback`.

Inspired by Downcast's playlist engine.

## Testing on a device
- Debug build: `./gradlew --console=plain :app:installPlayDebug`, then `adb shell monkey -p com.stephane.antennapod.debug -c android.intent.category.LAUNCHER 1` (AGENTS.md's launch command uses the upstream package name and does not work here).
- The debug app is separate from the release app. Import a database export to test with real podcasts. Downloaded files from the release app are not readable by the debug app, so reset `FeedMedia.downloaded`/`file_url` in the imported copy (`adb exec-out run-as com.stephane.antennapod.debug cat databases/Antennapod.db`, edit with sqlite3, push back with `run-as ... sh -c 'cat > databases/Antennapod.db'` while the app is force-stopped).

## Skip ad
Per-podcast "Ad skip time" in the podcast's settings (fork table via `FeedAdSkip` / `FeedAdSkipDbAdapter`). When set, the player shows a "Skip ad" button that jumps ahead by that amount from the current position, separate from fast-forward.

## Player style
Settings → User interface → Player style: Modern (default) or Classic (upstream layout). Modern aims for an Apple Podcasts-like look: small artwork, bold title, big controls. Fork-only files: `PlayerStyle.java`, `audioplayer_fragment_modern.xml`.

## Working preferences
- Ask design questions as a short numbered list with a recommendation per item, and let the answer come back as plain text (not a multiple-choice picker); answers often add nuance the options don't cover.
