package de.danoeh.antennapod.storage.importexport;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class OfficialAppExporterTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testRemovesOnlyForkTables() throws Exception {
        File file = folder.newFile("backup.db");
        file.delete();
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            db.execSQL("CREATE TABLE Feeds (id INTEGER PRIMARY KEY, title TEXT)");
            db.execSQL("INSERT INTO Feeds (title) VALUES ('News')");
            db.execSQL("CREATE TABLE Playlists (id INTEGER PRIMARY KEY, name TEXT)");
            db.execSQL("INSERT INTO Playlists (name) VALUES ('Morning')");
            db.execSQL("CREATE TABLE PlaylistRules (id INTEGER PRIMARY KEY, playlist INTEGER)");
            db.execSQL("CREATE INDEX PlaylistRules_playlist ON PlaylistRules (playlist)");
            db.execSQL("CREATE TABLE FeedAdSkip (feed INTEGER PRIMARY KEY, seconds INTEGER)");
            db.execSQL("CREATE TABLE FeedEpisodeFilters (feed INTEGER PRIMARY KEY, include_terms TEXT)");
            db.setVersion(3110000);
        }

        OfficialAppExporter.removeForkTables(file);

        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null,
                SQLiteDatabase.OPEN_READONLY)) {
            Set<String> tables = new HashSet<>();
            try (Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master", null)) {
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0));
                }
            }
            assertTrue(tables.contains("Feeds"));
            assertFalse(tables.contains("Playlists"));
            assertFalse(tables.contains("PlaylistRules"));
            assertFalse(tables.contains("PlaylistRules_playlist"));
            assertFalse(tables.contains("FeedAdSkip"));
            assertFalse(tables.contains("FeedEpisodeFilters"));
            assertEquals(3110000, db.getVersion());
            try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM Feeds", null)) {
                cursor.moveToFirst();
                assertEquals(1, cursor.getInt(0));
            }
        }
    }
}
