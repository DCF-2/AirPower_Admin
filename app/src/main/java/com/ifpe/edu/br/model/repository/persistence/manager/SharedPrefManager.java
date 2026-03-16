package com.ifpe.edu.br.model.repository.persistence.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.ifpe.edu.br.model.Constants;
import com.ifpe.edu.br.model.util.AirPowerLog;


public class SharedPrefManager {
    private static final String TAG = SharedPrefManager.class.getSimpleName();
    private static final String PREF_FILE_NAME = "AirPowerAdmin-Preference";
    private final SharedPreferences mSP;
    private final SharedPreferences.Editor mEditor;
    private static SharedPrefManager instance;

    private static final String KEY_IS_FIRST_RUN = "is_first_run";
    private Context context = null;

    public static SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    public static SharedPrefManager getInstance() throws Exception {
        if (instance == null) {
            throw new IllegalStateException("SharedPrefManager error: getInstance called before construction");
        }
        return instance;
    }

    private SharedPrefManager(Context context) {
        mSP = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        mEditor = mSP.edit();
        this.context = context;
    }

    public void writeString(String key, String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
            return;
        }
        mEditor.putString(key, value);
        mEditor.apply();
    }

    public void writeBoolean(String key, Boolean value) {
        if (key == null || key.isEmpty() || value == null) {
            return;
        }
        mEditor.putBoolean(key, value);
        mEditor.apply();
    }

    public String readString(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return mSP.getString(key, null);
    }

    // ==========================================
    // MÉTODOS DE REDE DINÂMICA (BFF PROXY)
    // ==========================================

    // Busca a URL do Servidor Proxy salva no celular (Ex: http://10.5.0.68:8080)
    public String getProxyBaseUrl() {
        return mSP.getString(Constants.AppConfig.PREF_PROXY_URL_KEY, "");
    }

    // Salva a nova URL do Servidor
    public void setProxyBaseUrl(String proxyUrl) {
        writeString(Constants.AppConfig.PREF_PROXY_URL_KEY, proxyUrl);
        writeBoolean(KEY_IS_FIRST_RUN, false);
    }

    public boolean isFirstRun() {
        return mSP.getBoolean(KEY_IS_FIRST_RUN, true);
    }
}