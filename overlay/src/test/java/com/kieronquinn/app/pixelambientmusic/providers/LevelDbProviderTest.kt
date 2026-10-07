package com.kieronquinn.app.pixelambientmusic.providers

import android.app.Application
import android.net.Uri
import com.google.audio.ambientmusic.ShardTracks
import com.kieronquinn.app.pixelambientmusic.config.DeviceConfigOverrides
import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.Options
import org.iq80.leveldb.table.BytewiseComparator
import org.iq80.leveldb.table.TableBuilder
import org.iq80.leveldb.util.Slices
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class LevelDbProviderTest {
    private lateinit var folder: File
    private lateinit var provider: LevelDbProvider
    @Before fun setup() {
        val context = RuntimeEnvironment.getApplication<Application>()
        File(context.filesDir, "pixel_ambient_music").mkdirs()
        folder = File(context.filesDir, "superpacks/ambientmusic-index-17_09_02").apply { mkdirs() }
        DeviceConfigOverrides::class.java.getDeclaredField("FLAG_VALUES").apply { isAccessible = true }
            .set(null, mapOf("NowPlaying__device_country" to "jp",
                "NowPlaying__ambient_music_extra_languages" to "ar,us"))
        provider = Robolectric.buildContentProvider(LevelDbProvider::class.java).create().get()
    }
    private fun shard(name: String, vararg titles: String): File {
        return File(folder, name).also { file ->
            file.outputStream().use { stream ->
                val builder = TableBuilder(Options().compressionType(CompressionType.NONE),
                    stream.channel, BytewiseComparator())
                titles.forEachIndexed { index, title ->
                    val track = ShardTracks.Track.newBuilder().setDbId("db000000001")
                        .setTrackName(title).setArtist("Test artist").build()
                    builder.add(Slices.wrappedBuffer("%08d".format(index).toByteArray()),
                        Slices.wrappedBuffer(track.toByteArray()))
                }
                builder.finish()
            }
        }
    }
    private fun query(path: String): Int = provider.query(Uri.parse(
        "content://com.google.android.as.pam.ambientmusic.leveldbprovider/$path"),
        null, null, null, null)!!.use { cursor -> cursor.moveToFirst(); cursor.getInt(0) }

    @Test fun deduplicatesAcrossCountriesAndSkipsCorruptShards() {
        shard("JP0", "First", "Shared")
        shard("US0", "Shared", "Last")
        File(folder, "AR0").writeBytes(byteArrayOf(0, 1, 2))
        assertEquals(3, query("count/leveldb"))
        assertEquals(3, query("count/leveldb"))
    }
    @Test fun sameFilenameReplacementInvalidatesCachedCount() {
        val file = shard("JP0", "First")
        assertEquals(1, query("count/leveldb"))
        val before = query("hash")
        shard("JP0", "First", "Second")
        file.setLastModified(file.lastModified() + 10_000)
        assertNotEquals(before, query("hash"))
        assertEquals(2, query("count/leveldb"))
    }
    @Test fun emptyDatabaseReturnsZero() { assertEquals(0, query("count/leveldb")) }
}
