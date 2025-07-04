package org.dallasmakerspace.cron

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.dallasmakerspace.activedirectory.ActiveDirectoryService
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.discourse.DiscourseAvatarService
import org.dallasmakerspace.members.MemberComparator
import org.dallasmakerspace.members.MemberRepository
import org.dallasmakerspace.members.MemberService
import org.dallasmakerspace.models.DMSMember
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.Logger

class MemberRefreshCronJobAvatarTest {

  private lateinit var mockLoggerFactory: LoggerFactory
  private lateinit var mockLogger: Logger
  private lateinit var mockMemberService: MemberService
  private lateinit var mockMemberComparator: MemberComparator
  private lateinit var mockActiveDirectoryService: ActiveDirectoryService
  private lateinit var mockDiscourseAvatarService: DiscourseAvatarService
  private lateinit var mockMemberRepository: MemberRepository
  private lateinit var memberRefreshCronJob: MemberRefreshCronJob

  @Before
  fun setUp() {
    mockLoggerFactory = mock()
    mockLogger = mock()
    mockMemberService = mock()
    mockMemberComparator = mock()
    mockActiveDirectoryService = mock()
    mockDiscourseAvatarService = mock()
    mockMemberRepository = mock()

    whenever(mockLoggerFactory.create(any<Class<*>>())).thenReturn(mockLogger)
    whenever(mockLoggerFactory.createInMemoryLogger(any<Class<*>>())).thenReturn(mockLogger)

    memberRefreshCronJob =
        MemberRefreshCronJob(
            mockLoggerFactory,
            mockMemberService,
            mockMemberComparator,
            mockActiveDirectoryService,
            mockDiscourseAvatarService,
            mockMemberRepository)
  }

  @Test
  fun `run should process avatar refresh when enabled and not in shadow mode`() {
    val params = MemberRefreshCronJobParams(isRunningInShadowMode = false, refreshAvatars = true)
    val testMembers =
        listOf(
            DMSMember(
                id = 1,
                username = "user1",
                displayName = "User One",
                enabled = true,
                discourseUsername = "user1_discourse"),
            DMSMember(
                id = 2,
                username = "user2",
                displayName = "User Two",
                enabled = true,
                discourseUsername = "user2_discourse"))

    runBlocking {
      // Setup mocks
      whenever(mockMemberService.getAllMembers()).thenReturn(testMembers)
      whenever(mockActiveDirectoryService.getMembersByUsernameList(any())).thenReturn(emptyMap())
      whenever(mockMemberRepository.getMembersWithDiscourseUsernames()).thenReturn(testMembers)
      whenever(mockDiscourseAvatarService.refreshAvatarUrl("user1_discourse"))
          .thenReturn("/user_avatar/talk.dallasmakerspace.org/user1/{size}/123_2.png")
      whenever(mockDiscourseAvatarService.refreshAvatarUrl("user2_discourse"))
          .thenReturn("/user_avatar/talk.dallasmakerspace.org/user2/{size}/456_2.png")
      whenever(mockMemberRepository.updateDiscourseAvatarUrl(any(), any())).thenReturn(true)

      // Run the job
      val result = memberRefreshCronJob.run(params)

      // Verify avatar service was called
      verify(mockDiscourseAvatarService).refreshAvatarUrl("user1_discourse")
      verify(mockDiscourseAvatarService).refreshAvatarUrl("user2_discourse")

      // Verify database updates
      verify(mockMemberRepository)
          .updateDiscourseAvatarUrl(
              "user1", "/user_avatar/talk.dallasmakerspace.org/user1/{size}/123_2.png")
      verify(mockMemberRepository)
          .updateDiscourseAvatarUrl(
              "user2", "/user_avatar/talk.dallasmakerspace.org/user2/{size}/456_2.png")

      // Verify logging
      verify(mockLogger).info(argThat { this.contains("Avatar refresh enabled") })
      verify(mockLogger).info(argThat { this.contains("Starting avatar refresh") })

      assertFalse(result.isNotEmpty())
    }
  }

  @Test
  fun `MemberRefreshCronJobParams should have correct default values`() {
    val defaultParams = MemberRefreshCronJobParams()

    assertTrue(defaultParams.isRunningInShadowMode)
    assertEquals(true, defaultParams.refreshAvatars)
  }

  @Test
  fun `MemberRefreshCronJobParams toString should include all parameters`() {
    val params = MemberRefreshCronJobParams(isRunningInShadowMode = false, refreshAvatars = true)
    val toString = params.toString()

    assertTrue(toString.contains("isRunningInShadowMode=false"))
    assertTrue(toString.contains("refreshAvatars=true"))
  }
}
