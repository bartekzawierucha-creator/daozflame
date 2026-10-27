package com.example.game.net

import com.example.game.engine.DaoNet
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Etap 9: klient Sieci Dao (Supabase: PostgREST + Edge Functions + anonimowe logowanie).
 * Wywołania blokujące – ViewModel woła je na Dispatchers.IO. Bez skonfigurowanego adresu klient jest wyłączony
 * i gra używa przeciwników offline ([DaoNet.offlineOpponents]).
 */
class DaoNetClient(
    private val baseUrl: String,
    private val anonKey: String,
    private val tokenStore: TokenStore
) {
    interface TokenStore {
        var accessToken: String?
        var userId: String?
    }

    data class RankingRow(val id: String = "", val sect_name: String = "", val rating: Int = 0, val place: Int = 0)
    data class CloudRow(val payload: String = "", val game_day: Int = 0, val era: Int = 1, val top_realm: String = "", val saved_at: String = "")
    data class VerifyResult(val attackerRating: Int = 0, val defenderRating: Int = 0)

    private data class ShadowRow(val id: String = "", val sect_name: String = "", val rating: Int = 0, val era: Int = 1, val team: DaoNet.ShadowTeam = DaoNet.ShadowTeam())

    val enabled: Boolean get() = baseUrl.isNotBlank() && anonKey.isNotBlank()

    private val http = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val json = "application/json".toMediaType()

    private fun request(path: String) = Request.Builder()
        .url(baseUrl.trimEnd('/') + path)
        .header("apikey", anonKey)
        .header("Authorization", "Bearer " + (tokenStore.accessToken ?: anonKey))

    private fun <T> call(req: Request, parse: (String) -> T): T {
        http.newCall(req).execute().use { r ->
            val body = r.body?.string().orEmpty()
            check(r.isSuccessful) { "Sieć Dao: ${r.code} $body" }
            return parse(body)
        }
    }

    /** Anonimowe konto gracza (bez e-maila); token zapamiętany na urządzeniu. */
    fun signIn() {
        if (tokenStore.accessToken != null) return
        val req = request("/auth/v1/signup").post("{}".toRequestBody(json)).build()
        call(req) { body ->
            @Suppress("UNCHECKED_CAST")
            val m = moshi.adapter(Map::class.java).fromJson(body) as Map<String, Any?>
            tokenStore.accessToken = m["access_token"] as String?
            @Suppress("UNCHECKED_CAST")
            tokenStore.userId = (m["user"] as Map<String, Any?>?)?.get("id") as String?
        }
    }

    fun uploadShadow(team: DaoNet.ShadowTeam, season: Long): String {
        signIn()
        val body = mapOf("owner" to tokenStore.userId, "sect_name" to team.sectName, "season" to season, "era" to team.era, "team" to team)
        val req = request("/rest/v1/shadows?on_conflict=owner,season")
            .header("Prefer", "resolution=merge-duplicates,return=representation")
            .post(moshi.adapter(Any::class.java).toJson(body).toRequestBody(json)).build()
        return call(req) { rows(it).first().id }
    }

    fun opponents(season: Long, rating: Int): List<DaoNet.ShadowTeam> {
        val req = request("/rest/v1/shadows?season=eq.$season&rating=gte.${rating - 250}&rating=lte.${rating + 250}&owner=neq.${tokenStore.userId}&limit=10").get().build()
        return call(req) { rows(it).map { r -> r.team.copy(id = r.id, rating = r.rating, sectName = r.sect_name) } }
    }

    /** Serwer odtwarza walkę z ziarna i dopiero wtedy zapisuje rankingi. */
    fun verifyDuel(attackerId: String, defenderId: String, seed: Long): VerifyResult {
        val body = """{"attackerId":"$attackerId","defenderId":"$defenderId","seed":"$seed"}"""
        val req = request("/functions/v1/verify-duel").post(body.toRequestBody(json)).build()
        return call(req) { moshi.adapter(VerifyResult::class.java).fromJson(it)!! }
    }

    fun ranking(season: Long): List<RankingRow> {
        val req = request("/rest/v1/season_ranking?season=eq.$season&order=rating.desc&limit=50").get().build()
        val type = Types.newParameterizedType(List::class.java, RankingRow::class.java)
        return call(req) { moshi.adapter<List<RankingRow>>(type).fromJson(it).orEmpty() }
    }

    fun uploadSave(payload: String, day: Int, era: Int, topRealm: String) {
        signIn()
        val body = mapOf("owner" to tokenStore.userId, "payload" to payload, "game_day" to day, "era" to era, "top_realm" to topRealm)
        val req = request("/rest/v1/cloud_saves?on_conflict=owner").header("Prefer", "resolution=merge-duplicates")
            .post(moshi.adapter(Any::class.java).toJson(body).toRequestBody(json)).build()
        call(req) { }
    }

    fun downloadSave(): CloudRow? {
        signIn()
        val req = request("/rest/v1/cloud_saves?owner=eq.${tokenStore.userId}&limit=1").get().build()
        val type = Types.newParameterizedType(List::class.java, CloudRow::class.java)
        return call(req) { moshi.adapter<List<CloudRow>>(type).fromJson(it).orEmpty().firstOrNull() }
    }

    private fun rows(body: String): List<ShadowRow> {
        val type = Types.newParameterizedType(List::class.java, ShadowRow::class.java)
        return moshi.adapter<List<ShadowRow>>(type).fromJson(body).orEmpty()
    }
}
