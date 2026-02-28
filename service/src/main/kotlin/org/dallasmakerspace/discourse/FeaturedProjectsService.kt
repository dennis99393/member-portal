package org.dallasmakerspace.discourse

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.members.MemberRepository
import org.dallasmakerspace.models.FeaturedProject

private const val SHOW_AND_TELL_CATEGORY_SLUG = "show-and-tell"
private const val SHOW_AND_TELL_CATEGORY_ID = 49
private const val MAX_FEATURED_PROJECTS = 10
private const val MIN_IMAGE_WIDTH = 300
private const val TOPIC_FETCH_DELAY_MS = 500L

@Singleton
class FeaturedProjectsService
@Inject
constructor(
    private val discourseApiClient: IDiscourseApiClient,
    private val memberRepository: MemberRepository,
    loggerFactory: LoggerFactory,
) {
  private val log = loggerFactory.create(javaClass)

  private val cacheDuration = 1.days
  private val topicLookbackDays = 730L // 2 years — covers all member photo history
  private val globalPostLookbackDays = 30L // window for home-page global top-10

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val cachedProjects = AtomicReference<List<FeaturedProject>>(emptyList())
  private val cachedMemberProjects = AtomicReference<Map<String, List<FeaturedProject>>>(emptyMap())
  private val lastFetchTime = AtomicReference<Instant?>(null)
  private val isRefreshing = AtomicBoolean(false)

  // Capture each full <img ...> tag so we can inspect width and src together
  private val imgTagRegex = Regex("""<img([^>]+)>""")
  private val srcAttrRegex = Regex("""src="([^"]+)"""")
  private val largeSrcAttrRegex = Regex("""data-large-src="([^"]+)"""")
  private val widthAttrRegex = Regex("""width="(\d+)"""")

  /** Returns the global top-10 featured projects (home page). Always instant — reads from cache. */
  fun getFeaturedProjects(): List<FeaturedProject> {
    triggerRefreshIfStale()
    return cachedProjects.get()
  }

  /**
   * Returns all featured projects for a specific DMS member (profile page). Always instant — reads
   * from the per-member cache populated by the same background refresh as [getFeaturedProjects].
   */
  fun getFeaturedProjectsForMember(dmsUsername: String): List<FeaturedProject> {
    triggerRefreshIfStale()
    return cachedMemberProjects.get()[dmsUsername] ?: emptyList()
  }

  private fun triggerRefreshIfStale() {
    val lastFetch = lastFetchTime.get()
    val isStale = lastFetch == null || (Clock.System.now() - lastFetch) >= cacheDuration
    if (isStale && isRefreshing.compareAndSet(false, true)) {
      serviceScope.launch {
        try {
          doRefresh()
        } finally {
          isRefreshing.set(false)
        }
      }
    }
  }

  /**
   * Finds the first large image URL from Discourse's rendered HTML. Prefers `data-large-src`
   * (full-resolution Discourse upload) over `src`. Skips images whose `width` attribute is below
   * [MIN_IMAGE_WIDTH] (filters out emojis, avatars, inline icons).
   */
  private fun extractLargeImageUrl(cooked: String): String? {
    for (match in imgTagRegex.findAll(cooked)) {
      val attrs = match.groupValues[1]
      val width = widthAttrRegex.find(attrs)?.groupValues?.get(1)?.toIntOrNull()
      if (width != null && width < MIN_IMAGE_WIDTH) continue
      val url =
          largeSrcAttrRegex.find(attrs)?.groupValues?.get(1)
              ?: srcAttrRegex.find(attrs)?.groupValues?.get(1)
              ?: continue
      if (url.isBlank()) continue
      return when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("/") -> "https://talk.dallasmakerspace.org$url"
        else -> url
      }
    }
    return null
  }

  private fun resolveAvatarUrl(raw: String?): String? =
      raw?.let { url ->
            when {
              url.startsWith("//") -> "https:$url"
              url.startsWith("/") -> "https://talk.dallasmakerspace.org$url"
              else -> url
            }
          }
          ?.replace("{size}", "40")

  /**
   * Single background pass that fetches 2 years of Show & Tell posts and populates both caches:
   * - [cachedProjects] — global top-[MAX_FEATURED_PROJECTS] from the last 30 days (home page)
   * - [cachedMemberProjects] — all image posts per member over 2 years (profile pages)
   */
  @Suppress("TooGenericExceptionCaught")
  private suspend fun doRefresh() {
    try {
      log.info("Refreshing featured projects cache (2-year window)")

      val now = Clock.System.now()
      val topicCutoff = now.minus(topicLookbackDays.days)
      val globalPostCutoff = now.minus(globalPostLookbackDays.days)

      // Fetch pages 0 and 1 in parallel — covers ~2 years of monthly Show & Tell topics
      val allTopics = coroutineScope {
        listOf(
                async {
                  runCatching {
                        discourseApiClient.getCategoryTopics(
                            SHOW_AND_TELL_CATEGORY_SLUG, SHOW_AND_TELL_CATEGORY_ID, page = 0)
                      }
                      .getOrNull()
                },
                async {
                  runCatching {
                        discourseApiClient.getCategoryTopics(
                            SHOW_AND_TELL_CATEGORY_SLUG, SHOW_AND_TELL_CATEGORY_ID, page = 1)
                      }
                      .getOrNull()
                },
            )
            .mapNotNull { it.await() }
            .flatMap { it.topicList.topics }
            .distinctBy { it.id }
      }

      val matchingTopics =
          allTopics.filter { topic ->
            topic.title.startsWith("Show and Tell") &&
                runCatching { Instant.parse(topic.createdAt) }
                    .getOrNull()
                    ?.let { it >= topicCutoff } == true
          }

      if (matchingTopics.isEmpty()) {
        log.info("No Show and Tell topics found in past 2 years")
        return
      }

      log.debug("Found ${matchingTopics.size} Show and Tell topics in past 2 years")

      // Fetch topic post streams sequentially with a delay to avoid Discourse 429 rate limiting
      val topicDetails = buildList {
        for ((index, topic) in matchingTopics.withIndex()) {
          if (index > 0) delay(TOPIC_FETCH_DELAY_MS)
          runCatching { discourseApiClient.getTopicPosts(topic.id) }
              .onFailure { log.warn("Failed to fetch posts for topic ${topic.id}", it) }
              .getOrNull()
              ?.let { details -> add(topic to details) }
        }
      }

      // Collect all image-containing posts (exclude intro post #1)
      val allCandidatePosts =
          topicDetails.flatMap { (topic, details) ->
            details.postStream.posts
                .filter { post -> post.postNumber != 1 }
                .mapNotNull { post ->
                  val imageUrl = extractLargeImageUrl(post.cooked) ?: return@mapNotNull null
                  Triple(topic, post, imageUrl)
                }
          }

      if (allCandidatePosts.isEmpty()) {
        log.info("No image-containing posts found in Show and Tell topics")
        return
      }

      // Batch-lookup all Discourse usernames → DMS members in one DB call
      val discourseUsernames = allCandidatePosts.map { (_, post, _) -> post.username }.distinct()
      val membersByDiscourseUsername =
          memberRepository.getMembersByDiscourseUsernames(discourseUsernames)

      // Build per-member map and global candidate list in a single pass
      val memberProjectsMap = mutableMapOf<String, MutableList<FeaturedProject>>()
      val globalCandidates = mutableListOf<FeaturedProject>()

      for ((topic, post, imageUrl) in allCandidatePosts) {
        val member = membersByDiscourseUsername[post.username] ?: continue
        val project =
            FeaturedProject(
                topicId = topic.id,
                postId = post.id,
                title = topic.title,
                imageUrl = imageUrl,
                memberUsername = member.username,
                memberDisplayName = member.displayName,
                memberAvatarUrl = resolveAvatarUrl(member.discourseAvatarUrl),
                likeCount = post.resolvedLikeCount,
                discourseTopicUrl =
                    "https://talk.dallasmakerspace.org/t/${topic.slug}/${topic.id}/${post.postNumber}",
                createdAt = post.createdAt,
            )

        memberProjectsMap.getOrPut(member.username) { mutableListOf() }.add(project)

        val postTime = runCatching { Instant.parse(post.createdAt) }.getOrNull()
        if (postTime != null && postTime >= globalPostCutoff) {
          globalCandidates.add(project)
        }
      }

      val newMemberProjects =
          memberProjectsMap.mapValues { (_, list) ->
            list.sortedByDescending { runCatching { Instant.parse(it.createdAt) }.getOrNull() }
          }
      val newGlobalProjects =
          globalCandidates.sortedByDescending { it.likeCount }.take(MAX_FEATURED_PROJECTS)

      cachedProjects.set(newGlobalProjects)
      cachedMemberProjects.set(newMemberProjects)
      lastFetchTime.set(Clock.System.now())
      log.info(
          "Featured projects cache updated: ${newGlobalProjects.size} global, " +
              "${newMemberProjects.size} members cached")
      val top5 =
          newMemberProjects.entries
              .sortedByDescending { it.value.size }
              .take(5)
              .joinToString(", ") { "${it.key}(${it.value.size})" }
      log.debug("Top 5 members by photo count: $top5")
    } catch (e: Exception) {
      log.error("Failed to refresh featured projects cache; keeping stale data", e)
    }
  }
}
