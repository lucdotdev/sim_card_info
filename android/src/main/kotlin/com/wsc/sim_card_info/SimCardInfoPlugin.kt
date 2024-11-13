package com.wsc.sim_card_info
import java.io.StringWriter
import android.util.JsonWriter
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener

//import com.google.gson.Gson


/** SimCardInfoPlugin */
class SimCardInfoPlugin : FlutterPlugin, MethodCallHandler, ActivityAware,
    RequestPermissionsResultListener {
    private var context: Context? = null
    private lateinit var channel: MethodChannel
    private var methodChannelName = "getSimInfo"

    private var result: Result? = null
    private var permissionEvent: EventSink? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "sim_card_info")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == methodChannelName) {
            getSimInfo()?.let {
                result.success(it)
            } ?: result.error("NO_CONTEXT", "Context is null", null)
        } else {
            result.notImplemented()
        }
    }

    @SuppressLint("HardwareIds")
    private fun getSimInfo(): String? {
        val currentContext = context ?: return null
        
        val simCardInfo = StringWriter()
        val writer = JsonWriter(simCardInfo)
        writer.beginArray()
        val telephonyManager =
            currentContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        if (telephonyManager == null || ActivityCompat.checkSelfPermission(
                currentContext,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return "Permission denied"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val subscriptionManager = currentContext.getSystemService(SubscriptionManager::class.java)
            subscriptionManager?.activeSubscriptionInfoList?.let { subscriptionInfoList ->
                for (info in subscriptionInfoList) {
                    writer.beginObject()
                    writer.name("carrierName").value(info.carrierName.toString())
                    writer.name("displayName").value(info.displayName.toString())
                    writer.name("slotIndex").value(info.simSlotIndex.toString())
                    writer.name("number").value(info.number.toString())
                    writer.name("countryIso").value(info.countryIso.toString())
                    writer.name("countryPhonePrefix").value(info.countryIso.toString())
                    writer.endObject()
                }
            }
        } else {
            writer.beginObject()
            writer.name("carrierName").value(telephonyManager.networkOperatorName.toString())
            writer.name("displayName").value(telephonyManager.simOperatorName.toString())
            writer.name("slotIndex").value(telephonyManager.simSerialNumber.toString())
            writer.name("number").value(telephonyManager.line1Number.toString())
            writer.name("countryIso").value(telephonyManager.simCountryIso.toString())
            writer.name("countryPhonePrefix").value(telephonyManager.simCountryIso.toString())
            writer.endObject()
        }
        writer.endArray()
        return simCardInfo.toString()
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        context = binding.activity
        binding.addRequestPermissionsResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        context = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        context = binding.activity
        binding.addRequestPermissionsResultListener(this)
    }

    override fun onDetachedFromActivity() {
        channel.setMethodCallHandler(null)
        context = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionEvent?.success(true)
                getSimInfo()
                return true
            } else {
                permissionEvent?.success(false)
            }
        }
        result?.error("PERMISSION", "onRequestPermissionsResult is not granted", null)
        return false
    }
}

data class SimInfo(
    val carrierName: String = "",
    val displayName: String = "",
    val slotIndex: Int = 0,
    val number: String = "",
    val countryIso: String = "",
    val countryPhonePrefix: String = ""
) {}
