# Tech Lead Review: Badge Validation & Fix Implementation

**Date:** 2026-02-16
**Developer:** Claude Code
**Reviewer Needed:** Tech Lead

## Summary

Implemented badge validation reports and badge fix functionality to help IT Infrastructure team identify and correct badge numbers that are less than 10 digits in Active Directory.

## Changes Overview

### 1. Badge Validation Reports (Data Visualization)

**Purpose:** Help Infrastructure team identify members with invalid badge numbers (< 10 digits)

#### Backend Reports Created:
- **`BadgeValidationReport.kt`** - Queries MakerManager database for active members with badges < 10 digits
- **`BadgeValidationAdReport.kt`** - Queries Active Directory (with pagination) for active users with badges < 10 digits

**Features:**
- Paginated LDAP queries (1000 entries per batch) to avoid size limit errors
- Member profile enrichment with Discourse avatars
- Sorted by badge length (shortest first) for easy prioritization
- Restricted to IT Infrastructure team only

#### Frontend Templates Created:
- `ui/.../templates/fragments/reports/it/badge-validation.html`
- `ui/.../templates/fragments/reports/it/badge-validation-ad.html`

**Navigation:** Added new "IT Infrastructure" category to reports menu

### 2. Badge Fix Functionality (Profile Page)

**Purpose:** Allow Infrastructure team to fix badge numbers directly from profile pages

#### API Endpoint:
```
POST /members/{username}/fix-badge
```

**Logic:**
1. Validates badge exists and is < 10 digits
2. Prepends zeros to make it 10 digits (e.g., "123" → "0000000123")
3. Updates Active Directory `employeeID` field via LDAP
4. Logs activity with old/new values

#### Profile Page Updates:
- Shows two separate badge number rows:
  - "Badge (MakerManager)"
  - "Badge (Active Directory)"
- "Fix Badge Length in AD" button (only visible to Infra when badge < 10 digits)
- JavaScript handler with confirmation dialog and loading state

#### Activity Log:
- New event type: `FIX_BADGE_LENGTH_AD(12)`
- Logs old and new badge numbers in attributes JSON

## Files Changed/Created

### Common Models
- `common-models/.../ActivityLog.kt` - Added `FIX_BADGE_LENGTH_AD` event

### Service (Backend)
**Created:**
- `service/.../reports/membership/BadgeValidationReport.kt`
- `service/.../reports/it/BadgeValidationAdReport.kt`

**Modified:**
- `service/.../routing/Members.kt` - Added `FixBadge` resource
- `service/.../plugins/Routing.kt` - Added fix badge endpoint
- `service/.../members/MemberService.kt` - Added `updateBadgeInAD()` helper
- `service/.../activedirectory/IActiveDirectoryClient.kt` - Added `updateBadgeNumber()` interface
- `service/.../activedirectory/ActiveDirectoryClient.kt` - Implemented `updateBadgeNumber()` and `getAllActiveUsersWithBadges()` with pagination
- `service/.../activedirectory/ActiveDirectoryService.kt` - Added `updateBadgeNumber()` wrapper
- `service/.../activedirectory/IActiveDirectoryService.kt` - Added interface method
- `service/.../activedirectory/*Mock.kt` - Added mock implementations
- `service/.../dataviz/di/DataVizModule.kt` - Registered new reports

### UI (Frontend)
**Created:**
- `ui/.../templates/fragments/reports/it/` (new directory)
- `ui/.../templates/fragments/reports/it/badge-validation.html`
- `ui/.../templates/fragments/reports/it/badge-validation-ad.html`

**Modified:**
- `ui/.../server/reports/ReportGraph.kt` - Added "IT Infrastructure" category
- `ui/.../server/routes/ProfileHandler.kt` - Added badge validation logic
- `ui/.../templates/profile.html` - Added badge display and fix button with JavaScript

## Potential Issues & Concerns

### 🔴 Critical Issues

1. **LDAP Modification Permissions**
   - The Active Directory client needs write permissions to modify `employeeID` field
   - **Action Required:** Verify service account has appropriate LDAP write permissions
   - **Test:** Try fixing a badge in dev/staging first

2. **Badge Source Discrepancy**
   - Currently, both "Badge (MakerManager)" and "Badge (Active Directory)" show the same value from `member.badgeNumber`
   - **Issue:** We're not actually fetching badges from both sources separately
   - **Action Required:** Profile handler should query both MM and AD independently to show true differences
   - **Current Code:** Lines 90-100 in ProfileHandler.kt

### 🟡 Medium Priority Issues

3. **No Rollback Mechanism**
   - Once badge is fixed in AD, there's no undo functionality
   - **Recommendation:** Consider adding activity log viewing or rollback feature
   - **Workaround:** Activity log records old value for manual rollback if needed

4. **Concurrent Modification**
   - No locking mechanism if two admins try to fix the same badge simultaneously
   - **Risk:** Low (unlikely scenario)
   - **Mitigation:** Activity log will show both actions

5. **Error Handling**
   - LDAP errors are caught but may need more specific handling
   - **Action Required:** Test various failure scenarios:
     - User not found in AD
     - LDAP connection failure
     - Permission denied
     - Invalid badge format

### 🟢 Low Priority / Enhancement Ideas

6. **Batch Badge Fix**
   - Reports show many invalid badges, but fix is one-at-a-time
   - **Enhancement:** Consider adding bulk fix from report page

7. **Validation Scope**
   - Currently only checks length, not format (e.g., non-numeric characters)
   - **Enhancement:** Add format validation (should be all digits)

8. **Cache Invalidation**
   - After fixing badge, member cache may be stale
   - **Impact:** Minor - page reload fetches fresh data
   - **Enhancement:** Consider explicit cache invalidation

## Testing Checklist

### Unit Tests Needed
- [ ] Test `updateBadgeNumber()` in ActiveDirectoryClient (mock LDAP)
- [ ] Test fix badge endpoint with various scenarios
- [ ] Test badge validation reports pagination

### Integration Tests Needed
- [ ] Test LDAP modification in dev environment
- [ ] Verify activity log entry creation
- [ ] Test permission restrictions (non-infra users shouldn't see button)

### Manual Testing Checklist
- [ ] Run badge validation reports as Infra member
- [ ] Verify pagination works for large result sets (AD report)
- [ ] View profile with badge < 10 digits as Infra member
- [ ] Click "Fix Badge" button and verify confirmation dialog
- [ ] Confirm badge updated in AD
- [ ] Verify activity log entry created
- [ ] Confirm non-Infra members don't see fix button
- [ ] Test error handling (disconnect service, invalid user, etc.)

## Security Considerations

### ✅ Good
- Endpoint protected by `member:write` permission
- UI button only shown to Infrastructure team members
- Activity logged with subject username
- Confirmation dialog prevents accidental clicks

### ⚠️ Review Needed
- **API Authorization:** Endpoint has `member:write` but should it require `Infrastructure` group membership?
- **Actor Tracking:** Activity log doesn't capture who performed the fix (actor is null)
  - **Fix:** Pass authenticated user as actor in activity log

## Deployment Notes

### Prerequisites
1. Verify LDAP service account has write permissions for `employeeID` attribute
2. Test in dev/staging before production
3. Notify Infrastructure team of new feature

### Database Migrations
- None required (only enum addition)

### Configuration Changes
- None required

### Monitoring
- Watch for LDAP modification errors in logs
- Monitor activity log for fix badge events

## Recommendations

### ✅ Fixed (2026-02-16)
1. **✅ Fixed Critical Issue #2:** Profile handler now fetches badges from both MM and AD separately
   - Added `getBadgeFromMakerManager()` and `getBadgeFromActiveDirectory()` methods to service MemberService
   - Added API endpoints: GET `/members/{username}/badge-mm` and `/members/{username}/badge-ad`
   - Updated UI MemberServiceClient to call new endpoints
   - Updated ProfileHandler to fetch and display separate badge values
2. **✅ Fixed Security Issue:** Added actor username to activity log
   - Created new overload of `insertActivityLogEntry()` accepting separate actor and subject
   - Updated fix badge endpoint to extract authenticated user from `X-Username` header
   - Activity log now records who performed the fix (actor) separately from whose badge was fixed (subject)
3. **✅ Added validation:** Badge is now validated to be numeric before fixing
   - Added check to ensure badge contains only digits
   - Returns error if badge contains non-numeric characters
4. **✅ Added attributes:** Activity log now records old and new badge values in JSON attributes

### Before Merge
4. **Add tests:** At least integration tests for LDAP modification

### Code Example for Fix #1 (Badge Source Issue):
```kotlin
// In ProfileHandler.kt around line 90
if (jsonMap["is_self"] == "true" || isInfra) {
  // Get badge from MakerManager
  val mmBadge = memberService.getBadgeFromMakerManager(requestedUsername)
  mmBadge?.let { jsonMap["badge_number_mm"] = it }

  // Get badge from Active Directory
  val adBadge = memberService.getBadgeFromActiveDirectory(requestedUsername)
  adBadge?.let { jsonMap["badge_number_ad"] = it }

  // Check if they differ or if AD badge needs fixing
  if (adBadge != null && adBadge.length < 10) {
    jsonMap["badge_needs_fix"] = true
    jsonMap["badge_padded"] = adBadge.padStart(10, '0')
  }
}
```

### After Merge
1. Monitor first few badge fixes closely
2. Gather feedback from Infrastructure team
3. Consider batch fix feature if many badges need fixing

## Questions for Reviewer

1. Should we add a confirmation step requiring typing the padded badge number?
2. Should badge fix be restricted to Infrastructure group at API level, not just UI?
3. Do we need approval workflow or audit trail beyond activity log?
4. Should we send notifications when badges are fixed (email/Discord)?
5. Is there a concern about fixing badges that might be intentionally short for some reason?

---

**Status:** ✅ Critical issues fixed - Ready for testing and review
**Time to Fix:** 1 hour for all critical fixes (badge separation, actor tracking, validation, attributes)
**Remaining:** Integration tests needed before production deployment
