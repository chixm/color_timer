package com.chixm.colortimer

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import androidx.core.content.edit
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
private const val TAG = "HeartRateService"

class HeartRateService : Service() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var passiveClient: androidx.health.services.client.PassiveMonitoringClient

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "HeartRateService started")
        sharedPreferences = getSharedPreferences("heart_rate_prefs", MODE_PRIVATE)
        val healthClient = HealthServices.getClient(this)
        passiveClient = healthClient.passiveMonitoringClient

        // PassiveListenerConfigを作成（データタイプを指定）
        val config = PassiveListenerConfig.builder()
            .setDataTypes(setOf(DataType.HEART_RATE_BPM))
            .build()

        val callback = object : PassiveListenerCallback {
            override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
                // DataPointContainer から指定された DataType のデータを取得
                val heartRateDataPoints = dataPoints.getData(DataType.HEART_RATE_BPM)
                Log.i(TAG, "onNewDataPointsReceived count=${heartRateDataPoints.size}")

                // 最新の心拍数を取得し、NumberならIntに変換
                val latestHeartRate = heartRateDataPoints.lastOrNull()?.value
                    .let { if (it is Number) it.toInt() else 0 }

                Log.i(TAG, "latestHeartRate=$latestHeartRate")
                saveHeartRate(latestHeartRate)
            }

            override fun onPermissionLost() {
                // 権限が失われた場合の処理（例: ログ出力やUI通知）
                Log.w("HeartRateService", "Permission lost for passive monitoring")
            }

            override fun onRegistered() {
                // リスナーが正常に登録された場合の処理
                Log.i("HeartRateService", "Passive listener registered successfully")
            }

            override fun onRegistrationFailed(throwable: Throwable) {
                // 登録に失敗した場合の処理
                Log.e("HeartRateService", "Failed to register passive listener", throwable)
            }
        }

        // 正しいメソッドで登録
        passiveClient.setPassiveListenerCallback(config, callback)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 正しいメソッドで解除（非同期）
        passiveClient.clearPassiveListenerCallbackAsync().addListener({
            Log.i("HeartRateService", "Passive listener cleared")
        }, { command -> command.run() })  // Executorで実行
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun saveHeartRate(heartRate: Int) {
        sharedPreferences.edit {
            putInt("latest_heart_rate", heartRate)
        }
    }
}