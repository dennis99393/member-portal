package org.dallasmakerspace.dataviz.reports.interlock

import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.json.JsonData
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.dallasmakerspace.activedirectory.ADUser
import org.dallasmakerspace.activedirectory.IActiveDirectoryService
import org.dallasmakerspace.core.logging.ElasticsearchClientManager
import org.dallasmakerspace.dataviz.DataVizReport
import org.dallasmakerspace.members.MemberService
import org.dallasmakerspace.models.DataField
import org.dallasmakerspace.models.DataItem
import org.dallasmakerspace.models.DataType
import org.dallasmakerspace.models.DataVizResponse

private const val RESCAN_DEBOUNCE_SECONDS = 30L

class InterlockUsageReport
@Inject
constructor(
    private val activeDirectoryService: IActiveDirectoryService,
    private val memberService: MemberService,
) : DataVizReport() {

  override fun getName(): String = "interlock-usage"

  override suspend fun getData(params: Map<String, List<String>>): DataVizResponse {
    val totalStartTime = System.currentTimeMillis()

    val interlockTag =
        params["tool"]?.firstOrNull()
            ?: return DataVizResponse(
                data = emptyList(),
                dataFields = emptyList(),
                metadata = mapOf("error" to "Missing required parameter: tool"),
            )

    val days = params["days"]?.firstOrNull()?.toIntOrNull() ?: 14

    // Step 1 — ES query
    val esStartTime = System.currentTimeMillis()
    val events = fetchElasticsearchEvents(interlockTag, days)
    val esTimeTaken = System.currentTimeMillis() - esStartTime

    // Step 2 — Session reconstruction
    val sessions = reconstructSessions(events)

    // Step 4 — Badge → Member lookup via AD
    val badgeNumbers = sessions.map { it.badge }.toSet()
    ElasticsearchClientManager.log.info("InterlockUsageReport: badge lookup for: $badgeNumbers")
    val memberLookupStartTime = System.currentTimeMillis()
    val badgeToUsername = fetchBadgeToUsername(badgeNumbers)
    ElasticsearchClientManager.log.info("InterlockUsageReport: resolved badges: $badgeToUsername")
    val usernames = badgeToUsername.values.toSet()
    val members =
        if (usernames.isNotEmpty()) {
          try {
            memberService.getMembersByUsernameList(usernames.toList())
          } catch (e: Exception) {
            ElasticsearchClientManager.log.warn(
                "InterlockUsageReport: member lookup failed: ${e.message}")
            emptyMap()
          }
        } else {
          emptyMap()
        }
    val memberLookupTimeTaken = System.currentTimeMillis() - memberLookupStartTime

    // Step 5 — Build DataVizResponse (most recent first)
    val dataItems =
        sessions.reversed().map { session ->
          val username = badgeToUsername[session.badge]
          val member = username?.let { members[it] }

          val memberJson: JsonElement =
              if (username != null) {
                buildJsonObject {
                  put("username", JsonPrimitive(username))
                  put("displayName", JsonPrimitive(member?.displayName ?: username))
                  put("avatarUrl", JsonPrimitive(getAvatarUrl(member?.discourseAvatarUrl)))
                }
              } else {
                JsonPrimitive("Badge: ${maskBadge(session.badge)}")
              }

          val authJson = buildJsonObject {
            put("variant", JsonPrimitive(if (session.authorized) "success" else "danger"))
            put("text", JsonPrimitive(if (session.authorized) "AUTHORIZED" else "DENIED"))
          }

          val duration = formatDuration(session.startTime, session.shutdownTime)

          val shutdownJson = buildJsonObject {
            when (session.shutdownType) {
              "IDLE_TIMEOUT" -> {
                put("variant", JsonPrimitive("secondary"))
                put("text", JsonPrimitive("IDLE TIMEOUT"))
              }
              "STARTUP_CURRENT" -> {
                put("variant", JsonPrimitive("danger"))
                put("text", JsonPrimitive("STARTUP CURRENT"))
                put("tooltip", JsonPrimitive("User error: start button pressed at the same time as badging in"))
              }
              "MANUAL_BADGE_OUT" -> {
                put("variant", JsonPrimitive("primary"))
                put("text", JsonPrimitive("BADGE OUT"))
              }
              else -> {
                put("variant", JsonPrimitive("light"))
                put("text", JsonPrimitive("RUNNING"))
              }
            }
          }

          DataItem(
              values =
                  mapOf(
                      "Time" to JsonPrimitive(session.startTime),
                      "Member" to memberJson,
                      "Auth" to authJson,
                      "Duration" to JsonPrimitive(duration),
                      "Shutdown" to shutdownJson,
                  ))
        }

    val dataFields =
        listOf(
            DataField(name = "Time", type = DataType.RELATIVE_DATE, label = "Time"),
            DataField(name = "Member", type = DataType.MEMBER, label = "Member"),
            DataField(name = "Auth", type = DataType.BADGE, label = "Auth"),
            DataField(name = "Duration", type = DataType.STRING, label = "Duration"),
            DataField(name = "Shutdown", type = DataType.BADGE, label = "Shutdown"),
        )

    val totalTimeTaken = System.currentTimeMillis() - totalStartTime
    val timeTakenInSec = String.format(Locale.US, "%.3f", totalTimeTaken / 1000f)

    val metadata =
        mapOf(
            "Generated at" to
                DateTimeFormatter.ISO_DATE_TIME.format(
                    Instant.now().atZone(java.time.ZoneId.of("America/Chicago"))),
            "Time taken" to "$timeTakenInSec sec",
            "ES query time" to "${esTimeTaken}ms",
            "Member lookup time" to "${memberLookupTimeTaken}ms",
            "Total sessions" to "${sessions.size}",
            "Tool" to interlockTag,
        )

    return DataVizResponse(data = dataItems, dataFields = dataFields, metadata = metadata)
  }

  // ---------------------------------------------------------------------------
  // Data class for raw ES document
  // ---------------------------------------------------------------------------

  private data class EsEvent(
      val timestamp: String,
      val message: String,
  )

  // ---------------------------------------------------------------------------
  // Data class for a reconstructed session
  // ---------------------------------------------------------------------------

  private data class SessionInfo(
      val startTime: String,
      val badge: String,
      val authorized: Boolean,
      var shutdownType: String?,
      var shutdownTime: String?,
  )

  // ---------------------------------------------------------------------------
  // Step 1 — Elasticsearch fetch
  // ---------------------------------------------------------------------------

  @Suppress("TooGenericExceptionCaught")
  private fun fetchElasticsearchEvents(interlockTag: String, days: Int): List<EsEvent> {
    val client = ElasticsearchClientManager.client

    val termQuery =
        Query.Builder().term { t -> t.field("host.keyword").value(interlockTag) }.build()

    val rangeQuery =
        Query.Builder()
            .range { r -> r.field("@timestamp").gte(JsonData.of("now-${days}d")) }
            .build()

    val boolQuery = Query.Builder().bool { b -> b.must(listOf(termQuery, rangeQuery)) }.build()

    val request =
        SearchRequest.Builder()
            .index("not-john-index")
            .query(boolQuery)
            .sort { s -> s.field { f -> f.field("@timestamp").order(SortOrder.Asc) } }
            .size(10_000)
            .source { src -> src.filter { f -> f.includes("@timestamp", "message") } }
            .build()

    return try {
      val response = client.search(request, Map::class.java)
      response.hits().hits().mapNotNull { hit ->
        val source = hit.source() ?: return@mapNotNull null
        @Suppress("UNCHECKED_CAST") val src = source as Map<String, Any?>
        val timestamp = src["@timestamp"] as? String ?: return@mapNotNull null
        val message = src["message"] as? String ?: return@mapNotNull null
        EsEvent(timestamp = timestamp, message = message)
      }
    } catch (e: Exception) {
      ElasticsearchClientManager.log.warn("InterlockUsageReport: ES query failed: ${e.message}")
      emptyList()
    }
  }

  // ---------------------------------------------------------------------------
  // Step 2 — Session reconstruction
  // ---------------------------------------------------------------------------

  private fun reconstructSessions(events: List<EsEvent>): List<SessionInfo> {
    val sessions = mutableListOf<SessionInfo>()
    var current: SessionInfo? = null

    for (doc in events) {
      // Messages are prefixed "YYYY-MM-DD HH:MM:SS <content>" — strip both date and time tokens.
      val msg = doc.message.substringAfter(" ").substringAfter(" ").ifBlank { continue }

      when {
        msg.startsWith("User:") -> {
          current?.let {
            // Only emit the interrupted session if it had meaningful duration.
            // Short gaps (< 30s) are re-scan noise from the same user trying again.
            val elapsedSeconds = try {
              Instant.parse(doc.timestamp).epochSecond - Instant.parse(it.startTime).epochSecond
            } catch (e: Exception) { Long.MAX_VALUE }
            if (elapsedSeconds >= RESCAN_DEBOUNCE_SECONDS) sessions.add(it)
          }
          val authorized = msg.contains("'authorized': True")
          val badge = Regex("'id': '(\\d+)'").find(msg)?.groupValues?.get(1) ?: continue
          current = SessionInfo(doc.timestamp, badge, authorized, null, null)
          if (!authorized) {
            sessions.add(current!!)
            current = null
          }
        }
        msg.contains("Idle timeout") -> {
          current?.let {
            it.shutdownType = "IDLE_TIMEOUT"
            it.shutdownTime = doc.timestamp
            sessions.add(it)
            current = null
          }
        }
        msg.contains("Startup Current Detected") -> {
          current?.let {
            it.shutdownType = "STARTUP_CURRENT"
            it.shutdownTime = doc.timestamp
            sessions.add(it)
            current = null
          }
        }
        msg.contains("Badged out while awaiting timeout") -> {
          current?.let {
            it.shutdownType = "MANUAL_BADGE_OUT"
            it.shutdownTime = doc.timestamp
            sessions.add(it)
            current = null
          }
        }
      }
    }
    current?.let { sessions.add(it) } // emit still-running session

    return sessions
  }

  // ---------------------------------------------------------------------------
  // Step 3 — Duration formatting
  // ---------------------------------------------------------------------------

  private fun formatDuration(startTime: String, shutdownTime: String?): String {
    shutdownTime ?: return "—"
    return try {
      val start = Instant.parse(startTime)
      val end = Instant.parse(shutdownTime)
      val totalSeconds = end.epochSecond - start.epochSecond
      if (totalSeconds < 0) return "—"
      val minutes = totalSeconds / 60
      val seconds = totalSeconds % 60
      "${minutes}m ${seconds}s"
    } catch (e: Exception) {
      "—"
    }
  }

  // ---------------------------------------------------------------------------
  // Step 4 — Badge → username lookup via Active Directory
  // ---------------------------------------------------------------------------

  @Suppress("TooGenericExceptionCaught", "UNCHECKED_CAST")
  private suspend fun fetchBadgeToUsername(badgeNumbers: Set<String>): Map<String, String> {
    if (badgeNumbers.isEmpty()) return emptyMap()
    return withContext(Dispatchers.IO) {
      try {
        val adResult =
            activeDirectoryService.getMembersByBadgeNumberList(badgeNumbers.toList())
                as? Map<String, ADUser?> ?: return@withContext emptyMap()
        adResult
            .mapNotNull { (badge, adUser) ->
              val username =
                  adUser?.sAMAccountName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
              badge to username
            }
            .toMap()
      } catch (e: Exception) {
        ElasticsearchClientManager.log.warn(
            "InterlockUsageReport: AD badge lookup failed: ${e.message}")
        emptyMap()
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Avatar URL helper (same as BadgeSwipesReport)
  // ---------------------------------------------------------------------------

  private fun maskBadge(badge: String): String {
    if (badge.length <= 4) return "*".repeat(badge.length)
    return badge.take(2) + "*".repeat(badge.length - 4) + badge.takeLast(2)
  }

  private fun getAvatarUrl(discourseAvatarUrl: String?): String {
    return discourseAvatarUrl
        ?.takeIf { it.isNotEmpty() }
        ?.let { url ->
          when {
            url.startsWith("//") -> "https:$url"
            else -> "https://talk.dallasmakerspace.org$url"
          }.replace("{size}", "144")
        } ?: ""
  }
}
