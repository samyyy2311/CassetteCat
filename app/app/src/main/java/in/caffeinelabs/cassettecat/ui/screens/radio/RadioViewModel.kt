package `in`.caffeinelabs.cassettecat.ui.screens.radio

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.caffeinelabs.cassettecat.data.radio.RadioBrowserApiClient
import `in`.caffeinelabs.cassettecat.data.radio.RadioFavoritesRepository
import `in`.caffeinelabs.cassettecat.data.radio.RadioHistoryRepository
import `in`.caffeinelabs.cassettecat.data.radio.RadioSortOrder
import `in`.caffeinelabs.cassettecat.data.radio.RadioStation
import `in`.caffeinelabs.cassettecat.data.radio.customRadioStation
import `in`.caffeinelabs.cassettecat.data.settings.AppPreferencesRepository
import `in`.caffeinelabs.cassettecat.data.settings.ExternalService
import `in`.caffeinelabs.cassettecat.data.settings.ServiceSettingsRepository
import `in`.caffeinelabs.cassettecat.ui.screens.library.SortDirection
import `in`.caffeinelabs.cassettecat.ui.screens.library.flipped
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RadioViewModel(app: Application) : AndroidViewModel(app) {
    private val apiClient = RadioBrowserApiClient()
    private val favoritesRepository = RadioFavoritesRepository(app)
    private val historyRepository = RadioHistoryRepository(app)
    private val serviceSettingsRepository = ServiceSettingsRepository(app)
    private val appPreferencesRepository = AppPreferencesRepository(app)

    val favoriteStations: Flow<List<RadioStation>> = favoritesRepository.favoriteStations
    val recentStations: Flow<List<RadioStation>> = historyRepository.recentStations

    private val _topStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val topStations: StateFlow<List<RadioStation>> = _topStations.asStateFlow()

    private val _searchResults = MutableStateFlow<List<RadioStation>>(emptyList())
    val searchResults: StateFlow<List<RadioStation>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _fetchFailed = MutableStateFlow(false)
    val fetchFailed: StateFlow<Boolean> = _fetchFailed.asStateFlow()

    private val _sortOrder = MutableStateFlow(RadioSortOrder.POPULARITY)
    val sortOrder: StateFlow<RadioSortOrder> = _sortOrder.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.DESCENDING)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    private val _countries = MutableStateFlow<List<String>>(emptyList())
    val countries: StateFlow<List<String>> = _countries.asStateFlow()

    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry: StateFlow<String?> = _selectedCountry.asStateFlow()

    private val _states = MutableStateFlow<List<String>>(emptyList())
    val states: StateFlow<List<String>> = _states.asStateFlow()

    private val _selectedState = MutableStateFlow<String?>(null)
    val selectedState: StateFlow<String?> = _selectedState.asStateFlow()

    private val _languages = MutableStateFlow<List<String>>(emptyList())
    val languages: StateFlow<List<String>> = _languages.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private var lastQuery = ""

    init {
        viewModelScope.launch {
            val prefs = appPreferencesRepository.preferences.first()
            _sortOrder.value = runCatching { RadioSortOrder.valueOf(prefs.radioSortOrder) }.getOrDefault(RadioSortOrder.POPULARITY)
            _sortDirection.value = runCatching { SortDirection.valueOf(prefs.radioSortDirection) }.getOrDefault(SortDirection.DESCENDING)
            _selectedState.value = prefs.radioSelectedState.ifBlank { null }
            _selectedLanguage.value = prefs.radioSelectedLanguage.ifBlank { null }
            _selectedTag.value = prefs.radioSelectedTag.ifBlank { null }

            _selectedCountry.value = if (!prefs.radioDefaultCountryApplied && prefs.radioSelectedCountry.isBlank()) {
                val deviceCountry = Locale.getDefault().displayCountry.takeIf { it.isNotBlank() }
                appPreferencesRepository.setRadioDefaultCountryApplied(true)
                appPreferencesRepository.setRadioSelectedCountry(deviceCountry ?: "")
                deviceCountry
            } else {
                prefs.radioSelectedCountry.ifBlank { null }
            }

            serviceSettingsRepository.settings.collect { settings ->
                val radioEnabled = settings.isEnabled(ExternalService.RADIO_BROWSER)
                if (!radioEnabled) {
                    _isOffline.value = true
                    _topStations.value = emptyList()
                    _searchResults.value = emptyList()
                } else {
                    refresh()
                    if (_countries.value.isEmpty()) {
                        _countries.value = apiClient.countries()
                        _languages.value = apiClient.languages()
                        _tags.value = apiClient.tags()
                        _states.value = apiClient.states(_selectedCountry.value)
                    }
                }
            }
        }
    }

    private suspend fun isRadioEnabled(): Boolean =
        serviceSettingsRepository.settings.first().isEnabled(ExternalService.RADIO_BROWSER)

    private fun isOnline(): Boolean {
        val app = getApplication<Application>()
        val connectivityManager = app.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun refresh() {
        val radioEnabled = isRadioEnabled()
        _isOffline.value = !radioEnabled || !isOnline()
        if (_isOffline.value) {
            _topStations.value = emptyList()
            _searchResults.value = emptyList()
            _fetchFailed.value = false
            return
        }
        val reverse = _sortDirection.value == SortDirection.DESCENDING
        val country = _selectedCountry.value
        val state = _selectedState.value
        val language = _selectedLanguage.value
        val tag = _selectedTag.value
        if (lastQuery.isBlank()) {
            val result = apiClient.topStations(
                country = country, state = state, language = language, tag = tag, sort = _sortOrder.value, reverse = reverse
            )
            _fetchFailed.value = result == null
            if (result != null) _topStations.value = result
        } else {
            _isSearching.value = true
            val result = apiClient.search(
                lastQuery, country = country, state = state, language = language, tag = tag, sort = _sortOrder.value, reverse = reverse
            )
            _fetchFailed.value = result == null
            if (result != null) _searchResults.value = result
            _isSearching.value = false
        }
    }

    fun retry() {
        viewModelScope.launch { if (isRadioEnabled()) refresh() }
    }

    fun setCountry(country: String?) {
        _selectedCountry.value = country
        _selectedState.value = null
        viewModelScope.launch {
            appPreferencesRepository.setRadioSelectedCountry(country ?: "")
            appPreferencesRepository.setRadioSelectedState("")
            if (isRadioEnabled()) {
                val states = apiClient.states(country)
                if (_selectedCountry.value == country) _states.value = states
                refresh()
            }
        }
    }

    fun setState(state: String?) {
        _selectedState.value = state
        viewModelScope.launch {
            appPreferencesRepository.setRadioSelectedState(state ?: "")
            if (isRadioEnabled()) refresh()
        }
    }

    fun setLanguage(language: String?) {
        _selectedLanguage.value = language
        viewModelScope.launch {
            appPreferencesRepository.setRadioSelectedLanguage(language ?: "")
            if (isRadioEnabled()) refresh()
        }
    }

    fun setTag(tag: String?) {
        _selectedTag.value = tag
        viewModelScope.launch {
            appPreferencesRepository.setRadioSelectedTag(tag ?: "")
            if (isRadioEnabled()) refresh()
        }
    }

    fun search(query: String) {
        lastQuery = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            if (!isRadioEnabled()) {
                _searchResults.value = emptyList()
                return@launch
            }
            refresh()
        }
    }

    fun setSortOrder(order: RadioSortOrder) {
        _sortDirection.value = if (order == _sortOrder.value) _sortDirection.value.flipped() else SortDirection.DESCENDING
        _sortOrder.value = order
        viewModelScope.launch {
            appPreferencesRepository.setRadioSortOrder(_sortOrder.value.name)
            appPreferencesRepository.setRadioSortDirection(_sortDirection.value.name)
            if (isRadioEnabled()) refresh()
        }
    }

    fun resetFiltersAndSort() {
        _selectedCountry.value = null
        _selectedState.value = null
        _selectedLanguage.value = null
        _selectedTag.value = null
        _sortOrder.value = RadioSortOrder.POPULARITY
        _sortDirection.value = SortDirection.DESCENDING
        viewModelScope.launch {
            appPreferencesRepository.setRadioSelectedCountry("")
            appPreferencesRepository.setRadioSelectedState("")
            appPreferencesRepository.setRadioSelectedLanguage("")
            appPreferencesRepository.setRadioSelectedTag("")
            appPreferencesRepository.setRadioSortOrder(RadioSortOrder.POPULARITY.name)
            appPreferencesRepository.setRadioSortDirection(SortDirection.DESCENDING.name)
            if (isRadioEnabled()) refresh()
        }
    }

    fun setFavorite(station: RadioStation, favorite: Boolean) {
        viewModelScope.launch {
            if (favorite) favoritesRepository.add(station) else favoritesRepository.remove(station.uuid)
        }
    }

    fun addCustomStation(name: String, url: String) {
        viewModelScope.launch { favoritesRepository.add(customRadioStation(name, url)) }
    }

    // Called when a station actually starts playing: records local history and pings
    // Radio Browser's click endpoint so its popularity ranking stays meaningful.
    fun recordPlay(station: RadioStation) {
        viewModelScope.launch { historyRepository.recordPlayed(station) }
        if (!station.uuid.startsWith("custom:")) {
            viewModelScope.launch { apiClient.trackClick(station.uuid) }
        }
    }
}
