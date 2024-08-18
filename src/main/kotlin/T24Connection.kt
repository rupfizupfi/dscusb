package ch.rupfizupfi.dscusb

import ch.rupfizupfi.dscusb.T24.DataCallback
import ch.rupfizupfi.dscusb.T24.WakeUpCallback

class T24Connection {
    private val t24wifi: T24WIFI = T24WIFI()
    private var id: Int = 0
    private var baseStation: Byte = 0
    private var dataTag: Int = 0

    fun open() {
        val port = t24wifi.openPort(0, 0)
        if (port == 0.toByte()) {
            // Port opened successfully
        } else {
            throw Exception("Failed to open port: $port")
        }
    }

    fun close() {
        t24wifi.closePort()
    }

    fun wakeAll() {
        t24wifi.wakeAll(baseStation)
    }

    fun sleepAll() {
        t24wifi.sleepAll()
    }

    fun registerDataCallback(callback: DataCallback) {
        t24wifi.registerCallbackDP(callback)
    }

    fun registerWakeUpCallback(callback: WakeUpCallback) {
        t24wifi.registerCallbackWR(callback)
    }

    fun unregisterCallbacks() {
        t24wifi.registerCallbackDP(0)
        t24wifi.registerCallbackWR(0)
        t24wifi.registerCallbackSA(0)
        t24wifi.registerCallbackSAF(0)
    }

    fun pair(baseStation: Byte, useRemoteSettings: Byte, configMode: Byte, duration: Byte): Pair<Int, Int> {
        val id = IntArray(1)
        val dataTag = IntArray(1)
        val result = t24wifi.pair(baseStation, useRemoteSettings, configMode, duration, id, dataTag)
        if (result == 0.toByte()) {
            this.id = id[0]
            this.dataTag = dataTag[0]
            this.baseStation = baseStation
            return Pair(id[0], dataTag[0])
        } else {
            throw Exception("Pairing failed: $result")
        }
    }

    fun pairAsync(baseStation: Byte, useRemoteSettings: Byte, configMode: Byte, duration: Byte): Pair<Int, Int> {
        t24wifi.pairAsync(baseStation, useRemoteSettings, configMode, duration)
        val id = IntArray(1)
        val dataTag = IntArray(1)
        var result: Byte
        do {
            result = t24wifi.pairAsyncPoll(id, dataTag)
        } while (result == 99.toByte())
        if (result == 0.toByte()) {
            this.id = id[0]
            this.dataTag = dataTag[0]
            this.baseStation = baseStation

            return Pair(id[0], dataTag[0])
        } else {
            throw Exception("Async pairing failed: $result")
        }
    }

    fun softPair(baseStation: Byte, id: Int, dataTag: Int): Byte {
        return t24wifi.softPair(baseStation, id, dataTag)
    }

    fun getBaseStationInfo(baseStation: Byte): Map<String, Any> {
        val info = mutableMapOf<String, Any>()
        if (t24wifi.baseStationExists(baseStation) > 0) {
            info["GroupKey"] = t24wifi.baseStationKeyExists(baseStation)
            info["RFChannel"] = t24wifi.baseStationChannel(baseStation)
            info["ID"] = t24wifi.baseStationId(baseStation)
            info["FirmwareRevision"] = t24wifi.baseStationRevision(baseStation)
        } else {
            throw Exception("Base station not detected!")
        }
        return info
    }

    fun commandResume(): Byte {
        return t24wifi.commandResume(baseStation, id)
    }

    fun getDataProviderInfo(): Map<String, Any> {
        val baseStation = ByteArray(1)
        val value = FloatArray(1)
        val status = ByteArray(1)
        val rssi = ByteArray(1)
        val cv = ByteArray(1)
        val error = ByteArray(1)
        val lowBatt = ByteArray(1)
        val msInterval = IntArray(1)
        val msLastReceived = IntArray(1)
        val result = t24wifi.dataProviderInfo(
            dataTag,
            baseStation,
            value,
            status,
            rssi,
            cv,
            error,
            lowBatt,
            msInterval,
            msLastReceived
        )
        if (result > 0) {
            return mapOf(
                "BaseStation" to baseStation[0],
                "Value" to value[0],
                "Status" to status[0],
                "RSSI" to rssi[0],
                "CV" to cv[0],
                "Error" to error[0],
                "LowBatt" to lowBatt[0],
                "Interval" to msInterval[0],
                "LastReceived" to msLastReceived[0]
            )
        } else {
            throw Exception("No data provider info found for data tag: $dataTag")
        }
    }

    fun readLoadCellValue(): Float {
        val baseStation = ByteArray(1)
        val value = FloatArray(1)
        val status = ByteArray(1)
        val rssi = ByteArray(1)
        val cv = ByteArray(1)
        val error = ByteArray(1)
        val lowBatt = ByteArray(1)
        val msInterval = IntArray(1)
        val msLastReceived = IntArray(1)
        val result = t24wifi.dataProviderInfo(
            dataTag,
            baseStation,
            value,
            status,
            rssi,
            cv,
            error,
            lowBatt,
            msInterval,
            msLastReceived
        )
        if (result > 0) {
            return value[0]
        } else {
            throw Exception("Failed to read load cell value")
        }
    }
}