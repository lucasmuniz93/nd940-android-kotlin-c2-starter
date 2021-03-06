package com.udacity.asteroidradar.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.udacity.asteroidradar.Asteroid
import com.udacity.asteroidradar.Constants
import com.udacity.asteroidradar.PictureOfDay
import com.udacity.asteroidradar.api.*
import com.udacity.asteroidradar.database.AsteroidDatabase
import com.udacity.asteroidradar.database.DatabasePicture
import com.udacity.asteroidradar.database.asDatabaseModel
import com.udacity.asteroidradar.database.asDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*

class AsteroidRepository(private val database: AsteroidDatabase) {

    enum class Query() { SAVED, TODAY, WEEK }

    private val _queryType: MutableLiveData<Query> = MutableLiveData(Query.WEEK)
    val queryType: LiveData<Query>
        get() = _queryType

    // Apply the filter based on queryType
    val asteroid: LiveData<List<Asteroid>> =
        Transformations.switchMap(queryType) {
            when (it) {
                Query.SAVED -> Transformations.map(database.asteroidDao.getAsteroid()) {
                    it.asDomainModel()
                }
                Query.TODAY -> Transformations.map(database.asteroidDao.getTodayAsteroids()) {
                    it.asDomainModel()
                }
                Query.WEEK -> Transformations.map(database.asteroidDao.getWeekAsteroids()) {
                    it.asDomainModel()
                }
                else -> throw IllegalArgumentException("incompatible query type")
            }
        }

    private val _status: MutableLiveData<Boolean> = MutableLiveData(false)
    val status: LiveData<Boolean>
        get() = _status

    val pictureOfDay: LiveData<PictureOfDay> = Transformations.map(getPicture()) {
        it?.asDomainModel()
    }

    private fun getPicture(): LiveData<DatabasePicture> {
        return database.pictureDao.getPicture()
    }

    // Get the asteroids and imagem from the server
    suspend fun refreshAsteroids() {
        _status.postValue(true)
        withContext(Dispatchers.IO) {
            try {
                val restultNetwork = Network.retrofitService.getProperties(
                    startDate = getStartDateFormatted(),
                    endDate = getEndDateFormatted()
                ).await()
                // Convert the result into an arraylist of Asteroid
                val resultJSONObject = JSONObject(restultNetwork)
                val resultParsed = parseAsteroidsJsonResult(resultJSONObject)

                val imageofDay = Network.retrofitService.getImageOfTheDay().await()
                // Clear the database to remove last picture
                if (imageofDay.mediaType == "image") {
                    database.pictureDao.clear()
                    database.pictureDao.insertAll(imageofDay.asDatabaseModel())
                }
                _status.postValue(false)
                database.asteroidDao.insertAll(*resultParsed.asDatabaseModel())
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Error refresh asteroid ", e.message!!)
                    _status.postValue(false)
                }
                e.printStackTrace()
            }
        }
    }

    suspend fun removeOldDates(){
        withContext(Dispatchers.IO){
            database.asteroidDao.removeOldDates()
        }
    }
    // Format the date
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

    fun applyFilter(filter: Query) {
        _queryType.value = filter
    }
}