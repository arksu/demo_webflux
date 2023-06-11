package com.example.demowebflux.crypto

class Base58 {

    companion object {
        val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray()
        private val ENCODED_ZERO = ALPHABET[0]

        fun encode(i: ByteArray): String {
            var input = i
            if (input.isEmpty()) {
                return ""
            }
            // Count leading zeros.
            var zeros = 0
            while (zeros < input.size && input[zeros].toInt() == 0) {
                ++zeros
            }
            // Convert base-256 digits to base-58 digits (plus conversion to ASCII characters)
            input = input.copyOf(input.size) // since we modify it in-place
            val encoded = CharArray(input.size * 2) // upper bound
            var outputStart = encoded.size
            var inputStart = zeros
            while (inputStart < input.size) {
                encoded[--outputStart] = ALPHABET[divmod(input, inputStart, 256, 58).toInt()]
                if (input[inputStart].toInt() == 0) {
                    ++inputStart // optimization - skip leading zeros
                }
            }
            // Preserve exactly as many leading encoded zeros in output as there were leading zeros in input.
            while (outputStart < encoded.size && encoded[outputStart] == ENCODED_ZERO) {
                ++outputStart
            }
            while (--zeros >= 0) {
                encoded[--outputStart] = ENCODED_ZERO
            }
            // Return encoded string (including encoded leading zeros).
            return String(encoded, outputStart, encoded.size - outputStart)
        }

        private fun divmod(number: ByteArray, firstDigit: Int, base: Int, divisor: Int): Byte {
            // this is just long division which accounts for the base of the input digits
            var remainder = 0
            for (i in firstDigit until number.size) {
                val digit = number[i].toInt() and 0xFF
                val temp = remainder * base + digit
                number[i] = (temp / divisor).toByte()
                remainder = temp % divisor
            }
            return remainder.toByte()
        }
    }
}