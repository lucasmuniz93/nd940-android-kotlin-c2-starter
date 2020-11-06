package com.udacity.asteroidradar.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import com.udacity.asteroidradar.Constants.DATABASE_VERSION

/*
*  Create a API between network result and database
*/
@Dao
interface AsteroidDao {
    @Query("SELECT * FROM databaseasteroid ORDER BY closeApproachDate DESC")
    fun getAsteroid(): LiveData<List<DatabaseAsteroid>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg asteroid: DatabaseAsteroid)

    @Query("DELETE FROM databaseasteroid")
    fun clear()
}

@Dao
interface PictureDao {
    @Query("SELECT * FROM databasepicture")
    fun getPictureUrl(): LiveData<DatabasePicture>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg picture: DatabasePicture)

    @Query("DELETE FROM databasepicture")
    fun clear()
}

@Database(entities = [DatabaseAsteroid::class,DatabasePicture::class], version = DATABASE_VERSION)
abstract class AsteroidDatabase : RoomDatabase() {
    abstract val asteroidDao: AsteroidDao

    abstract val pictureDao: PictureDao
}

// Returns a singleton instance of database
private lateinit var INSTANCE: AsteroidDatabase

fun getDatabase(context: Context): AsteroidDatabase {
    synchronized(AsteroidDatabase::class.java) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE = Room.databaseBuilder(
                context.applicationContext,
                AsteroidDatabase::class.java,
                "asteroids"
            ).build()
        }
    }
    return INSTANCE
}