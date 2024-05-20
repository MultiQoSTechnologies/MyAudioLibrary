package com.example.myaudiolibrary.core.db

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.room.*
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import com.example.myaudiolibrary.core.db.Playlist.Member

private const val TABLE_PLAYLIST_MEMBER = "tbl_playlist_members"

private const val PLAYLIST_COLUMN_ID = "playlist_id"
private const val MEMBER_COLUMN_ORDER = "play_order"
private const val MEMBER_FILE_ID = "media_id"
private const val MEMBER_FILE_URI = "uri"

@Stable
@Entity(tableName = "tbl_playlists")
data class Playlist(
    @JvmField val name: String,
    @JvmField @PrimaryKey(autoGenerate = true) @ColumnInfo(name = PLAYLIST_COLUMN_ID) val id: Long = 0,
    @ColumnInfo(defaultValue = "") val desc: String = "",
    @JvmField @ColumnInfo(name = "date_created") val dateCreated: Long = System.currentTimeMillis(),
    @JvmField @ColumnInfo(name = "date_modified") val dateModified: Long = System.currentTimeMillis(),
) {
    @Entity(
        tableName = TABLE_PLAYLIST_MEMBER,
        primaryKeys = [PLAYLIST_COLUMN_ID, MEMBER_FILE_URI],
        foreignKeys = [ForeignKey(
            entity = Playlist::class,
            parentColumns = [PLAYLIST_COLUMN_ID],
            childColumns = [PLAYLIST_COLUMN_ID],
            onDelete = CASCADE
        )],
        indices = [Index(value = [PLAYLIST_COLUMN_ID, MEMBER_FILE_URI], unique = false)]
    )
    @Stable
    data class Member(
        @JvmField @ColumnInfo(name = PLAYLIST_COLUMN_ID) val playlistID: Long,
        @JvmField @ColumnInfo(name = MEMBER_FILE_ID) val id: String,
        @JvmField @ColumnInfo(name = MEMBER_COLUMN_ORDER) val order: Int,
        @JvmField @ColumnInfo(name = MEMBER_FILE_URI) val uri: String,
        @JvmField val title: String,
        @JvmField val subtitle: String,
        @JvmField @ColumnInfo(name = "artwork_uri") val artwork: String? = null,
    )
}


@Database(entities = [Playlist::class, Member::class], version = 3, exportSchema = false)
abstract class Realm : RoomDatabase() {

    abstract val playlists: Playlists

    companion object {
        private const val DB_NAME = "realm_db"
        private const val TRIGGER = "trigger"

        //language=SQL
        private const val TRIGGER_BEFORE_INSERT =
            "CREATE TRIGGER IF NOT EXISTS ${TRIGGER}_reorder_insert BEFORE INSERT ON $TABLE_PLAYLIST_MEMBER " + "BEGIN UPDATE $TABLE_PLAYLIST_MEMBER SET $MEMBER_COLUMN_ORDER = $MEMBER_COLUMN_ORDER + 1 " + "WHERE new.$PLAYLIST_COLUMN_ID == $PLAYLIST_COLUMN_ID AND $MEMBER_COLUMN_ORDER >= new.$MEMBER_COLUMN_ORDER;" + "END;"

        //language=SQL
        private const val TRIGGER_AFTER_DELETE =
            "CREATE TRIGGER IF NOT EXISTS ${TRIGGER}_reorder_delete AFTER DELETE ON $TABLE_PLAYLIST_MEMBER " + "BEGIN UPDATE $TABLE_PLAYLIST_MEMBER SET $MEMBER_COLUMN_ORDER = $MEMBER_COLUMN_ORDER - 1 " + "WHERE old.$PLAYLIST_COLUMN_ID == $PLAYLIST_COLUMN_ID AND old.$MEMBER_COLUMN_ORDER < $MEMBER_COLUMN_ORDER;" + "END;"

        private val CALLBACK = object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL(TRIGGER_BEFORE_INSERT)
                db.execSQL(TRIGGER_AFTER_DELETE)
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(TRIGGER_BEFORE_INSERT)
                db.execSQL(TRIGGER_AFTER_DELETE)
            }
        }

        @Volatile
        private var INSTANCE: Realm? = null

        fun get(context: Context): Realm {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, Realm::class.java, DB_NAME
                ).addCallback(CALLBACK)
                    .allowMainThreadQueries().fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Dao
interface Playlists {
    companion object {
        operator fun invoke(context: Context) = Realm.get(context).playlists
        const val PRIVATE_PLAYLIST_PREFIX = '_'
    }

    // playlists
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(playlist: Playlist): Long

    @Query("SELECT MAX(play_order) FROM tbl_playlist_members WHERE playlist_id = :playlistId")
    suspend fun lastPlayOrder(playlistId: Long): Int?

    @Update
    suspend fun update(playlist: Playlist): Int

    @Delete
    suspend fun delete(playlist: Playlist): Int

    @Delete
    suspend fun delete(playlists: List<Playlist>): Int

    @Query("SELECT * FROM tbl_playlists WHERE playlist_id == :id")
    suspend fun get(id: Long): Playlist?

    @Query("SELECT * FROM tbl_playlists WHERE name == :name")
    suspend fun get(name: String): Playlist?

    @Query("SELECT * FROM tbl_playlists WHERE :query IS NULL OR name LIKE '%' || :query || '%'")
    fun observe(query: String? = null): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: Member): Long

    // members
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(members: List<Member>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(member: Member): Long

    @Query("SELECT * FROM tbl_playlist_members WHERE playlist_id == :playlistId AND uri == :uri")
    suspend fun get(playlistId: Long, uri: String): Member?

    @Query("SELECT EXISTS(SELECT 1 FROM tbl_playlist_members WHERE playlist_id == :playlistId AND uri == :uri)")
    suspend fun exists(playlistId: Long, uri: String): Boolean

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM tbl_playlist_members WHERE playlist_id = :playlistId ORDER BY play_order ASC")
    fun observe2(playlistId: Long): Flow<List<Member>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM tbl_playlist_members LEFT JOIN tbl_playlists ON tbl_playlist_members.playlist_id == tbl_playlists.playlist_id WHERE tbl_playlists.name == :name ORDER BY tbl_playlist_members.play_order ASC")
    fun observe2(name: String): Flow<List<Member>>

    @Query("SELECT * FROM tbl_playlist_members WHERE playlist_id = :id ORDER BY play_order ASC")
    suspend fun getMembers(id: Long): List<Member>

    suspend fun getMembers(name: String): List<Member> {
        val x = get(name) ?: return emptyList()
        return getMembers(x.id)
    }
}