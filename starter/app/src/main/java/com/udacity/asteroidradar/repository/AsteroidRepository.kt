package com.udacity.asteroidradar.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.udacity.asteroidradar.Asteroid
import com.udacity.asteroidradar.Constants
import com.udacity.asteroidradar.api.*
import com.udacity.asteroidradar.database.AsteroidDatabase
import com.udacity.asteroidradar.database.asDatabaseModel
import com.udacity.asteroidradar.database.asDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class AsteroidRepository(private val database: AsteroidDatabase) {

    val asteroid: LiveData<List<Asteroid>> =
        Transformations.map(database.asteroidDao.getAsteroid()) {
            it.asDomainModel()
        }

    val pictureOfDay =  Transformations.map(database.pictureDao.getPictureUrl()) {
        it.asDomainModel().url
    }

    suspend fun refreshAsteroids() {
        withContext(Dispatchers.IO) {
            try {
                val restultNetwork = Network.retrofitService.getProperties(
                    startDate = getStartDateFormatted(),
                    endDate = getEndDateFormatted()
                ).await()

                val resultJSONObject = JSONObject(restultNetwork)
                val resultParsed = parseAsteroidsJsonResult(resultJSONObject)

                // Clear the database to remove old dates before insert new ones
                database.asteroidDao.clear()
                database.asteroidDao.insertAll(*resultParsed.asDatabaseModel())
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Error refresh asteroid ", e.message!!)
                }
                e.printStackTrace()
            }
        }
    }

    suspend fun refreshPicture() {
        withContext(Dispatchers.IO) {
            try {
                val imageofDay = Network.retrofitService.getImageOfTheDay().await()
                // Clear the database to remove last picture
                if (imageofDay.mediaType == "image") {
                    database.pictureDao.clear()
                    database.pictureDao.insertAll(imageofDay.asDatabaseModel())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Error refresh picture ", e.message!!)
                }
                e.printStackTrace()
            }
        }
    }

    fun getStartDateFormatted(): String {
        val calendar = Calendar.getInstance()
        val currentTime = calendar.time
        val dateFormat = SimpleDateFormat(Constants.API_QUERY_DATE_FORMAT, Locale.getDefault())
        return dateFormat.format(currentTime)
    }

    fun getEndDateFormatted(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val currentTime = calendar.time
        val dateFormat = SimpleDateFormat(Constants.API_QUERY_DATE_FORMAT, Locale.getDefault())
        return dateFormat.format(currentTime)
    }
}