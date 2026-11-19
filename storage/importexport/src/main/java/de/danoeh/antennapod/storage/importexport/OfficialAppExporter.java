package de.danoeh.antennapod.storage.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.danoeh.antennapod.storage.database.EpisodeFilterDbAdapter;
import de.danoeh.antennapod.storage.database.FeedAdSkipDbAdapter;
import de.danoeh.antennapod.storage.database.PlaylistDbAdapter;

public final class OfficialAppExporter {
    private static final String TEMP_FILE_NAME = "official-app-export.db";
    private static final String[] FORK_TABLES = {
        PlaylistDbAdapter.TABLE_NAME_PLAYLIST_RULES,
        PlaylistDbAdapter.TABLE_NAME_PLAYLISTS,
        PlaylistDbAdapter.TABLE_NAME_PLAYLIST_DISMISSED,
        FeedAdSkipDbAdapter.TABLE_NAME_FEED_AD_SKIP,
        EpisodeFilterDbAdapter.TABLE_NAME_FEED_EPISODE_FILTERS,
        EpisodeFilterDbAdapter.TABLE_NAME_EPISODE_FILTER_CONDITIONS
    };

    private OfficialAppExporter() {
    }

    public static void exportToDocument(@NonNull Uri uri, @NonNull Context context) throws IOException {
        File tempDb = new File(context.getCacheDir(), TEMP_FILE_NAME);
        try {
            try (FileOutputStream out = new FileOutputStream(tempDb)) {
                DatabaseExporter.exportToStream(out, context);
            }
            removeForkTables(tempDb);
            try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "wt");
                    FileOutputStream out = new FileOutputStream(pfd.getFileDescriptor())) {
                FileUtils.copyFile(tempDb, out);
            }
        } finally {
            deleteWithSidecars(tempDb);
        }
    }

    static void removeForkTables(@NonNull File databaseFile) {
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(databaseFile.getAbsolutePath(), null,
                SQLiteDatabase.OPEN_READWRITE)) {
            for (String table : FORK_TABLES) {
                db.execSQL("DROP TABLE IF EXISTS " + table);
            }
            db.execSQL("VACUUM");
        }
    }

    private static void deleteWithSidecars(@NonNull File databaseFile) {
        FileUtils.deleteQuietly(databaseFile);
        for (String suffix : new String[]{"-wal", "-shm", "-journal"}) {
            FileUtils.deleteQuietly(new File(databaseFile.getAbsolutePath() + suffix));
        }
    }
}
