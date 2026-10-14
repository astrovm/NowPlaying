package com.kieronquinn.app.pixelambientmusic.providers

import android.app.Application
import android.net.Uri
import com.google.audio.ambientmusic.Linear.Tracks
import com.google.audio.ambientmusic.ShardTracks
import com.kieronquinn.app.pixelambientmusic.config.DeviceConfigOverrides
import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.Options
import org.iq80.leveldb.table.BytewiseComparator
import org.iq80.leveldb.table.TableBuilder
import org.iq80.leveldb.util.Slices
import java.io.File
import java.nio.file.Files
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
        val context = RuntimeEnvironment.getApplication()
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

    private fun linearQuery(name: String, path: String = "linear") = provider.query(
        Uri.parse("content://com.google.android.as.pam.ambientmusic.leveldbprovider/$path/" +
            Uri.encode(name)), null, null, null, null)

    @Test fun readsBothSupportedLinearDatabases() {
        val cache = RuntimeEnvironment.getApplication().cacheDir
        listOf("linear.db", "linear_v3.db").forEach { name ->
            File(cache, name).writeBytes(Tracks.getDefaultInstance().toByteArray())
            linearQuery(name)!!.use { assertEquals(0, it.count) }
            assertEquals(0, query("count/$name"))
        }
    }

    @Test fun rejectsOtherCacheFilesAndEncodedTraversal() {
        val context = RuntimeEnvironment.getApplication()
        val bytes = Tracks.getDefaultInstance().toByteArray()
        File(context.cacheDir, "other.db").writeBytes(bytes)
        File(context.filesDir, "private.db").writeBytes(bytes)
        val names = listOf("other.db", "../files/private.db", "/linear.db", ".", "..",
            "linear.db/../other.db", "linear.db\\other.db", "linear.db\u0000") +
            (1..100).map { "../".repeat(it) + "files/private.db" }
        names.forEach { name ->
            assertNull(name, linearQuery(name))
            assertNull(name, linearQuery(name, "count"))
        }
    }

    @Test fun returnsNullForMissingCorruptAndNonFileDatabases() {
        val cache = RuntimeEnvironment.getApplication().cacheDir
        val file = File(cache, "linear.db")
        file.delete()
        assertNull(linearQuery("linear.db"))
        file.writeBytes(byteArrayOf(0x80.toByte()))
        assertNull(linearQuery("linear.db"))
        file.delete()
        file.mkdir()
        assertNull(linearQuery("linear.db"))
        file.delete()
    }

    @Test fun rejectsSymlinksOutsideCache() {
        val context = RuntimeEnvironment.getApplication()
        val target = File(context.filesDir, "private.db")
        target.writeBytes(Tracks.getDefaultInstance().toByteArray())
        val link = File(context.cacheDir, "linear.db").toPath()
        Files.deleteIfExists(link)
        Files.createSymbolicLink(link, target.toPath())
        assertNull(linearQuery("linear.db"))
    }
    private fun countries(values: Map<String, String>) {
        DeviceConfigOverrides::class.java.getDeclaredField("FLAG_VALUES").apply { isAccessible = true }
            .set(null, values)
    }

    @Test fun japanOnlyDoesNotLoadOtherCountries() {
        countries(mapOf("NowPlaying__device_country" to "jp"))
        shard("JP0", "Japan track")
        shard("US0", "Other track")
        assertEquals(1, query("count/leveldb"))
    }

    @Test fun missingCountryConfigurationDoesNotSelectEveryCountry() {
        countries(emptyMap())
        shard("JP0", "Japan track")
        shard("US0", "Other track")
        assertEquals(0, query("count/leveldb"))
    }

    @Test fun largeMetadataIsStreamedWithinTheDeviceHeapBudget() {
        val count = 20_000
        val player = ShardTracks.Track.Player.newBuilder()
            .setUrl("https://example.test/" + "x".repeat(512)).build()
        File(folder, "JP0").outputStream().use { stream ->
            val builder = TableBuilder(Options().compressionType(CompressionType.NONE),
                stream.channel, BytewiseComparator())
            repeat(count) { index ->
                val track = ShardTracks.Track.newBuilder().setDbId("db000000001")
                    .setTrackName("Synthetic $index").setArtist("Test artist")
                repeat(25) { track.addPlayer(player) }
                builder.add(Slices.wrappedBuffer("%08d".format(index).toByteArray()),
                    Slices.wrappedBuffer(track.build().toByteArray()))
            }
            builder.finish()
        }
        assertEquals(count, query("count/leveldb"))
    }

}
