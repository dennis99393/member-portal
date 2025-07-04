package org.dallasmakerspace.cron

import kotlin.test.assertEquals
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
import org.mockito.kotlin.never
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

      assertTrue(result.isNotEmpty())
    }
  }

  @Test
  fun `run should skip avatar refresh when disabled`() {
    val params = MemberRefreshCronJobParams(isRunningInShadowMode = false, refreshAvatars = false)
    val testMembers =
        listOf(
            DMSMember(
                id = 1,
                username = "user1",
                displayName = "User One",
                enabled = true,
                discourseUsername = "user1_discourse"))

    runBlocking {
      // Setup mocks
      whenever(mockMemberService.getAllMembers()).thenReturn(testMembers)
      whenever(mockActiveDirectoryService.getMembersByUsernameList(any())).thenReturn(emptyMap())

      // Run the job
      val result = memberRefreshCronJob.run(params)

      // Verify avatar service was NOT called
      verify(mockDiscourseAvatarService, never()).refreshAvatarUrl(any())
      verify(mockMemberRepository, never()).updateDiscourseAvatarUrl(any(), any())
      verify(mockMemberRepository, never()).getMembersWithDiscourseUsernames()

      // Verify logging shows avatar refresh disabled
      verify(mockLogger).info(argThat { this.contains("Avatar refresh disabled") })

      assertTrue(result.isNotEmpty())
    }
  }

  @Test
  fun `run should simulate avatar refresh in shadow mode`() {
    val params = MemberRefreshCronJobParams(isRunningInShadowMode = true, refreshAvatars = true)
    val testMembers =
        listOf(
            DMSMember(
                id = 1,
                username = "user1",
                displayName = "User One",
                enabled = true,
                discourseUsername = "user1_discourse"))

    runBlocking {
      // Setup mocks
      whenever(mockMemberService.getAllMembers()).thenReturn(testMembers)
      whenever(mockActiveDirectoryService.getMembersByUsernameList(any())).thenReturn(emptyMap())
      whenever(mockMemberRepository.getMembersWithDiscourseUsernames()).thenReturn(testMembers)
      whenever(mockDiscourseAvatarService.refreshAvatarUrl("user1_discourse"))
          .thenReturn("/user_avatar/talk.dallasmakerspace.org/user1/{size}/123_2.png")

      // Run the job
      val result = memberRefreshCronJob.run(params)

      // Verify avatar service WAS called (shadow mode still tests API calls)
      verify(mockDiscourseAvatarService).refreshAvatarUrl("user1_discourse")
      // But database update should NOT happen in shadow mode
      verify(mockMemberRepository, never()).updateDiscourseAvatarUrl(any(), any())

      // Verify shadow mode logging
      verify(mockLogger).debug(argThat { this.contains("Shadow mode: Would update avatar") })

      assertTrue(result.isNotEmpty())
    }
  }

  @Test
  fun `run should handle avatar refresh failures gracefully`() {
    val params = MemberRefreshCronJobParams(isRunningInShadowMode = false, refreshAvatars = true)
    val testMembers =
        listOf(
            DMSMember(
                id = 1,
                username = "user1",
                displayName = "User One",
                enabled = true,
                discourseUsername = "user1_discourse"))

    runBlocking {
      // Setup mocks
      whenever(mockMemberService.getAllMembers()).thenReturn(testMembers)
      whenever(mockActiveDirectoryService.getMembersByUsernameList(any())).thenReturn(emptyMap())
      whenever(mockMemberRepository.getMembersWithDiscourseUsernames()).thenReturn(testMembers)
      whenever(mockDiscourseAvatarService.refreshAvatarUrl("user1_discourse"))
          .thenReturn(null) // Simulate failure

      // Run the job
      val result = memberRefreshCronJob.run(params)

      // Verify avatar service was called but database was not updated
      verify(mockDiscourseAvatarService).refreshAvatarUrl("user1_discourse")
      verify(mockMemberRepository, never()).updateDiscourseAvatarUrl(any(), any())

      // Verify failure logging
      verify(mockLogger).info(argThat { this.contains("Avatar refresh failed: 1") })

      assertTrue(result.isNotEmpty())
    }
  }

  @Test
  fun `run should handle members with null discourse usernames`() {
    val params = MemberRefreshCronJobParams(isRunningInShadowMode = false, refreshAvatars = true)
    val testMembers =
        listOf(
            DMSMember(
                id = 1,
                username = "user1",
                displayName = "User One",
                enabled = true,
                discourseUsername = null // No discourse username
                ))

    runBlocking {
      // Setup mocks
      whenever(mockMemberService.getAllMembers()).thenReturn(testMembers)
      whenever(mockActiveDirectoryService.getMembersByUsernameList(any())).thenReturn(emptyMap())
      whenever(mockMemberRepository.getMembersWithDiscourseUsernames()).thenReturn(testMembers)

      // Run the job
      val result = memberRefreshCronJob.run(params)

      // Verify avatar service was not called for null username
      verify(mockDiscourseAvatarService, never()).refreshAvatarUrl(any())

      // Verify warning was logged
      verify(mockLogger).warn(argThat { this.contains("has null discourse username") })

      assertTrue(result.isNotEmpty())
    }
  }

  @Test
  fun `MemberRefreshCronJobParams should have correct default values`() {
    val defaultParams = MemberRefreshCronJobParams()

    assertTrue(defaultParams.isRunningInShadowMode)
    assertEquals(false, defaultParams.refreshAvatars)
  }

  @Test
  fun `MemberRefreshCronJobParams toString should include all parameters`() {
    val params = MemberRefreshCronJobParams(isRunningInShadowMode = false, refreshAvatars = true)
    val toString = params.toString()

    assertTrue(toString.contains("isRunningInShadowMode=false"))
    assertTrue(toString.contains("refreshAvatars=true"))
  }
}
