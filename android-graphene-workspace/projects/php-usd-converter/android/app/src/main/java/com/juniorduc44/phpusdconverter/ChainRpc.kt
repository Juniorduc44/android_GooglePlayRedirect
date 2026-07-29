package com.juniorduc44.phpusdconverter

import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL

object ChainRpc {
    const val RH_RPC = "https://rpc.mainnet.chain.robinhood.com"
    const val CHAIN_ID = 4663

    fun ethChainId(rpc: String = RH_RPC, timeoutMs: Int = 12_000): Int {
        val result = rpcCall("eth_chainId", emptyList(), rpc, timeoutMs)
        return result.removePrefix("0x").toInt(16)
    }

    fun ethGetBalanceEth(address: String, rpc: String = RH_RPC, timeoutMs: Int = 12_000): BigDecimal {
        val result = rpcCall("eth_getBalance", listOf(address, "latest"), rpc, timeoutMs)
        val wei = BigInteger(result.removePrefix("0x"), 16)
        return BigDecimal(wei).divide(BigDecimal.TEN.pow(18), 6, RoundingMode.HALF_UP)
    }

    private fun rpcCall(
        method: String,
        params: List<Any>,
        rpc: String,
        timeoutMs: Int,
    ): String {
        val body = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", method)
            .put("params", org.json.JSONArray(params))
            .toString()
        val conn = (URL(rpc).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "php-usd-converter-android-wallet/1.7")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("HTTP $code: ${text.take(160)}")
            val json = JSONObject(text)
            if (json.has("error") && !json.isNull("error")) {
                throw IllegalStateException(json.get("error").toString())
            }
            return json.getString("result")
        } finally {
            conn.disconnect()
        }
    }
}
