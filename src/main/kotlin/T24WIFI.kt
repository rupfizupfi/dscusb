package ch.rupfizupfi.dscusb

import ch.rupfizupfi.dscusb.T24.DataCallback
import ch.rupfizupfi.dscusb.T24.WakeUpCallback
import jnr.ffi.LibraryLoader


class T24WIFI {
    private val library: T24AdvDrv64

    init {
        System.loadLibrary("T24AdvDrv64")
        library = LibraryLoader.create(T24AdvDrv64::class.java).load("T24AdvDrv64")
    }

    fun dllVersion(): Float = library.DLLVERSION()
    fun registerCallbackDP(iCallbackAddress: Long) = library.REGISTERCALLBACKDP(iCallbackAddress)
    fun registerCallbackWR(iCallbackAddress: Long) = library.REGISTERCALLBACKWR(iCallbackAddress)
    fun registerCallbackSA(iCallbackAddress: Long) = library.REGISTERCALLBACKSA(iCallbackAddress)
    fun registerCallbackSAF(iCallbackAddress: Long) = library.REGISTERCALLBACKSAF(iCallbackAddress)
    fun setCrossThreadCallback() = library.SETCROSSTHREADCALLBACK()
    fun openPort(comPort: Byte, baudrate: Int): Byte = library.OPENPORT(comPort, baudrate)
    fun closePort() = library.CLOSEPORT()
    fun writePacket(baseStation: Byte, buffer: ByteArray, length: Int) = library.WRITEPACKET(baseStation, buffer, length)
    fun sendDataProvider(baseStation: Byte, dataTag: Int, value: Float, status: Byte) = library.SENDDATAPROVIDER(baseStation, dataTag, value, status)
    fun softPair(baseStation: Byte, id: Int, dataTag: Int): Byte = library.SOFTPAIR(baseStation, id, dataTag)
    fun pair(baseStation: Byte, useRemoteSettings: Byte, configMode: Byte, duration: Byte, id: IntArray, dataTag: IntArray): Byte = library.PAIR(baseStation, useRemoteSettings, configMode, duration, id, dataTag)
    fun pairAsync(baseStation: Byte, useRemoteSettings: Byte, configMode: Byte, duration: Byte) = library.PAIRASYNC(baseStation, useRemoteSettings, configMode, duration)
    fun pairAsyncPoll(id: IntArray, dataTag: IntArray): Byte = library.PAIRASYNCPOLL(id, dataTag)
    fun baseStationRevision(baseStation: Byte): Float = library.BASESTATIONREVISION(baseStation)
    fun baseStationId(baseStation: Byte): Int = library.BASESTATIONID(baseStation)
    fun baseStationChannel(baseStation: Byte): Byte = library.BASESTATIONCHANNEL(baseStation)
    fun baseStationKeyExists(baseStation: Byte): Byte = library.BASESTATIONKEYEXISTS(baseStation)
    fun baseStationExists(baseStation: Byte): Byte = library.BASESTATIONEXISTS(baseStation)
    fun baseStationChannelSet(baseStation: Byte, channel: Byte): Byte = library.BASESTATIONCHANNELSET(baseStation, channel)
    fun wakeAll(baseStation: Byte) = library.WAKEALL(baseStation)
    fun sleepAll() = library.SLEEPALL()
    fun stayAwakeModeOn() = library.STAYAWAKEMODEON()
    fun stayAwakeModeOff() = library.STAYAWAKEMODEOFF()
    fun alwaysWakeModeOn(baseStation: Byte) = library.ALWAYSWAKEMODEON(baseStation)
    fun alwaysWakeModeOff(baseStation: Byte) = library.ALWAYSWAKEMODEOFF(baseStation)
    fun saModeOn(baseStation: Byte): Byte = library.SAMODEON(baseStation)
    fun saModeOff(baseStation: Byte): Byte = library.SAMODEOFF(baseStation)
    fun executeCommand(baseStation: Byte, id: Int, command: Byte): Byte = library.EXECUTECOMMAND(baseStation, id, command)
    fun commandSleep(baseStation: Byte, id: Int): Byte = library.COMMANDSLEEP(baseStation, id)
    fun commandResume(baseStation: Byte, id: Int): Byte = library.COMMANDRESUME(baseStation, id)
    fun commandReset(baseStation: Byte, id: Int): Byte = library.COMMANDRESET(baseStation, id)
    fun commandSave(baseStation: Byte, id: Int): Byte = library.COMMANDSAVE(baseStation, id)
    fun readParameter(baseStation: Byte, id: Int, command: Byte, buffer: ByteArray, unicode: Byte): Byte = library.READPARAMETER(baseStation, id, command, buffer, unicode)
    fun readModel(baseStation: Byte, id: Int, buffer: ByteArray, unicode: Byte): Byte = library.READMODEL(baseStation, id, buffer, unicode)
    fun writeParameter(baseStation: Byte, id: Int, command: Byte, buffer: ByteArray): Byte = library.WRITEPARAMETER(baseStation, id, command, buffer)
    fun dataProviderInfo(dataTag: Int, baseStation: ByteArray, value: FloatArray, status: ByteArray, rssi: ByteArray, cv: ByteArray, error: ByteArray, lowBatt: ByteArray, msInterval: IntArray, msLastReceived: IntArray): Byte = library.DATAPROVIDERINFO(dataTag, baseStation, value, status, rssi, cv, error, lowBatt, msInterval, msLastReceived)

    /**
     * Data Provider packets so that when packets arrive the hosting application receives a callback.
     */
    fun registerCallbackDP(callback: DataCallback) = library.REGISTERCALLBACKDP(callback)

    /**
     * Wake Request packets so that when packets arrive the hosting application receives a callback.
     */
    fun registerCallbackWR(callback: WakeUpCallback) = library.REGISTERCALLBACKWR(callback)

    /**
     *  Spectrum Analyser Callback
     */
    fun registerCallbackSA(callback: () -> Unit) = library.REGISTERCALLBACKSA(0)

    /**
     * Data Provider packets arriving from a T24-SAf (Fast) transmitter so that when
     * packets arrive the hosting application receives a callback.
     */
    fun registerCallbackSAF(callback: () -> Unit) = library.REGISTERCALLBACKSAF(0)
}