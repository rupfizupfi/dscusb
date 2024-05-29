package com.glowingblue.intellij

class Measurement(private var force: Float, private var timestamp: Long) {
    companion object {
        fun fromPair(pair: Pair<Float, Long>): Measurement {
            return Measurement(pair.first, pair.second);
        }
    }

    fun getForce(): Float {
        return this.force
    }

    fun getTimestamp(): Long {
        return this.timestamp
    }

    override fun toString(): String {
        return "Measurement: ${this.force}@${this.timestamp}"
    }
}