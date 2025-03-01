package com.phan_tech.flutter_overlay_apps

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import android.view.View
import android.view.WindowManager
import io.flutter.embedding.android.*
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.BasicMessageChannel
import io.flutter.plugin.common.JSONMessageCodec
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private lateinit var flutterView: FlutterView
    private val flutterChannel = MethodChannel(FlutterEngineCache.getInstance().get("my_engine_id")!!.dartExecutor, overlayAppMethodChannel)
    private val overlayMessageChannel = BasicMessageChannel(FlutterEngineCache.getInstance().get("my_engine_id")!!.dartExecutor, overlayAppMessageChannel, JSONMessageCodec.INSTANCE)

    override fun onBind(intent: Intent?): IBinder? {
        // Not used
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val engine = FlutterEngineCache.getInstance().get("my_engine_id")!!
        engine.lifecycleChannel.appIsResumed()

//        flutterView = FlutterView(applicationContext)
        flutterView = FlutterView(applicationContext, FlutterTextureView(applicationContext))

        flutterView.attachToFlutterEngine(FlutterEngineCache.getInstance().get("my_engine_id")!!)
        FlutterFragment.withCachedEngine("my_engine_id").renderMode(RenderMode.surface).transparencyMode(TransparencyMode.transparent)
        flutterView.fitsSystemWindows = true

        flutterChannel.setMethodCallHandler{ methodCall: MethodCall, result: MethodChannel.Result ->
            if(methodCall.method == "close"){
                val closed = stopService(Intent(baseContext, OverlayService().javaClass))
                result.success(closed)
            }
        }
        overlayMessageChannel.setMessageHandler(MyHandler())


        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager?

        val params = WindowManager.LayoutParams(
            WindowSetup.width,
            WindowSetup.height,
            WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = WindowSetup.gravity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }else{
            params.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_FULLSCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flutterView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        windowManager!!.addView(flutterView, params)
    }


    override fun onDestroy() {
        super.onDestroy()
        windowManager!!.removeView(flutterView)
    }
}

class MyHandler: BasicMessageChannel.MessageHandler<Any?>{
    override fun onMessage(message: Any?, reply: BasicMessageChannel.Reply<Any?>) {
        WindowSetup.messenger!!.send(message)
    }

}