package ch.rupfizupfi.dscusb


import ch.rupfizupfi.dscusb.T24.DataCallback
import ch.rupfizupfi.dscusb.T24.WakeUpCallback
import jnr.ffi.annotations.In
import jnr.ffi.annotations.Out

interface T24AdvDrv64 {
    fun DLLVERSION(): Float
    fun REGISTERCALLBACKDP(iCallbackAddress: Long)
    fun REGISTERCALLBACKDP(callback: DataCallback)
    fun REGISTERCALLBACKWR(iCallbackAddress: Long)
    fun REGISTERCALLBACKWR(callback: WakeUpCallback)
    fun REGISTERCALLBACKSA(iCallbackAddress: Long)
    fun REGISTERCALLBACKSAF(iCallbackAddress: Long)
    fun SETCROSSTHREADCALLBACK()
    fun OPENPORT(ComPort: Byte, Baudrate: Int): Byte
    fun CLOSEPORT()
    fun WRITEPACKET(BaseStation: Byte, @In Buffer: ByteArray, Length: Int)
    fun SENDDATAPROVIDER(BaseStation: Byte, DataTag: Int, Value: Float, Status: Byte)
    fun SOFTPAIR(BaseStation: Byte, ID: Int, DataTag: Int): Byte
    fun PAIR(BaseStation: Byte, UseRemoteSettings: Byte, ConfigMode: Byte, Duration: Byte, @Out ID: IntArray, @Out DataTag: IntArray): Byte
    fun PAIRASYNC(BaseStation: Byte, UseRemoteSettings: Byte, ConfigMode: Byte, Duration: Byte)
    fun PAIRASYNCPOLL(@Out ID: IntArray, @Out DataTag: IntArray): Byte
    fun BASESTATIONREVISION(BaseStation: Byte): Float
    fun BASESTATIONID(BaseStation: Byte): Int
    fun BASESTATIONCHANNEL(BaseStation: Byte): Byte
    fun BASESTATIONKEYEXISTS(BaseStation: Byte): Byte
    fun BASESTATIONEXISTS(BaseStation: Byte): Byte
    fun BASESTATIONCHANNELSET(BaseStation: Byte, Channel: Byte): Byte
    fun WAKEALL(BaseStation: Byte)
    fun SLEEPALL()
    fun STAYAWAKEMODEON()
    fun STAYAWAKEMODEOFF()
    fun ALWAYSWAKEMODEON(BaseStation: Byte)
    fun ALWAYSWAKEMODEOFF(BaseStation: Byte)
    fun SAMODEON(BaseStation: Byte): Byte
    fun SAMODEOFF(BaseStation: Byte): Byte
    fun EXECUTECOMMAND(BaseStation: Byte, ID: Int, Command: Byte): Byte
    fun COMMANDSLEEP(BaseStation: Byte, ID: Int): Byte
    fun COMMANDRESUME(BaseStation: Byte, ID: Int): Byte
    fun COMMANDRESET(BaseStation: Byte, ID: Int): Byte
    fun COMMANDSAVE(BaseStation: Byte, ID: Int): Byte
    fun READPARAMETER(BaseStation: Byte, ID: Int, Command: Byte, @Out Buffer: ByteArray, Unicode: Byte): Byte
    fun READMODEL(BaseStation: Byte, ID: Int, @Out Buffer: ByteArray, Unicode: Byte): Byte
    fun WRITEPARAMETER(BaseStation: Byte, ID: Int, Command: Byte, @In Buffer: ByteArray): Byte
    fun DATAPROVIDERINFO(DataTag: Int, @Out BaseStation: ByteArray, @Out Value: FloatArray, @Out Status: ByteArray, @Out RSSI: ByteArray, @Out CV: ByteArray, @Out Error: ByteArray, @Out LowBatt: ByteArray, @Out msInterval: IntArray, @Out msLastReceived: IntArray): Byte
}