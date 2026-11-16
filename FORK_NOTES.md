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
- The playlist is recalculated live after each episode. Playback stops at the end of the playlist. Playing from a playlist never adds to the queue; starting any other episode goes back to the normal queue.
- Playlist auto-downloads follow the normal download settings, never enqueue, and playlist episodes are protected from download cleanup.
- Code: `model/.../playlist/`, `storage/database/.../Playlist*` (DbAdapter, Engine, Reader, Writer, Playback), `app/.../ui/screen/playlist/`. Tests in `storage/database/src/test/.../Playlist*Test.java`.

Phases:
1. Data model and engine: done
2. Screens and rule editor: done
3. Playback: done
4. Auto-download and cleanup protection: done
5. Android Auto: not started (test with the Desktop Head Unit). README calls it "an idea for later".

Inspired by Downcast's playlist engine.

## Skip ad
Per-podcast "Ad skip time" in the podcast's settings (fork table via `FeedAdSkip` / `FeedAdSkipDbAdapter`). When set, the player shows a "Skip ad" button that jumps ahead by that amount from the current position, separate from fast-forward.

## Player style
Settings → User interface → Player style: Modern (default) or Classic (upstream layout). Modern aims for an Apple Podcasts-like look: small artwork, bold title, big controls. Fork-only files: `PlayerStyle.java`, `audioplayer_fragment_modern.xml`.

## Working preferences
- Ask design questions as a short numbered list with a recommendation per item, and let the answer come back as plain text (not a multiple-choice picker); answers often add nuance the options don't cover.
