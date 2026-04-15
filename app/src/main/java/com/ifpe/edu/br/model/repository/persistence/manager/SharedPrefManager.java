package com.ifpe.edu.br.model.repository.persistence.manager;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.ifpe.edu.br.model.Constants;
import com.ifpe.edu.br.model.util.AirPowerLog;

public class SharedPrefManager {
    private static final String TAG = SharedPrefManager.class.getSimpleName();
    private static final String PREF_FILE_NAME = "AirPowerAdmin-Preference-Secure";
    private SharedPreferences mSP;
    private SharedPreferences.Editor mEditor;
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
        this.context = context.getApplicationContext(); // Previne Memory Leaks

        try {
            // 1. Cria a Chave Mestra baseada no hardware do aparelho (Android Keystore)
            MasterKey masterKey = new MasterKey.Builder(this.context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // 2. Inicializa o SharedPreferences Encriptado
            // A chave usa AES256-SIV e o valor usa AES256-GCM
            mSP = EncryptedSharedPreferences.create(
                    this.context,
                    PREF_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            mEditor = mSP.edit();

        } catch (Exception e) {
            AirPowerLog.e(TAG, "Falha Crítica ao inicializar EncryptedSharedPreferences: " + e.getMessage());
            // Em caso extremo de falha da Keystore (raro, mas acontece em devices antigos ou modificados)
            // Fazemos fallback para o antigo como medida de segurança mínima de funcionamento
            mSP = this.context.getSharedPreferences("AirPowerAdmin-Preference", Context.MODE_PRIVATE);
            mEditor = mSP.edit();
        }
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

    public String getProxyBaseUrl() {
        return mSP.getString(Constants.AppConfig.PREF_PROXY_URL_KEY, "");
    }

    public void setProxyBaseUrl(String proxyUrl) {
        writeString(Constants.AppConfig.PREF_PROXY_URL_KEY, proxyUrl);
        writeBoolean(KEY_IS_FIRST_RUN, false);
    }

    public boolean isFirstRun() {
        return mSP.getBoolean(KEY_IS_FIRST_RUN, true);
    }
}