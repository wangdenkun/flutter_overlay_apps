package com.phan_tech.flutter_overlay_apps

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import io.flutter.FlutterInjector
import io.flutter.app.FlutterPluginRegistry
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.FlutterEngineGroup
import io.flutter.embedding.engine.dart.DartExecutor

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.*
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.*

const val mainAppMethodChannel: String = "com.phan_tech./flutter_overlay_apps"
const val overlayAppMethodChannel: String = "com.phan_tech/flutter_overlay_apps/overlay"
const val overlayAppMessageChannel: String = "com.phan_tech/flutter_overlay_apps/overlay/messenger"
const val OVERLAY_PERMISSION_REQUEST_CODE = 1;

/** FlutterOverlayAppsPlugin */
class FlutterOverlayAppsPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener, BasicMessageChannel.MessageHandler<Any?> {
    private lateinit var channel: MethodChannel
    private lateinit var messenger: BasicMessageChannel<Any?>
    private lateinit var context: Context
    private lateinit var activity: Activity
    private var permissionResult: Result? = null

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        WindowSetup.messenger = messenger
        WindowSetup.messenger!!.setMessageHandler(this)
        activity = binding.activity
        binding.addActivityResultListener(this)
        val enn = FlutterEngineGroup(context)
        val dEntry = DartExecutor.DartEntrypoint(
            FlutterInjector.instance().flutterLoader().findAppBundlePath(),
            "showOverlay"
        )

        val engine = enn.createAndRunEngine(context, dEntry)

        FlutterEngineCache.getInstance().put("my_engine_id", engine)

    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    }

    override fun onDetachedFromActivity() {
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, mainAppMethodChannel)
        channel.setMethodCallHandler(this)

        messenger = BasicMessageChannel(flutterPluginBinding.binaryMessenger, overlayAppMessageChannel, JSONMessageCodec.INSTANCE)
        messenger.setMessageHandler(this)
        this.context = flutterPluginBinding.applicationContext

    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "showOverlay" -> {
                // get permissions
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    result.error("1", "SDK version is lower than 23", "Requires android sdk 23 and above")
                } else if (!checkPermissions()) {
                    requestPermissions()
                } else {
                    val h = call.argument<Int>("height")
                    val w = call.argument<Int>("width")
                    val alignment = call.argument<String>("alignment")

                    WindowSetup.width = w ?: -1
                    WindowSetup.height = h ?: -1
                    WindowSetup.setGravityFromAlignment(alignment ?: "center")
                    activity.startService(Intent(context, OverlayService().javaClass))
                    result.success(true)
                }
            }
            "checkPermission" -> {
                permissionResult = result
                result.success(checkPermissions())
            }
            "requestPermission" -> {
                permissionResult = result
                requestPermissions()
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onMessage(message: Any?, reply: BasicMessageChannel.Reply<Any?>) {
        val overlayMessageChannel = BasicMessageChannel(FlutterEngineCache.getInstance().get("my_engine_id")!!.dartExecutor, overlayAppMessageChannel, JSONMessageCodec.INSTANCE)
        overlayMessageChannel.send(message, reply)
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private fun requestPermissions() {
        if (checkPermissions()){
            replyPermissionResult(true)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startCheckPermissionTask()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}"))
            activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        } else {
            replyPermissionResult(true)
        }
    }
    // 开启权限检测任务
    private fun startCheckPermissionTask(){
        val task = PermissionCheckTask(this)
        Timer().schedule(task, Date(),1000)
    }
    class PermissionCheckTask(private val plugin:FlutterOverlayAppsPlugin) : TimerTask() {
        override fun run() {
            if (plugin.checkPermissions()){
                plugin.replyPermissionResult(true)
                cancel()
            }
        }
    }

    /**
     * 悬浮授权回调
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    replyPermissionResult(true)
                }
                Activity.RESULT_CANCELED -> {
                    val hasPermission = checkPermissions();
                    replyPermissionResult(hasPermission)
                }
                else -> {
                    replyPermissionResult(false)
                }
            }
            true
        } else {
            replyPermissionResult(false)
            false
        }
    }
    fun replyPermissionResult(@NonNull success: Boolean){
        permissionResult?.run {
            success(success)
            permissionResult = null
        }
    }

}
