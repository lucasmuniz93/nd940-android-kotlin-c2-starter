package com.udacity.asteroidradar.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udacity.asteroidradar.Asteroid
import com.udacity.asteroidradar.api.AsteroidApi
import com.udacity.asteroidradar.api.parseAsteroidsJsonResult
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.await
import java.lang.Exception

class MainViewModel : ViewModel() {

    private val _status = MutableLiveData<String>()
    val status: LiveData<String>
        get() = _status

    private val _imageOfDay = MutableLiveData<String>()
    val imageOfDay: LiveData<String>
        get() = _imageOfDay

    private val _asteroids = MutableLiveData<List<Asteroid>>()
    val asteroid: LiveData<List<Asteroid>>
        get() = _asteroids

    private val _navigateToSelectedAsteroid = MutableLiveData<Asteroid>()
    val navigateToSelectedAsteroid: LiveData<Asteroid>
        get() = _navigateToSelectedAsteroid

    init {
        getAsteroidsRealEstateProperties()
    }

    // Get and parse the JSON from API
    private fun getAsteroidsRealEstateProperties() {
        viewModelScope.launch {
            try {
                val restult = AsteroidApi.retrofitService.getProperties().await()
                _asteroids.value = parseAsteroidsJsonResult(JSONObject(restult))
            } catch (e: Exception) {
                _status.value = "Failure: ${e.message}"
            }
        }
    }

    private fun getImageOfTheDay() {

        viewModelScope.launch {

            AsteroidApi.retrofitService.getImageOfTheDay().enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    Log.i("Mylog ", "Success Image")
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Log.i("Mylog Failed Image:", t.message.toString())
                }

            })

        }
    }

    fun displayAsteroidDetails(asteroid: Asteroid) {
        _navigateToSelectedAsteroid.value = asteroid
    }

    fun displayAsteroidComplete() {
        _navigateToSelectedAsteroid.value = null
    }
}