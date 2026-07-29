package com.juniorduc44.phpusdconverter

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Robinhood Chain (4663) market data via DexScreener public API.
 * Defensive: every network path catches exceptions and returns structured results.
 */
object RobinhoodChainTracker {

    const val CHAIN_ID = 4663
    const val DEXSCREENER_SLUG = "robinhood"
    const val NETWORK_NAME = "Robinhood Chain"
    const val RPC_PUBLIC = "https://rpc.mainnet.chain.robinhood.com"
    const val EXPLORER = "https://robinhoodchain.blockscout.com"

    data class RwaDefault(val symbol: String, val name: String, val address: String)

    data class AssetQuote(
        val symbol: String,
        val name: String,
        val address: String,
        val category: String,
        val priceUsd: Double? = null,
        val changeH24: Double? = null,
        val volumeH24: Double = 0.0,
        val liquidityUsd: Double = 0.0,
        val pairAddress: String = "",
        val dexId: String = "",
        val url: String = "",
        val error: String? = null,
    ) {
        fun formatPrice(): String {
            val p = priceUsd ?: return if (error != null) "—" else "n/a"
            return when {
                p >= 100 -> String.format(Locale.US, "$%,.2f", p)
                p >= 1 -> String.format(Locale.US, "$%,.4f", p)
                p >= 0.0001 -> String.format(Locale.US, "$%.6f", p)
                else -> String.format(Locale.US, "$%.8f", p)
            }
        }

        fun formatChange(): String {
            val c = changeH24 ?: return "—"
            val sign = if (c >= 0) "+" else ""
            return String.format(Locale.US, "%s%.2f%%", sign, c)
        }

        fun shortContract(): String {
            if (address.length < 12) return address
            return address.take(8) + "…" + address.takeLast(6)
        }
    }

    data class SelfTestResult(val name: String, val ok: Boolean, val detail: String)

    data class Snapshot(
        val rwa: List<AssetQuote>,
        val memes: List<AssetQuote>,
        val custom: List<AssetQuote>,
        val status: String,
        val ok: Boolean,
    )

    /** Verified DexScreener stock-token style assets on robinhood (2026-07-29). */
    val DEFAULT_RWA: List<RwaDefault> = listOf(
        RwaDefault("NVDA", "NVIDIA Stock Token", "0xd0601CE157Db5bdC3162BbaC2a2C8aF5320D9EEC"),
        RwaDefault("TSLA", "Tesla Stock Token", "0x322F0929c4625eD5bAd873c95208D54E1c003b2d"),
        RwaDefault("AAPL", "Apple Stock Token", "0xaF3D76f1834A1d425780943C99Ea8A608f8a93f9"),
        RwaDefault("GOOGL", "Alphabet Stock Token", "0x2e0847E8910a9732eB3fb1bb4b70a580ADAD4FE3"),
        RwaDefault("MSFT", "Microsoft Stock Token", "0xe93237C50D904957Cf27E7B1133b510C669c2e74"),
    )

    private val memeExclude = setOf(
        "USDG", "WETH", "ETH", "USDC", "USDT", "DAI",
        "NVDA", "TSLA", "AAPL", "GOOGL", "MSFT", "AMZN", "META", "HOOD",
    )

    private val memeSeeds = listOf(
        "robinhood", "meme", "pepe", "doge", "cat", "hood", "mars", "space",
        "frog", "inu", "ai", "coin", "token", "elon",
    )

    private val customAddresses = ConcurrentHashMap.newKeySet<String>()

    fun networkMetaLine(): String =
        "$NETWORK_NAME · chain $CHAIN_ID · gas ETH\nRPC $RPC_PUBLIC\n$EXPLORER"

    fun isValidEvmAddress(addr: String): Boolean =
        Regex("^0x[a-fA-F0-9]{40}$").matches(addr.trim())

    fun addCustom(address: String): String {
        val a = address.trim()
        if (!isValidEvmAddress(a)) {
            throw IllegalArgumentException("Contract must be a 0x… 40-hex EVM address")
        }
        customAddresses.add(a)
        return a
    }

    fun clearCustom() {
        customAddresses.clear()
    }

    fun listCustom(): List<String> = customAddresses.toList()

    fun fetchSnapshot(memeLimit: Int = 10): Snapshot {
        val rwa = try {
            fetchRwa()
        } catch (e: Exception) {
            listOf(
                AssetQuote(
                    symbol = "ERR",
                    name = "RWA section",
                    address = "",
                    category = "rwa",
                    error = "${e.javaClass.simpleName}: ${e.message}",
                )
            )
        }
        val memes = try {
            fetchTopMemes(memeLimit)
        } catch (e: Exception) {
            listOf(
                AssetQuote(
                    symbol = "ERR",
                    name = "Meme section",
                    address = "",
                    category = "meme",
                    error = "${e.javaClass.simpleName}: ${e.message}",
                )
            )
        }
        val custom = try {
            fetchCustom()
        } catch (e: Exception) {
            listOf(
                AssetQuote(
                    symbol = "ERR",
                    name = "Custom section",
                    address = "",
                    category = "custom",
                    error = "${e.javaClass.simpleName}: ${e.message}",
                )
            )
        }
        val rwaOk = rwa.count { it.error == null && it.priceUsd != null }
        val memeOk = memes.count { it.error == null && it.address.isNotEmpty() }
        val ok = rwaOk >= 3 && memeOk >= 5
        val status = "RWA priced: $rwaOk/5 · memes: $memeOk · network $NETWORK_NAME"
        return Snapshot(rwa, memes, custom, status, ok)
    }

    fun fetchRwa(): List<AssetQuote> {
        val addrs = DEFAULT_RWA.map { it.address }
        val pairs = try {
            tokensByAddress(addrs)
        } catch (e: Exception) {
            return DEFAULT_RWA.map {
                AssetQuote(
                    symbol = it.symbol,
                    name = it.name,
                    address = it.address,
                    category = "rwa",
                    error = e.message ?: e.javaClass.simpleName,
                )
            }
        }
        val best = bestPairPerToken(pairs)
        return DEFAULT_RWA.map { def ->
            val pq = best[def.address.lowercase(Locale.US)]
            if (pq == null) {
                AssetQuote(
                    symbol = def.symbol,
                    name = def.name,
                    address = def.address,
                    category = "rwa",
                    error = "No DexScreener pair found",
                )
            } else {
                pq.copy(symbol = def.symbol, name = def.name, category = "rwa")
            }
        }
    }

    fun fetchTopMemes(limit: Int = 10): List<AssetQuote> {
        val all = mutableListOf<AssetQuote>()
        val errors = mutableListOf<String>()
        for (q in memeSeeds) {
            try {
                all.addAll(searchPairs(q))
            } catch (e: Exception) {
                errors.add("$q: ${e.message}")
            }
        }
        val best = bestPairPerToken(all)
        val candidates = best.values
            .filter { it.symbol.uppercase(Locale.US) !in memeExclude }
            .filter { it.symbol.uppercase(Locale.US) !in DEFAULT_RWA.map { d -> d.symbol } }
            .filterNot {
                val p = it.priceUsd
                it.symbol.uppercase(Locale.US).endsWith("USD") && p != null && p in 0.95..1.05
            }
            .sortedByDescending { it.volumeH24 }

        val out = mutableListOf<AssetQuote>()
        val seen = mutableSetOf<String>()
        for (pq in candidates) {
            val sym = pq.symbol.uppercase(Locale.US)
            if (sym in seen) continue
            seen.add(sym)
            out.add(pq.copy(category = "meme"))
            if (out.size >= limit) break
        }
        if (out.isEmpty() && errors.isNotEmpty()) {
            out.add(
                AssetQuote(
                    symbol = "—",
                    name = "Memecoin discovery failed",
                    address = "",
                    category = "meme",
                    error = errors.joinToString("; ").take(300),
                )
            )
        }
        return out
    }

    fun fetchCustom(): List<AssetQuote> {
        val addrs = listCustom()
        if (addrs.isEmpty()) return emptyList()
        val pairs = try {
            tokensByAddress(addrs)
        } catch (e: Exception) {
            return addrs.map {
                AssetQuote(
                    symbol = "?",
                    name = "Custom",
                    address = it,
                    category = "custom",
                    error = e.message ?: e.javaClass.simpleName,
                )
            }
        }
        val best = bestPairPerToken(pairs)
        return addrs.map { a ->
            val pq = best[a.lowercase(Locale.US)]
            pq?.copy(category = "custom")
                ?: AssetQuote(
                    symbol = "?",
                    name = "Unknown token",
                    address = a,
                    category = "custom",
                    error = "No pair on DexScreener for this chain",
                )
        }
    }

    fun runSelfTests(): List<SelfTestResult> {
        val results = mutableListOf<SelfTestResult>()

        fun run(name: String, block: () -> String) {
            try {
                val d = block()
                results.add(SelfTestResult(name, true, d))
            } catch (e: Exception) {
                results.add(SelfTestResult(name, false, "${e.javaClass.simpleName}: ${e.message}"))
            }
        }

        run("network constants") {
            require(CHAIN_ID == 4663)
            require(DEXSCREENER_SLUG == "robinhood")
            "chainId=$CHAIN_ID"
        }
        run("address validation") {
            require(isValidEvmAddress("0xd0601CE157Db5bdC3162BbaC2a2C8aF5320D9EEC"))
            require(!isValidEvmAddress("0x123"))
            "OK"
        }
        run("default RWA catalog") {
            require(DEFAULT_RWA.size >= 5)
            DEFAULT_RWA.forEach { require(isValidEvmAddress(it.address)) }
            DEFAULT_RWA.joinToString { it.symbol }
        }
        run("DexScreener RWA batch") {
            val rows = fetchRwa()
            val ok = rows.count { it.error == null && it.priceUsd != null }
            require(ok >= 3) { "only $ok priced: ${rows.map { it.error }}" }
            rows.filter { it.priceUsd != null }.joinToString { "${it.symbol}=${it.formatPrice()}" }
        }
        run("DexScreener top memes") {
            val rows = fetchTopMemes(10)
            val ok = rows.count { it.error == null && it.address.isNotEmpty() }
            require(ok >= 5) { "only $ok memes" }
            rows.take(3).joinToString { it.symbol }
        }
        run("fetchSnapshot") {
            val s = fetchSnapshot(10)
            require(s.rwa.size >= 5)
            "ok=${s.ok} ${s.status}"
        }
        return results
    }

    fun formatTable(rows: List<AssetQuote>, title: String): String {
        val sb = StringBuilder()
        sb.appendLine(title)
        if (rows.isEmpty()) {
            sb.appendLine("(empty)")
            return sb.toString()
        }
        for (a in rows) {
            if (a.error != null) {
                sb.appendLine(String.format(Locale.US, "%-10s ERROR %s", a.symbol, a.error.take(50)))
                if (a.address.isNotEmpty()) {
                    sb.appendLine(String.format(Locale.US, "%-10s %s", "", a.address))
                }
                continue
            }
            sb.appendLine(
                String.format(
                    Locale.US,
                    "%-10s %12s %8s vol=%,.0f",
                    a.symbol,
                    a.formatPrice(),
                    a.formatChange(),
                    a.volumeH24,
                )
            )
            sb.appendLine(
                String.format(
                    Locale.US,
                    "%-10s %s  %s",
                    "",
                    a.shortContract(),
                    a.address,
                )
            )
        }
        return sb.toString().trimEnd()
    }

    // --- HTTP / JSON ---

    private fun httpGet(url: String, timeoutMs: Int = 25_000): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            requestMethod = "GET"
            setRequestProperty("User-Agent", "php-usd-converter-android/1.6.1")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code: ${body.take(160)}")
            }
            return body
        } finally {
            conn.disconnect()
        }
    }

    private fun searchPairs(query: String): List<AssetQuote> {
        val q = java.net.URLEncoder.encode(query, Charsets.UTF_8.name())
        val body = httpGet("https://api.dexscreener.com/latest/dex/search?q=$q")
        val root = JSONObject(body)
        val pairs = root.optJSONArray("pairs") ?: return emptyList()
        return parsePairsArray(pairs)
    }

    private fun tokensByAddress(addresses: List<String>): List<AssetQuote> {
        val cleaned = addresses.map { it.trim() }.filter { isValidEvmAddress(it) }
        if (cleaned.isEmpty()) return emptyList()
        val joined = cleaned.joinToString(",")
        val body = httpGet("https://api.dexscreener.com/tokens/v1/$DEXSCREENER_SLUG/$joined")
        val arr = JSONArray(body)
        return parsePairsArray(arr)
    }

    private fun parsePairsArray(pairs: JSONArray): List<AssetQuote> {
        val out = mutableListOf<AssetQuote>()
        for (i in 0 until pairs.length()) {
            val p = pairs.optJSONObject(i) ?: continue
            if (p.optString("chainId") != DEXSCREENER_SLUG) continue
            val bt = p.optJSONObject("baseToken") ?: continue
            val vol = p.optJSONObject("volume")
            val liq = p.optJSONObject("liquidity")
            val chg = p.optJSONObject("priceChange")
            val priceStr = p.optString("priceUsd", "")
            val price = priceStr.toDoubleOrNull()
            out.add(
                AssetQuote(
                    symbol = bt.optString("symbol", "?"),
                    name = bt.optString("name", ""),
                    address = bt.optString("address", ""),
                    category = "raw",
                    priceUsd = price,
                    changeH24 = chg?.optDouble("h24")?.takeIf { !it.isNaN() },
                    volumeH24 = vol?.optDouble("h24")?.takeIf { !it.isNaN() } ?: 0.0,
                    liquidityUsd = liq?.optDouble("usd")?.takeIf { !it.isNaN() } ?: 0.0,
                    pairAddress = p.optString("pairAddress", ""),
                    dexId = p.optString("dexId", ""),
                    url = p.optString("url", ""),
                )
            )
        }
        return out
    }

    private fun bestPairPerToken(pairs: List<AssetQuote>): Map<String, AssetQuote> {
        val best = linkedMapOf<String, AssetQuote>()
        for (p in pairs) {
            val key = p.address.lowercase(Locale.US)
            if (key.isEmpty()) continue
            val prev = best[key]
            if (prev == null || p.liquidityUsd > prev.liquidityUsd) {
                best[key] = p
            }
        }
        return best
    }
}
