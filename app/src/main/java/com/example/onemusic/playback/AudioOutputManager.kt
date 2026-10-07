package com.example.onemusic.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioOutputDevice(
    val id: Int,
    val name: String,
    val type: Int,
    val audioDeviceInfo: AudioDeviceInfo,
    val icon: ImageVector,
    val isSpeaker: Boolean = false,
    val isBluetooth: Boolean = false
)

class AudioOutputManager(
    private val context: Context,
    private val onDeviceSelectedCallback: (AudioDeviceInfo?) -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _availableDevices = MutableStateFlow<List<AudioOutputDevice>>(emptyList())
    val availableDevices: StateFlow<List<AudioOutputDevice>> = _availableDevices.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<Int?>(null)
    val selectedDeviceId: StateFlow<Int?> = _selectedDeviceId.asStateFlow()

    var onHeadsetConnected: (() -> Unit)? = null
    var onHeadsetDisconnected: (() -> Unit)? = null

    private fun isExternalAudioDevice(type: Int): Boolean {
        return type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER || type == AudioDeviceInfo.TYPE_BLE_BROADCAST))
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshDevices()
            val hasExternal = addedDevices?.any { isExternalAudioDevice(it.type) } == true
            if (hasExternal) {
                onHeadsetConnected?.invoke()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshDevices()
            val hasExternal = removedDevices?.any { isExternalAudioDevice(it.type) } == true
            if (hasExternal) {
                onHeadsetDisconnected?.invoke()
            }
        }
    }

    init {
        try {
            audioManager?.registerAudioDeviceCallback(deviceCallback, null)
        } catch (_: Exception) {}
        refreshDevices()
    }

    fun refreshDevices() {
        val am = audioManager ?: return
        val rawDevices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val list = mutableListOf<AudioOutputDevice>()

        // Nhóm và chuyển đổi sang AudioOutputDevice
        for (dev in rawDevices) {
            val type = dev.type
            // Bỏ qua earpiece cho nghe nhạc thường nếu có loa ngoài
            if (type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) continue

            val isSpeaker = type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE)

            val isBluetooth = type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER || type == AudioDeviceInfo.TYPE_BLE_BROADCAST))

            val icon: ImageVector = when {
                isSpeaker -> Icons.Rounded.PhoneAndroid
                isBluetooth -> if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET)) Icons.Rounded.Headphones else Icons.Rounded.Speaker
                type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> Icons.Rounded.Headphones
                type == AudioDeviceInfo.TYPE_USB_DEVICE || type == AudioDeviceInfo.TYPE_USB_HEADSET || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && type == AudioDeviceInfo.TYPE_USB_ACCESSORY) -> Icons.Rounded.Usb
                type == AudioDeviceInfo.TYPE_HDMI || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && (type == AudioDeviceInfo.TYPE_HDMI_ARC || type == AudioDeviceInfo.TYPE_HDMI_EARC)) -> Icons.Rounded.Tv
                else -> Icons.Rounded.Speaker
            }

            // Lấy tên thiết bị thực tế
            val realName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dev.productName?.toString()?.takeIf { it.isNotBlank() }
            } else null

            val displayName = when {
                isSpeaker -> "Loa điện thoại"
                !realName.isNullOrBlank() -> realName
                isBluetooth -> "Tai nghe / Loa Bluetooth"
                type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Tai nghe có dây"
                type == AudioDeviceInfo.TYPE_USB_DEVICE || type == AudioDeviceInfo.TYPE_USB_HEADSET -> "Tai nghe USB-C / DAC"
                else -> "Đầu ra âm thanh"
            }

            list.add(
                AudioOutputDevice(
                    id = dev.id,
                    name = displayName,
                    type = type,
                    audioDeviceInfo = dev,
                    icon = icon,
                    isSpeaker = isSpeaker,
                    isBluetooth = isBluetooth
                )
            )
        }

        // Đảm bảo Loa điện thoại luôn nằm đầu danh sách nếu có
        val sortedList = list.distinctBy { it.id }.sortedWith(
            compareByDescending<AudioOutputDevice> { it.isBluetooth }
                .thenByDescending { it.isSpeaker }
        )

        _availableDevices.value = sortedList

        // Nếu chưa chọn thiết bị nào hoặc thiết bị cũ bị ngắt, chọn thiết bị mặc định
        if (_selectedDeviceId.value == null || sortedList.none { it.id == _selectedDeviceId.value }) {
            val defaultDev = sortedList.firstOrNull { it.isBluetooth } ?: sortedList.firstOrNull { it.isSpeaker } ?: sortedList.firstOrNull()
            _selectedDeviceId.value = defaultDev?.id
        }
    }

    fun selectDevice(device: AudioOutputDevice) {
        _selectedDeviceId.value = device.id
        onDeviceSelectedCallback(device.audioDeviceInfo)
    }

    fun release() {
        try {
            audioManager?.unregisterAudioDeviceCallback(deviceCallback)
        } catch (_: Exception) {}
    }
}
