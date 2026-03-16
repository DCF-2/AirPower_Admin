package com.ifpe.edu.br.model

class Constants {

    object AppConfig {
        // A chave de identificação deste app no banco de dados
        const val APP_CLIENT_NAME = "Airpower_Admin"
        // Chave do SharedPreferences para o IP dinâmico da nossa API Proxy
        const val PREF_PROXY_URL_KEY = "PROXY_BASE_URL"
    }

    object ResponseErrorCode {
        // --- Autenticação AirPower ---
        const val AP_JWT_EXPIRED = 30
        const val AP_REFRESH_TOKEN_EXPIRED = 31
        const val AP_GENERIC_ERROR = 32

        // --- Erros Genéricos ---
        const val UNKNOWN_INTERNAL_ERROR = 90
        const val SUCCESS = 0
        const val EMPTY_STATE_MESSAGE = ""
        const val EMPTY_STATE_CODE = -1
    }

    object UIState {
        const val EMPTY_STATE = "EMPTY_STATE"
        const val STATE_UPDATE_SESSION = "STATE_UPDATE_SESSION"
        const val STATE_REFRESH_TOKEN = "STATE_REFRESH_TOKEN"
        const val STATE_LOADING = "LOADING"
        const val STATE_REQUEST_LOGIN = "STATE_REQUEST_LOGIN"
        const val STATE_NETWORK_ISSUE = "NETWORK_ISSUE"
        const val STATE_SUCCESS = "SUCCESS"
        const val GENERIC_ERROR = "GENERIC_ERROR"
        const val STATE_AUTHENTICATION_FAILURE = "AUTHENTICATION_FAILURE"
    }

    object MetricsGroup {
        const val ALL = "ALL"
        const val DASHBOARDS = "DASHBOARDS"
    }

    object UIStateKey {
        const val STATE_ERROR = "STATE_ERROR"
        const val SESSION = "SESSION"
        const val AUTH_KEY = "AUTH_STATE"
        const val LOGIN_KEY = "STATE_LOGIN"
        const val REFRESH_TOKEN_KEY = "REFRESH_TOKEN_KEY"
        const val DEVICE_SUMMARY_KEY = "DEVICE_SUMMARY_KEY"
        const val ALARMS_KEY = "ALARMS_KEY"
        const val METRICS_KEY = "METRICS_KEY"
        const val DEVICE_METRICS_KEY = "DEVICE_METRICS_KEY"
        const val AGG_DATA_KEY = "AGG_DATA_KEY"
        const val NOTIFICATIONS_KEY = "NOTIFICATIONS_KEY"
        const val DASHBOARDS_KEY = "DASHBOARDS_KEY"
    }

    object ResKeys {
        const val KEY_COD_DRAWABLE: String = "drawable"
    }

    object NotificationState {
        const val READ = "READ"
    }

    object ServerConnectionIds {
        const val CONNECTION_ID_THINGSBOARD = 1
        const val CONNECTION_ID_AIR_POWER_SERVER = 2
    }
}