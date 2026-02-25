package com.ifpe.edu.br.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ifpe.edu.br.common.contracts.UIState
import com.ifpe.edu.br.model.Constants
import com.ifpe.edu.br.model.repository.Repository
import com.ifpe.edu.br.model.repository.remote.dto.AirPowerNotificationItem
import com.ifpe.edu.br.model.repository.remote.dto.AlarmInfo
import com.ifpe.edu.br.model.repository.remote.dto.AllMetricsWrapper
import com.ifpe.edu.br.model.repository.remote.dto.DashboardInfo
import com.ifpe.edu.br.model.repository.remote.dto.DeviceSummary
import com.ifpe.edu.br.model.repository.remote.dto.Id
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggDataWrapperResponse
import com.ifpe.edu.br.model.repository.remote.dto.agg.AggregationRequest
import com.ifpe.edu.br.model.repository.remote.dto.agg.generateCacheKey
import com.ifpe.edu.br.model.repository.remote.dto.auth.AuthUser
import com.ifpe.edu.br.model.repository.remote.dto.error.ErrorCode
import com.ifpe.edu.br.model.util.AirPowerLog
import com.ifpe.edu.br.model.util.ResultWrapper
import com.ifpe.edu.br.view.manager.UIStateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentHashMap

/*
* Trabalho de conclusão de curso - IFPE 2025
* Author: Willian Santos
* Project: AirPower Costumer
*/

class AirPowerViewModel(
    application: Application,
    connection: Retrofit
) : AndroidViewModel(application) {

    private val TAG: String = AirPowerViewModel::class.java.simpleName

    // INTERVAL
    private val MINUTE = 60000L
    private val CACHE_CLEANUP_INTERVAL = MINUTE * 2
    private val FETCH_INTERVAL_DEVICE = MINUTE * 5
    private val FETCH_INTERVAL_NOTIFICATION = 30 * 1000L
    private val FETCH_INTERVAL_AUTO_UPDATE = 2 * MINUTE
    private val FETCH_INTERVAL_ALARM = MINUTE
    private val MIN_DELAY_UI = 1500L
    private val MIN_DELAY_CARD = 800L

    val uiStateManager = UIStateManager.getInstance()
    private var repository = Repository.getInstance()
    private val jobs: MutableMap<String, Job> = mutableMapOf()
    private val aggregationDataCache =
        ConcurrentHashMap<String, MutableStateFlow<ResultWrapper<AggDataWrapperResponse>>>()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val DEVICE_JOB = "DEVICE_JOB"
    private val ALARMS_JOB = "ALARMS_JOB"
    private val NOTIFICATIONS_JOB = "NOTIFICATIONS_JOB"
    private val DASHBOARDS_JOB = "DASHBOARDS_JOB"

    init {
        startCacheCleanupJob()
    }

    private fun startCacheCleanupJob(): Job {
        return viewModelScope.launch {
            while (isActive) {
                delay(CACHE_CLEANUP_INTERVAL)
                val initialSize = aggregationDataCache.size
                if (initialSize > 0) {
                    aggregationDataCache.entries.removeAll { (_, flow) ->
                        val hasNoSubscribers = flow.subscriptionCount.value == 0
                        val isTerminalState =
                            flow.value is ResultWrapper.Success ||
                                    flow.value is ResultWrapper.ApiError ||
                                    flow.value is ResultWrapper.NetworkError ||
                                    flow.value is ResultWrapper.GenericError
                        hasNoSubscribers && isTerminalState
                    }
                    val finalSize = aggregationDataCache.size
                    if (initialSize != finalSize) {
                        if (AirPowerLog.ISVERBOSE)
                            AirPowerLog.d(
                                TAG,
                                "Cache cleanup ran. Removed ${initialSize - finalSize} entries."
                            )
                    }
                }
            }
        }
    }

    fun initSession(
        user: AuthUser,
    ) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val uiStateKey = Constants.UIStateKey.LOGIN_KEY
            uiStateManager.setUIState(
                uiStateKey,
                UIState(Constants.UIState.STATE_LOADING)
            )
            var isAuthSuccess = false

            // 1. Autenticação
            when (val authResponse = repository.authenticateDirect(user = user)) {
                is ResultWrapper.ApiError -> {
                    delay(getTimeLeftDelay(startTime))
                    handleApiError(authResponse.errorCode, uiStateKey)
                }
                is ResultWrapper.NetworkError -> {
                    delay(getTimeLeftDelay(startTime))
                    handleNetworkError(uiStateKey)
                }
                is ResultWrapper.GenericError -> {
                    delay(getTimeLeftDelay(startTime))
                    // Usa um código genérico se disponível no enum, ou trata no else do handler
                    handleApiError(ErrorCode.AP_GENERIC_ERROR, uiStateKey)
                }
                is ResultWrapper.Success<*> -> {
                    isAuthSuccess = true
                }
                ResultWrapper.Empty -> {}
            }

            var isGetUserSuccess = false
            if (isAuthSuccess) {
                // 2. Recuperar Usuário
                when (val currentUserResponse = repository.retrieveCurrentUserDirect()) {
                    is ResultWrapper.ApiError -> {
                        delay(getTimeLeftDelay(startTime))
                        handleApiError(currentUserResponse.errorCode, uiStateKey)
                    }
                    is ResultWrapper.NetworkError -> {
                        delay(getTimeLeftDelay(startTime))
                        handleNetworkError(uiStateKey)
                    }
                    is ResultWrapper.GenericError -> {
                        delay(getTimeLeftDelay(startTime))
                        handleApiError(ErrorCode.AP_GENERIC_ERROR, uiStateKey)
                    }
                    is ResultWrapper.Success<*> -> {
                        // O repositório já salvou no banco e atualizou o cache da authority
                        isGetUserSuccess = true
                    }
                    ResultWrapper.Empty -> {}
                }
            }

            if (isAuthSuccess && isGetUserSuccess) {
                delay(getTimeLeftDelay(startTime))
                handleSuccess(uiStateKey)
            }
        }
    }

    fun getAggregatedDataState(request: AggregationRequest): StateFlow<ResultWrapper<AggDataWrapperResponse>> {
        val cacheKey = request.generateCacheKey()
        return aggregationDataCache.getOrPut(cacheKey) {
            MutableStateFlow(ResultWrapper.Empty)
        }
    }

    fun fetchAggregatedData(request: AggregationRequest) {
        AirPowerLog.e("TAG", "fetchAggregatedData: request: $request")
        val flow = getAggregatedDataState(request) as MutableStateFlow
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val aggKey = Constants.UIStateKey.AGG_DATA_KEY
            uiStateManager.setUIState(aggKey, UIState(Constants.UIState.STATE_LOADING))
            val sessionKey = Constants.UIStateKey.SESSION
            uiStateManager.setUIState(sessionKey, UIState(Constants.UIState.EMPTY_STATE))

            val result = repository.retrieveAllDeviceAggregatedDataWrapper(request)

            when (result) {
                is ResultWrapper.ApiError -> {
                    handleApiError(result.errorCode, sessionKey)
                    handleApiError(result.errorCode, aggKey, getTimeLeftDelayCard(startTime))
                }
                is ResultWrapper.NetworkError -> {
                    handleNetworkError(sessionKey)
                    handleNetworkError(aggKey, getTimeLeftDelayCard(startTime))
                }
                is ResultWrapper.GenericError -> {
                    handleApiError(ErrorCode.AP_GENERIC_ERROR, sessionKey)
                    handleApiError(ErrorCode.AP_GENERIC_ERROR, aggKey, getTimeLeftDelayCard(startTime))
                }
                is ResultWrapper.Success<AggDataWrapperResponse> -> {
                    handleSuccess(sessionKey)
                    handleSuccess(aggKey, getTimeLeftDelayCard(startTime))
                }
                ResultWrapper.Empty -> { }
            }
            flow.value = result
        }
    }

    private fun getTimeLeftDelay(startTime: Long): Long {
        val timeDelayed = System.currentTimeMillis() - startTime
        return (MIN_DELAY_UI - timeDelayed).coerceAtLeast(0L)
    }

    private fun getTimeLeftDelayCard(startTime: Long): Long {
        val timeDelayed = System.currentTimeMillis() - startTime
        return (MIN_DELAY_CARD - timeDelayed).coerceAtLeast(0L)
    }

    fun getDevicesSummary(): StateFlow<List<DeviceSummary>> {
        return repository.devicesSummary
    }

    fun updateSession() {
        viewModelScope.launch {
            val uiStateKey = Constants.UIStateKey.REFRESH_TOKEN_KEY
            when (val refreshTokenResultWrapper = repository.updateSession()) {
                is ResultWrapper.ApiError -> {
                    handleApiError(refreshTokenResultWrapper.errorCode, uiStateKey)
                }
                is ResultWrapper.NetworkError -> {
                    handleNetworkError(uiStateKey)
                }
                is ResultWrapper.GenericError -> {
                    handleApiError(ErrorCode.AP_GENERIC_ERROR, uiStateKey)
                }
                is ResultWrapper.Success<*> -> {
                    handleSuccess(uiStateKey)
                }
                ResultWrapper.Empty -> {}
            }
        }
    }

    fun isSessionExpired() {
        viewModelScope.launch {
            val uiStateKey = Constants.UIStateKey.SESSION
            if (repository.isSessionExpired()) {
                uiStateManager.setUIState(
                    uiStateKey,
                    UIState(Constants.UIState.STATE_REFRESH_TOKEN)
                )
            } else {
                handleSuccess(uiStateKey)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            stopAllFetchers()
        }
    }

    fun resetUIState(stateId: String) {
        uiStateManager.setUIState(stateId, getEmptyValueUIState())
    }

    fun startDataFetchers() {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "startDataFetchers()")
        if (jobs[DEVICE_JOB]?.isActive != true) {
            jobs[DEVICE_JOB] = fetchDevicesData()
        }
        if (jobs[ALARMS_JOB]?.isActive != true) {
            jobs[ALARMS_JOB] = fetchAlarmData()
        }
        if (jobs[NOTIFICATIONS_JOB]?.isActive != true) {
            jobs[NOTIFICATIONS_JOB] = fetchNotificationData()
        }
        if (jobs[DASHBOARDS_JOB]?.isActive != true) {
            jobs[DASHBOARDS_JOB] = fetchDashboards()
        }
    }

    fun getDashboardsForCurrentUser(): StateFlow<List<DashboardInfo>> {
        return repository.getDashBoards()
    }

    private fun fetchDashboards(): Job {
        return viewModelScope.launch {
            val uiStateKey = Constants.UIStateKey.DASHBOARDS_KEY
            while (isActive) {
                when (val resultWrapper = repository.retrieveDashBoardsForCurrentUser()) {
                    is ResultWrapper.ApiError -> {
                        handleApiError(resultWrapper.errorCode, uiStateKey)
                    }
                    is ResultWrapper.NetworkError -> {
                        handleNetworkError(uiStateKey)
                    }
                    is ResultWrapper.GenericError -> {
                        handleApiError(ErrorCode.AP_GENERIC_ERROR, uiStateKey)
                    }
                    is ResultWrapper.Success<List<DashboardInfo>> -> {
                        handleSuccess(uiStateKey)
                    }
                    ResultWrapper.Empty -> {}
                }
                delay(FETCH_INTERVAL_AUTO_UPDATE)
            }
        }
    }

    private fun fetchNotificationData(): Job {
        return viewModelScope.launch {
            val notificationsKey = Constants.UIStateKey.NOTIFICATIONS_KEY
            while (isActive) {
                when (val resultWrapper = repository.retrieveNotifications()) {
                    is ResultWrapper.Success -> {
                        handleSuccess(notificationsKey)
                    }
                    is ResultWrapper.ApiError -> {
                        handleApiError(resultWrapper.errorCode, notificationsKey)
                    }
                    is ResultWrapper.NetworkError -> {
                        handleNetworkError(notificationsKey)
                    }
                    is ResultWrapper.GenericError -> {
                        handleApiError(ErrorCode.AP_GENERIC_ERROR, notificationsKey)
                    }
                    ResultWrapper.Empty -> {}
                }
                delay(FETCH_INTERVAL_NOTIFICATION)
            }
        }
    }

    private fun fetchDevicesData(): Job {
        return viewModelScope.launch {
            val deviceSummarySummaryKey = Constants.UIStateKey.DEVICE_SUMMARY_KEY
            val uiStateKey = Constants.UIStateKey.SESSION
            while (isActive) {
                when (val resultWrapper = repository.retrieveDeviceSummaryForCurrentUser()) {
                    is ResultWrapper.Success -> {
                        handleSuccess(deviceSummarySummaryKey)
                    }
                    is ResultWrapper.ApiError -> {
                        handleApiError(resultWrapper.errorCode, deviceSummarySummaryKey)
                        handleApiError(resultWrapper.errorCode, uiStateKey)
                    }
                    is ResultWrapper.NetworkError -> {
                        handleNetworkError(deviceSummarySummaryKey)
                        handleNetworkError(uiStateKey)
                    }
                    is ResultWrapper.GenericError -> {
                        handleApiError(ErrorCode.AP_GENERIC_ERROR, deviceSummarySummaryKey)
                        handleApiError(ErrorCode.AP_GENERIC_ERROR, uiStateKey)
                    }
                    ResultWrapper.Empty -> {}
                }
                delay(FETCH_INTERVAL_DEVICE)
            }
        }
    }

    @Deprecated("Marked to be removed on text release")
    fun fetchAllDashboardsMetricsWrapper(): Job {
        return viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val sessionStateKey = Constants.UIStateKey.SESSION
            val fetchMetricsKey = Constants.UIStateKey.METRICS_KEY
            uiStateManager.setUIState(fetchMetricsKey, UIState(Constants.UIState.STATE_LOADING))

            when (val resultWrapper = repository.fetchAllDashboardsMetricsWrapper()) {
                is ResultWrapper.Success -> {
                    handleSuccess(sessionStateKey)
                }
                is ResultWrapper.ApiError -> {
                    handleApiError(resultWrapper.errorCode, sessionStateKey)
                }
                is ResultWrapper.NetworkError -> {
                    handleNetworkError(sessionStateKey)
                }
                is ResultWrapper.GenericError -> {
                    handleApiError(ErrorCode.AP_GENERIC_ERROR, sessionStateKey)
                }
                ResultWrapper.Empty -> {}
            }
            delay(getTimeLeftDelayCard(startTime))
            uiStateManager.setUIState(fetchMetricsKey, UIState(Constants.UIState.STATE_SUCCESS))
        }
    }

    fun markNotificationAsRead(notificationId: Id): Job {
        return viewModelScope.launch {
            val uiStateKey = Constants.UIStateKey.SESSION
            when (val resultWrapper = repository.markNotificationAsRead(notificationId)) {
                is ResultWrapper.Success -> {}
                is ResultWrapper.ApiError -> {
                    handleApiError(resultWrapper.errorCode, uiStateKey)
                }
                is ResultWrapper.NetworkError -> {
                    handleNetworkError(uiStateKey)
                }
                is ResultWrapper.GenericError -> {
                    handleApiError(ErrorCode.AP_GENERIC_ERROR, uiStateKey)
                }
                ResultWrapper.Empty -> {}
            }
        }
    }

    fun removeReadNotification(notificationId: Id) {
        repository.removeReadNotification(notificationId)
    }

    private fun fetchAlarmData(): Job {
        return viewModelScope.launch {
            val alarmsKey = Constants.UIStateKey.ALARMS_KEY
            val uiStateKey = Constants.UIStateKey.SESSION
            while (isActive) {
                when (val resultWrapper = repository.retrieveAlarmInfo()) {
                    is ResultWrapper.Success -> {
                        handleSuccess(alarmsKey)
                    }
                    is ResultWrapper.ApiError -> {
                        handleApiError(resultWrapper.errorCode, alarmsKey)
                        handleApiError(resultWrapper.errorCode, uiStateKey)
                    }
                    is ResultWrapper.NetworkError -> {
                        handleNetworkError(alarmsKey)
                        handleNetworkError(uiStateKey)
                    }
                    is ResultWrapper.GenericError -> {
                        handleApiError(ErrorCode.AP_GENERIC_ERROR, alarmsKey)
                        handleApiError(ErrorCode.AP_GENERIC_ERROR, uiStateKey)
                    }
                    ResultWrapper.Empty -> {}
                }
                delay(FETCH_INTERVAL_ALARM)
            }
        }
    }

    private fun getEmptyValueUIState(): UIState {
        return UIState(Constants.UIState.EMPTY_STATE)
    }

    private suspend fun handleApiError(
        code: ErrorCode,
        uiStateKey: String,
        delay: Long = 0
    ) {
        val subTag = "handleApiError()"
        if (AirPowerLog.ISLOGABLE)
            AirPowerLog.d(TAG, "[$subTag]: code:$code stateKey:$uiStateKey delayed:${delay > 0}")

        when (code) {
            ErrorCode.TB_INVALID_CREDENTIALS -> {
                if (AirPowerLog.ISVERBOSE) AirPowerLog.d(TAG, "[$subTag]: TB_INVALID_CREDENTIALS")
                delay(delay)
                uiStateManager.setUIState(
                    uiStateKey, UIState(Constants.UIState.STATE_AUTHENTICATION_FAILURE)
                )
            }

            ErrorCode.TB_REFRESH_TOKEN_EXPIRED -> {
                if (AirPowerLog.ISVERBOSE)
                    AirPowerLog.d(TAG, "[$subTag]: TB_REFRESH_TOKEN_EXPIRED")
                delay(delay)
                uiStateManager.setUIState(
                    uiStateKey,
                    UIState(Constants.UIState.STATE_REQUEST_LOGIN)
                )
            }

            ErrorCode.AP_REFRESH_TOKEN_EXPIRED -> {
                if (AirPowerLog.ISVERBOSE)
                    AirPowerLog.d(TAG, "[$subTag]: AP_REFRESH_TOKEN_EXPIRED")
                delay(delay)
                uiStateManager.setUIState(
                    uiStateKey,
                    UIState(Constants.UIState.STATE_REQUEST_LOGIN)
                )
            }

            ErrorCode.AP_JWT_EXPIRED -> {
                if (AirPowerLog.ISVERBOSE)
                    AirPowerLog.d(TAG, "[$subTag]: AP_JWT_EXPIRED")
                delay(delay)
                uiStateManager.setUIState(
                    uiStateKey,
                    UIState(Constants.UIState.STATE_UPDATE_SESSION)
                )
            }

            else -> {
                if (AirPowerLog.ISVERBOSE)
                    AirPowerLog.d(TAG, "else -> GENERIC_ERROR")
                delay(delay)
                uiStateManager.setUIState(
                    uiStateKey, UIState(Constants.UIState.GENERIC_ERROR)
                )
            }
        }
    }

    private suspend fun handleNetworkError(
        uiStateKey: String,
        delay: Long = 0
    ) {
        val subTag = "handleNetworkError()"
        if (AirPowerLog.ISLOGABLE)
            AirPowerLog.d(TAG, "[$subTag]: stateKey:$uiStateKey delayed:${delay > 0}")
        delay(delay)
        uiStateManager.setUIState(
            uiStateKey, UIState(
                Constants.UIState.STATE_NETWORK_ISSUE
            )
        )
    }

    private suspend fun handleSuccess(
        uiStateKey: String,
        delay: Long = 0
    ) {
        val subTag = "handleSuccess()"
        if (AirPowerLog.ISLOGABLE)
            AirPowerLog.d(TAG, "[$subTag]: stateKey:$uiStateKey delayed:${delay > 0}")
        delay(delay)
        uiStateManager.setUIState(
            uiStateKey,
            UIState(Constants.UIState.STATE_SUCCESS)
        )
    }

    fun isUserLoggedIn(): Boolean {
        return repository.isUserLoggedIn()
    }

    fun getDeviceById(deviceId: String): DeviceSummary {
        return repository.getDeviceById(deviceId)
    }

    fun getAlarmInfoSet(): StateFlow<List<AlarmInfo>> {
        return repository.alarmInfo
    }

    @Deprecated("Marked to be removed on text release")
    fun getAllDevicesMetricsWrapper(): StateFlow<AllMetricsWrapper> {
        return repository.allDevicesMetricsWrapper
    }

    @Deprecated("Marked to be removed on text release")
    fun getUserDashBoardsDataWrapper(): StateFlow<List<AllMetricsWrapper>> {
        return repository.dashBoardsMetricsWrapper
    }

    fun getNotifications(): StateFlow<List<AirPowerNotificationItem>> {
        return repository.getNotifications()
    }

    fun stopAllFetchers() {
        if (AirPowerLog.ISLOGABLE) AirPowerLog.d(TAG, "stopAllFetchers()")
        jobs.values.forEach { job ->
            job.cancel()
        }
    }

    fun forceRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            stopAllFetchers()
            delay(500)
            startDataFetchers()
            _isRefreshing.value = false
        }
    }

    // Método para a AuthScreen consultar se é admin
    fun getCurrentUserAuthority(): String {
        return repository.getCachedUserAuthority()
    }
}