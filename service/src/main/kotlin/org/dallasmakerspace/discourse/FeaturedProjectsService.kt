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
  private val topicLookbackDays = 90L
  private val postLookbackDays = 30L

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val cachedProjects = AtomicReference<List<FeaturedProject>>(emptyList())
  private val lastFetchTime = AtomicReference<Instant?>(null)
  private val isRefreshing = AtomicBoolean(false)

  // Capture each full <img ...> tag so we can inspect width and src together
  private val imgTagRegex = Regex("""<img([^>]+)>""")
  private val srcAttrRegex = Regex("""src="([^"]+)"""")
  private val largeSrcAttrRegex = Regex("""data-large-src="([^"]+)"""")
  private val widthAttrRegex = Regex("""width="(\d+)"""")

  fun getFeaturedProjects(): List<FeaturedProject> {
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
    return cachedProjects.get()
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

  @Suppress("TooGenericExceptionCaught")
  private suspend fun doRefresh() {
    try {
      log.info("Refreshing featured projects cache")

      val now = Clock.System.now()
      val topicCutoff = now.minus(topicLookbackDays.days)
      val postCutoff = now.minus(postLookbackDays.days)

      // Fetch category page 0 to find recent "Show and Tell" monthly topics
      val categoryResponse =
          discourseApiClient.getCategoryTopics(
              SHOW_AND_TELL_CATEGORY_SLUG, SHOW_AND_TELL_CATEGORY_ID)
      val recentTopics =
          categoryResponse.topicList.topics.filter { topic ->
            topic.title.startsWith("Show and Tell") &&
                runCatching { Instant.parse(topic.createdAt) }
                    .getOrNull()
                    ?.let { it >= topicCutoff } == true
          }

      if (recentTopics.isEmpty()) {
        log.info("No recent Show and Tell topics found")
        return
      }

      log.debug("Found ${recentTopics.size} recent Show and Tell topics")

      // Fetch posts for all matching topics in parallel
      val allPosts = coroutineScope {
        recentTopics
            .map { topic ->
              async {
                runCatching { discourseApiClient.getTopicPosts(topic.id) }
                    .onFailure { log.warn("Failed to fetch posts for topic ${topic.id}", it) }
                    .getOrNull()
                    ?.let { details -> topic to details }
              }
            }
            .mapNotNull { it.await() }
      }

      // Collect qualifying posts
      val candidatePosts =
          allPosts.flatMap { (topic, details) ->
            details.postStream.posts
                .filter { post ->
                  // Exclude intro post (post_number == 1)
                  post.postNumber != 1 &&
                      // Only posts within last 30 days
                      runCatching { Instant.parse(post.createdAt) }
                          .getOrNull()
                          ?.let { it >= postCutoff } == true
                }
                .mapNotNull { post ->
                  val imageUrl = extractLargeImageUrl(post.cooked) ?: return@mapNotNull null
                  Triple(topic, post, imageUrl)
                }
          }

      if (candidatePosts.isEmpty()) {
        log.info("No image-containing posts found in recent Show and Tell topics")
        return
      }

      // Look up all poster usernames at once
      val discourseUsernames = candidatePosts.map { (_, post, _) -> post.username }.distinct()
      val membersByDiscourseUsername =
          memberRepository.getMembersByDiscourseUsernames(discourseUsernames)

      // Keep only posts from linked DMS members, sort by likes, take top N
      val featuredProjects =
          candidatePosts
              .mapNotNull { (topic, post, imageUrl) ->
                val member = membersByDiscourseUsername[post.username] ?: return@mapNotNull null
                FeaturedProject(
                    topicId = topic.id,
                    postId = post.id,
                    title = topic.title,
                    imageUrl = imageUrl,
                    memberUsername = member.username,
                    memberDisplayName = member.displayName,
                    memberAvatarUrl = member.discourseAvatarUrl
                        ?.let { url ->
                          val base = if (url.startsWith("//")) "https:$url" else
                            if (url.startsWith("/")) "https://talk.dallasmakerspace.org$url" else url
                          base.replace("{size}", "40")
                        },
                    likeCount = post.resolvedLikeCount,
                    discourseTopicUrl =
                        "https://talk.dallasmakerspace.org/t/${topic.slug}/${topic.id}/${post.postNumber}",
                )
              }
              .sortedByDescending { it.likeCount }
              .take(MAX_FEATURED_PROJECTS)

      cachedProjects.set(featuredProjects)
      lastFetchTime.set(Clock.System.now())
      log.info("Featured projects cache updated: ${featuredProjects.size} projects")
    } catch (e: Exception) {
      log.error("Failed to refresh featured projects cache; keeping stale data", e)
    }
  }
}
