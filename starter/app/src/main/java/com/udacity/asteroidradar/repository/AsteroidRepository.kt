package com.udacity.asteroidradar.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.udacity.asteroidradar.Asteroid
import com.udacity.asteroidradar.api.*
import com.udacity.asteroidradar.database.AsteroidDatabase
import com.udacity.asteroidradar.database.DatabaseAsteroid
import com.udacity.asteroidradar.database.asDatabaseModel
import com.udacity.asteroidradar.database.asDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.Exception

class AsteroidRepository(private val database: AsteroidDatabase) {

    val asteroid: LiveData<List<Asteroid>> =
        Transformations.map(database.asteroidDao.getAsteroid()) {
            it.asDomainModel()
        }

    // Responsible for updating the offline cache
    suspend fun refreshAsteroids() {
        withContext(Dispatchers.IO) {
            try {
                val restult = parseAsteroidsJsonResult(
                    JSONObject(
                        Network.retrofitService.getProperties().await()
                    )
                )
                database.asteroidDao.insertAll(*restult.asDatabaseModel())
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {

                }
                e.printStackTrace()
            }
        }
    }
}