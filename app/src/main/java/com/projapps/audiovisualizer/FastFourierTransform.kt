package com.projapps.audiovisualizer

class FastFourierTransform private constructor(n:Int) {

    private var n:Int = 0
    private var m:Int = 0

    // Lookup tables. Only need to recompute when size of FFT changes.
    private var cos:DoubleArray = DoubleArray(0)
    private var sin:DoubleArray = DoubleArray(0)

    init {
        initialise(n)
    }

    private fun initialise(n:Int) {
        this.n = n
        this.m = (Math.log(n.toDouble()) / Math.log(2.0)).toInt()

        // Make sure n is a power of 2
        if (n != (1 shl m)) {
            throw RuntimeException("FFT length must be power of 2")
        }

        // Pre-compute tables
        cos = DoubleArray(n / 2)
        sin = DoubleArray(n / 2)

        for (i in 0 until n / 2) {
            cos[i] = Math.cos(-2.0 * Math.PI * i.toDouble() / n)
            sin[i] = Math.sin(-2.0 * Math.PI * i.toDouble() / n)
        }
    }

    fun applyFFT(x:DoubleArray, y:DoubleArray) {
        var k:Int
        var n1:Int
        var a:Int
        var c:Double
        var s:Double
        var t1:Double
        var t2:Double

        // Bit-reverse
        var j1 = 0
        var n2 = n / 2
        for (i in 1 until n - 1) {
            n1 = n2
            while (j1 >= n1) {
                j1 -= n1
                n1 /= 2
            }
            j1 += n1
            if (i < j1) {
                t1 = x[i]
                x[i] = x[j1]
                x[j1] = t1
                t1 = y[i]
                y[i] = y[j1]
                y[j1] = t1
            }
        }

        // FFT
        n2 = 1

        for (i in 0 until m) {
            n1 = n2
            n2 += n2
            a = 0

            for (j in 0 until n1) {
                c = cos[a]
                s = sin[a]
                a += 1 shl (m - i - 1)
                k = j
                while (k < n) {
                    t1 = c * x[k + n1] - s * y[k + n1]
                    t2 = s * x[k + n1] + c * y[k + n1]
                    x[k + n1] = x[k] - t1
                    y[k + n1] = y[k] - t2
                    x[k] = x[k] + t1
                    y[k] = y[k] + t2
                    k += n2
                }
            }
        }
    }

    companion object {
        private var instance:FastFourierTransform? = null

        @Synchronized
        fun getInstance(n:Int):FastFourierTransform {
            if (instance == null) {
                instance = FastFourierTransform(n)
            } else {
                instance?.let {
                    if (it.n != n) {
                        it.initialise(n)
                    }
                }
            }
            return instance!!
        }
    }
}
