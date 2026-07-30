package com.juniorduc44.phpusdconverter

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Robinhood Chain (4663) market data via DexScreener public API.
 * Categories: top volume, boosts, momentum, memecoins, RWA, custom.
 */
object RobinhoodChainTracker {

    const val CHAIN_ID = 4663
    const val DEXSCREENER_SLUG = "robinhood"
    const val NETWORK_NAME = "Robinhood Chain"
    const val RPC_PUBLIC = "https://rpc.mainnet.chain.robinhood.com"
    const val EXPLORER = "https://robinhoodchain.blockscout.com"

    const val CAT_TOP_VOLUME = "top_volume"
    const val CAT_TRENDING_BOOSTS = "trending_boosts"
    const val CAT_TRENDING_MOMENTUM = "trending_momentum"
    const val CAT_MEMECOINS = "memecoins"
    const val CAT_RWA = "rwa"
    const val CAT_CUSTOM = "custom"
    const val DEFAULT_CATEGORY = CAT_TOP_VOLUME

    data class Category(val id: String, val label: String, val description: String)

    val CATEGORIES: List<Category> = listOf(
        Category(CAT_TOP_VOLUME, "Top 10 volume", "Highest 24h volume on Robinhood Chain"),
        Category(CAT_TRENDING_BOOSTS, "Trending · boosts", "DexScreener spotlight boosts"),
        Category(CAT_TRENDING_MOMENTUM, "Trending · momentum", "Biggest |24h %| movers"),
        Category(CAT_MEMECOINS, "Top 10 memecoins", "High-volume memes (no stables/stocks)"),
        Category(CAT_RWA, "RWA / stock tokens", "NVDA, TSLA, AAPL, GOOGL, MSFT"),
        Category(CAT_CUSTOM, "My contracts", "Contracts you added"),
    )

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
        val marketCap: Double? = null,
        val fdv: Double? = null,
        val estimatedSupply: Double? = null,
        val pairAddress: String = "",
        val dexId: String = "",
        val url: String = "",
        val error: String? = null,
        val note: String = "",
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

    private val volumeSeeds = listOf(
        "robinhood", "meme", "pepe", "doge", "cat", "hood", "mars", "space",
        "frog", "inu", "ai", "coin", "token", "elon",
        "NVDA", "TSLA", "AAPL", "USDG", "WETH", "stock", "ETH",
    )

    private val customAddresses = ConcurrentHashMap.newKeySet<String>()

    fun networkMetaLine(): String =
        "$NETWORK_NAME · chain $CHAIN_ID · gas ETH\nRPC $RPC_PUBLIC\n$EXPLORER"

    fun categoryLabels(): Array<String> = CATEGORIES.map { it.label }.toTypedArray()

    fun categoryIdForLabel(label: String): String =
        CATEGORIES.firstOrNull { it.label == label }?.id ?: DEFAULT_CATEGORY

    fun categoryLabelForId(id: String): String =
        CATEGORIES.firstOrNull { it.id == id }?.label
            ?: CATEGORIES.first { it.id == DEFAULT_CATEGORY }.label

    fun categoryDescription(id: String): String =
        CATEGORIES.firstOrNull { it.id == id }?.description ?: ""

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

    fun listCustom(): List<String> = customAddresses.toList()

    fun fetchCategory(categoryId: String, limit: Int = 10): List<AssetQuote> {
        return try {
            when (categoryId) {
                CAT_TOP_VOLUME -> fetchTopVolume(limit)
                CAT_TRENDING_BOOSTS -> fetchTrendingBoosts(limit)
                CAT_TRENDING_MOMENTUM -> fetchTrendingMomentum(limit)
                CAT_MEMECOINS -> fetchTopMemes(limit)
                CAT_RWA -> fetchRwa()
                CAT_CUSTOM -> fetchCustom()
                else -> fetchTopVolume(limit)
            }
        } catch (e: Exception) {
            listOf(
                AssetQuote(
                    symbol = "ERR",
                    name = categoryId,
                    address = "",
                    category = categoryId,
                    error = "${e.javaClass.simpleName}: ${e.message}",
                )
            )
        }
    }

    fun fetchRwa(): List<AssetQuote> {
        val addrs = DEFAULT_RWA.map { it.address }
        val pairs = try {
            tokensByAddress(addrs)
        } catch (e: Exception) {
            return DEFAULT_RWA.map {
                AssetQuote(it.symbol, it.name, it.address, CAT_RWA, error = e.message)
            }
        }
        val best = bestPairPerToken(pairs)
        return DEFAULT_RWA.map { def ->
            val pq = best[def.address.lowercase(Locale.US)]
            if (pq == null) {
                AssetQuote(def.symbol, def.name, def.address, CAT_RWA, error = "No pair found")
            } else {
                pq.copy(symbol = def.symbol, name = def.name, category = CAT_RWA)
            }
        }
    }

    fun fetchTopVolume(limit: Int = 10): List<AssetQuote> {
        val pairs = discoverPairs().sortedByDescending { it.volumeH24 }
        val out = mutableListOf<AssetQuote>()
        val seen = mutableSetOf<String>()
        for (p in pairs) {
            val key = p.address.lowercase(Locale.US)
            if (key.isEmpty() || key in seen) continue
            seen.add(key)
            out.add(p.copy(category = CAT_TOP_VOLUME))
            if (out.size >= limit) break
        }
        return out.ifEmpty {
            listOf(AssetQuote("—", "No volume data", "", CAT_TOP_VOLUME, error = "Empty"))
        }
    }

    fun fetchTrendingBoosts(limit: Int = 10): List<AssetQuote> {
        val boosts = mutableListOf<JSONObject>()
        try {
            boosts.addAll(tokenBoosts("top"))
            if (boosts.size < limit) {
                val seen = boosts.map { it.optString("tokenAddress").lowercase(Locale.US) }.toMutableSet()
                for (b in tokenBoosts("latest")) {
                    val a = b.optString("tokenAddress").lowercase(Locale.US)
                    if (a.isNotEmpty() && a !in seen) {
                        boosts.add(b)
                        seen.add(a)
                    }
                    if (boosts.size >= limit) break
                }
            }
        } catch (e: Exception) {
            return listOf(
                AssetQuote("ERR", "Boosts", "", CAT_TRENDING_BOOSTS, error = e.message)
            )
        }
        val addrs = boosts.mapNotNull {
            val a = it.optString("tokenAddress")
            if (isValidEvmAddress(a)) a else null
        }
        val quotes = try {
            bestPairPerToken(if (addrs.isEmpty()) emptyList() else tokensByAddress(addrs))
        } catch (_: Exception) {
            emptyMap()
        }
        val out = mutableListOf<AssetQuote>()
        for (b in boosts) {
            if (out.size >= limit) break
            val addr = b.optString("tokenAddress")
            val note = b.optString("description").take(100)
            val pq = quotes[addr.lowercase(Locale.US)]
            if (pq != null) {
                out.add(pq.copy(category = CAT_TRENDING_BOOSTS, note = note))
            } else {
                out.add(
                    AssetQuote(
                        "?", "Boosted token", addr, CAT_TRENDING_BOOSTS,
                        note = note, error = "No liquid pair quote yet",
                    )
                )
            }
        }
        return out.ifEmpty {
            listOf(AssetQuote("—", "No boosts", "", CAT_TRENDING_BOOSTS, error = "None right now"))
        }
    }

    fun fetchTrendingMomentum(limit: Int = 10): List<AssetQuote> {
        val minVol = 5_000.0
        val minLiq = 2_000.0
        val pairs = discoverPairs()
            .filter {
                it.volumeH24 >= minVol && it.liquidityUsd >= minLiq && it.changeH24 != null
            }
            .sortedByDescending { kotlin.math.abs(it.changeH24 ?: 0.0) }
        val out = mutableListOf<AssetQuote>()
        val seen = mutableSetOf<String>()
        for (p in pairs) {
            val key = p.address.lowercase(Locale.US)
            if (key.isEmpty() || key in seen) continue
            seen.add(key)
            out.add(p.copy(category = CAT_TRENDING_MOMENTUM))
            if (out.size >= limit) break
        }
        return out.ifEmpty {
            listOf(AssetQuote("—", "No movers", "", CAT_TRENDING_MOMENTUM, error = "Filters empty"))
        }
    }

    fun fetchTopMemes(limit: Int = 10): List<AssetQuote> {
        val all = mutableListOf<AssetQuote>()
        for (q in volumeSeeds.take(14)) {
            try {
                all.addAll(searchPairs(q))
            } catch (_: Exception) {
            }
        }
        val best = bestPairPerToken(all)
        val stock = DEFAULT_RWA.map { it.symbol }.toSet()
        val candidates = best.values
            .filter { it.symbol.uppercase(Locale.US) !in memeExclude }
            .filter { it.symbol.uppercase(Locale.US) !in stock }
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
            out.add(pq.copy(category = CAT_MEMECOINS))
            if (out.size >= limit) break
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
                AssetQuote("?", "Custom", it, CAT_CUSTOM, error = e.message)
            }
        }
        val best = bestPairPerToken(pairs)
        return addrs.map { a ->
            best[a.lowercase(Locale.US)]?.copy(category = CAT_CUSTOM)
                ?: AssetQuote("?", "Unknown", a, CAT_CUSTOM, error = "No pair")
        }
    }

    fun runSelfTests(): List<SelfTestResult> {
        val results = mutableListOf<SelfTestResult>()
        fun run(name: String, block: () -> String) {
            try {
                results.add(SelfTestResult(name, true, block()))
            } catch (e: Exception) {
                results.add(SelfTestResult(name, false, "${e.javaClass.simpleName}: ${e.message}"))
            }
        }
        run("network constants") {
            require(CHAIN_ID == 4663)
            "chainId=$CHAIN_ID"
        }
        run("categories") {
            require(CATEGORIES.size >= 5)
            CATEGORIES.joinToString { it.id }
        }
        run("RWA batch") {
            val rows = fetchRwa()
            val ok = rows.count { it.error == null && it.priceUsd != null }
            require(ok >= 3)
            "priced=$ok"
        }
        run("top volume") {
            val rows = fetchTopVolume(10)
            require(rows.any { it.priceUsd != null || it.volumeH24 > 0 })
            "n=${rows.size}"
        }
        run("trending boosts") {
            val rows = fetchTrendingBoosts(10)
            "n=${rows.size}"
        }
        run("category dispatch") {
            val r = fetchCategory(CAT_RWA, 5)
            require(r.size >= 5)
            "ok"
        }
        return results
    }

    // --- discovery / HTTP ---

    private fun discoverPairs(): List<AssetQuote> {
        val all = mutableListOf<AssetQuote>()
        for (q in volumeSeeds) {
            try {
                all.addAll(searchPairs(q))
            } catch (_: Exception) {
            }
        }
        return bestPairPerToken(all).values.toList()
    }

    private fun httpGet(url: String, timeoutMs: Int = 25_000): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            requestMethod = "GET"
            setRequestProperty("User-Agent", "php-usd-converter-android/1.6.2")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("HTTP $code: ${body.take(160)}")
            return body
        } finally {
            conn.disconnect()
        }
    }

    private fun tokenBoosts(which: String): List<JSONObject> {
        val path = if (which == "latest") "token-boosts/latest/v1" else "token-boosts/top/v1"
        val body = httpGet("https://api.dexscreener.com/$path")
        val arr = JSONArray(body)
        val out = mutableListOf<JSONObject>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("chainId") == DEXSCREENER_SLUG) out.add(o)
        }
        return out
    }

    private fun searchPairs(query: String): List<AssetQuote> {
        val q = java.net.URLEncoder.encode(query, Charsets.UTF_8.name())
        val body = httpGet("https://api.dexscreener.com/latest/dex/search?q=$q")
        val pairs = JSONObject(body).optJSONArray("pairs") ?: return emptyList()
        return parsePairsArray(pairs)
    }

    private fun tokensByAddress(addresses: List<String>): List<AssetQuote> {
        val cleaned = addresses.map { it.trim() }.filter { isValidEvmAddress(it) }
        if (cleaned.isEmpty()) return emptyList()
        val body = httpGet(
            "https://api.dexscreener.com/tokens/v1/$DEXSCREENER_SLUG/${cleaned.joinToString(",")}"
        )
        return parsePairsArray(JSONArray(body))
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
            val price = p.optString("priceUsd", "").toDoubleOrNull()
            val ch = if (chg != null && chg.has("h24") && !chg.isNull("h24")) {
                chg.optDouble("h24").takeIf { !it.isNaN() }
            } else null
            val mcap = if (p.has("marketCap") && !p.isNull("marketCap")) {
                p.optDouble("marketCap").takeIf { !it.isNaN() && it > 0 }
            } else null
            val fdv = if (p.has("fdv") && !p.isNull("fdv")) {
                p.optDouble("fdv").takeIf { !it.isNaN() && it > 0 }
            } else null
            val supply = estimateSupply(price, mcap, fdv)
            out.add(
                AssetQuote(
                    symbol = bt.optString("symbol", "?"),
                    name = bt.optString("name", ""),
                    address = bt.optString("address", ""),
                    category = "raw",
                    priceUsd = price,
                    changeH24 = ch,
                    volumeH24 = vol?.optDouble("h24")?.takeIf { !it.isNaN() } ?: 0.0,
                    liquidityUsd = liq?.optDouble("usd")?.takeIf { !it.isNaN() } ?: 0.0,
                    marketCap = mcap,
                    fdv = fdv,
                    estimatedSupply = supply,
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
            if (prev == null || p.liquidityUsd > prev.liquidityUsd) best[key] = p
        }
        return best
    }

    /** Circulating-ish supply ≈ mcap÷price (or fdv÷price) when Dex omits raw supply. */
    fun estimateSupply(price: Double?, marketCap: Double?, fdv: Double?): Double? {
        if (price == null || price <= 0) return null
        val cap = when {
            marketCap != null && marketCap > 0 -> marketCap
            fdv != null && fdv > 0 -> fdv
            else -> return null
        }
        return cap / price
    }
}
