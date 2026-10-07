package com.kieronquinn.app.pixelambientmusic.components.settings

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class DownloadStateTest {
    @Test fun completedAndUnrelatedDownloadsAreNotPendingMusic() {
        val context = RuntimeEnvironment.getApplication<Application>()
        val path = context.getDatabasePath("superpacks.db")
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { database ->
            database.execSQL("CREATE TABLE pending_downloads (superpack_name TEXT, completed INTEGER)")
            database.execSQL("INSERT INTO pending_downloads VALUES ('ambientmusic-index-17_09_02', 1)")
            database.execSQL("INSERT INTO pending_downloads VALUES ('spelling_correction', 0)")
            assertEquals(0, SettingsStateHandler.getSuperpackDownloadCount(context))
            database.execSQL("INSERT INTO pending_downloads VALUES ('ambientmusic-index-17_09_02', 0)")
            assertEquals(1, SettingsStateHandler.getSuperpackDownloadCount(context))
        }
    }
    @Test fun missingDatabaseReturnsZero() {
        val context = RuntimeEnvironment.getApplication<Application>()
        context.deleteDatabase("superpacks.db")
        assertEquals(0, SettingsStateHandler.getSuperpackDownloadCount(context))
    }
}
