package com.udacity.asteroidradar.main

import android.app.Application
import androidx.lifecycle.*
import com.udacity.asteroidradar.Asteroid
import com.udacity.asteroidradar.database.getDatabase
import com.udacity.asteroidradar.repository.AsteroidRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : ViewModel() {

    private val _navigateToSelectedAsteroid = MutableLiveData<Asteroid>()
    val navigateToSelectedAsteroid: LiveData<Asteroid>
        get() = _navigateToSelectedAsteroid

    private val database = getDatabase(application)
    private val asteroidsRepository = AsteroidRepository(database)

    init {
        refreshAsteroid()
    }

    val asteroid = asteroidsRepository.asteroid
    val picOfDay = asteroidsRepository.pictureOfDay
    val loadingStatus = asteroidsRepository.status

    private fun refreshAsteroid() {
        viewModelScope.launch {
            asteroidsRepository.refreshAsteroids()
        }
    }

    fun displayAsteroidDetails(asteroid: Asteroid) {
        _navigateToSelectedAsteroid.value = asteroid
    }

    fun displayAsteroidComplete() {
        _navigateToSelectedAsteroid.value = null
    }

    fun onFilterSelect(filter: AsteroidRepository.Query) {
        asteroidsRepository.applyFilter(filter)
    }

    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}