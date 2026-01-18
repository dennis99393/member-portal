# Short Links - Product Requirements Document

## Overview

A URL shortening service for Dallas Makerspace that provides branded short links under the `dallas.ms` domain,
redirecting through the member portal for tracking and management. The system supports both basic redirects and dynamic
pattern-based redirects, with namespace-based delegation to committees.

## Goals

- Provide memorable, branded short URLs for marketing materials, social media, and internal communications
- Enable namespace-based delegation so committees can self-manage their links
- Support both simple redirects and dynamic pattern-based redirects
- Track link usage and analytics
- Maintain control over link lifecycle (creation, modification, deactivation)

## User Stories

### As a Committee Chair (Woodshop, Ceramics, etc.)

- I want to manage links under my committee's namespace (e.g., `/ws`, `/cer`)
- I want to create memorable URLs like `dallas.ms/ws/safety-video`
- I want to see analytics for all links in my committee's namespace
- I want my namespace to have multiple aliases (e.g., `/ws`, `/wood`, `/woodshop`)

### As a Regular Member

- I want to create short links that get auto-generated slugs
- I want my short link to work reliably without managing custom slugs

### As an End User

- I want to click a `dallas.ms` link and be redirected to the intended destination
- I want the redirect to happen quickly without noticeable delay

### As an Infrastructure Team Member

- I want to create and manage namespaces for committees
- I want to review all active short links to prevent abuse
- I want to deactivate or delete inappropriate links
- I want to see global link usage statistics

## Core Concepts

### 1. Namespaces

Namespaces are top-level path segments that organize links and delegate management authority.

**Examples:**

- `/ws` - Woodshop namespace
- `/cer` - Ceramics namespace
- `/auto` - Automotive namespace
- Root (no prefix) - Auto-generated links for regular members

**Characteristics:**

- Owned by committees (mapped to Active Directory groups)
- Managed exclusively by Infrastructure team
- Support multiple aliases (e.g., `/ws`, `/wood`, `/woodshop` for same namespace)
- Each namespace has a primary alias for display purposes

### 2. Namespace Aliases

Multiple URL paths that resolve to the same namespace.

**Example:**

```
Namespace: Woodshop
Aliases: /ws (primary), /wood, /woodshop

All these URLs work identically:
- dallas.ms/ws/safety-video
- dallas.ms/wood/safety-video
- dallas.ms/woodshop/safety-video
```

**Benefits:**

- Flexibility: Short aliases for brevity, long aliases for clarity
- Backward compatibility: Can change primary alias without breaking links
- User choice: Different audiences can use different aliases

### 3. Redirect Types

#### Basic Redirects

Simple one-to-one URL mapping.

**Example:**

- Key: `safety-video`
- Destination: `https://youtube.com/watch?v=abc123`
- Result: `dallas.ms/ws/safety-video` → `https://youtube.com/watch?v=abc123`

#### Dynamic Redirects

Pattern-based redirects with variable substitution.

**Single Variable Example:**

- Pattern: `jira/{ticket_number}`
- Destination: `https://jira.dallasmakerspace.org/browse/{ticket_number}`
- Result: `dallas.ms/jira/MAKE-123` → `https://jira.dallasmakerspace.org/browse/MAKE-123`

**Multi-Variable Example:**

- Pattern: `gh/{org}/{repo}/issues/{number}`
- Destination: `https://github.com/{org}/{repo}/issues/{number}`
- Result: `dallas.ms/gh/dallasmakerspace/portal/issues/42` → GitHub issue

**Use Cases:**

- Jira tickets: `dallas.ms/jira/{ticket}`
- Wiki pages: `dallas.ms/wiki/{page}`
- Member profiles: `dallas.ms/member/{id}`
- Event pages: `dallas.ms/event/{event_id}`

### 4. Auto-Generated Links

Regular members create links at the root level with automatically generated slugs.

**Example:**

- User creates link to `https://example.com`
- System generates slug: `t6u9i3`
- Result: `dallas.ms/t6u9i3` → `https://example.com`

**Slug Characteristics:**

- 6-8 characters
- Lowercase letters + numbers
- Avoid ambiguous characters (0/O, 1/l/I)
- Guaranteed unique

## Technical Architecture

### Cloudflare Configuration

Simple wildcard redirect:

```
dallas.ms/* → members.dallasmakerspace.org/go/*
```

**Examples:**

- `dallas.ms/t6u9i3` → `members.dallasmakerspace.org/go/t6u9i3`
- `dallas.ms/ws/safety-video` → `members.dallasmakerspace.org/go/ws/safety-video`
- `dallas.ms/woodshop/safety-video` → `members.dallasmakerspace.org/go/woodshop/safety-video`

### Member Portal Routing

New route handler at `/go/{path...}` that:

1. Parses the incoming path
2. Resolves namespace aliases
3. Looks up the short link
4. Handles dynamic pattern matching
5. Logs the click event
6. Issues 302 redirect

**Path Resolution Logic:**

```
Input: "ws/safety-video"

1. Split on first '/' → alias="ws", slug="safety-video"
2. Resolve alias → namespace_id=5 (Woodshop)
3. Look up short_link WHERE namespace_id=5 AND slug="safety-video"
4. If basic: return destination_url
5. If dynamic: substitute variables and return
6. If not found: try dynamic pattern matching
7. If still not found: 404

Input: "t6u9i3" (no slash)

1. No namespace, this is root-level
2. Look up short_link WHERE namespace_id IS NULL AND slug="t6u9i3"
3. Return destination_url or 404
```

### Database Schema

```sql
-- Namespace registry
CREATE TABLE namespaces (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,                    -- Display name: "Woodshop"
    owner_type ENUM('committee', 'system') NOT NULL,
    owner_group_id INT,                            -- AD group ID for committee
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INT,                                -- Member ID
    INDEX(owner_group_id),
    INDEX(is_active)
);

-- Namespace aliases (multiple per namespace)
CREATE TABLE namespace_aliases (
    id INT PRIMARY KEY AUTO_INCREMENT,
    namespace_id INT NOT NULL,
    alias VARCHAR(20) NOT NULL UNIQUE,             -- "ws", "wood", "woodshop"
    is_primary BOOLEAN DEFAULT FALSE,              -- One primary per namespace
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (namespace_id) REFERENCES namespaces(id) ON DELETE CASCADE,
    UNIQUE INDEX(alias),
    INDEX(namespace_id)
);

-- Short links
CREATE TABLE short_links (
    id INT PRIMARY KEY AUTO_INCREMENT,
    namespace_id INT,                              -- NULL for root-level auto-generated
    slug VARCHAR(100) NOT NULL,                    -- "safety-video" or "jira/{ticket}"
    redirect_type ENUM('basic', 'dynamic') NOT NULL,
    destination_url VARCHAR(2048) NOT NULL,
    description TEXT,
    creator_id INT NOT NULL,                       -- Member ID
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (namespace_id) REFERENCES namespaces(id) ON DELETE CASCADE,
    UNIQUE INDEX(namespace_id, slug),              -- Slug unique within namespace
    INDEX(creator_id),
    INDEX(is_active),
    INDEX(redirect_type)
);

-- Click tracking
CREATE TABLE short_link_clicks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_link_id INT NOT NULL,
    resolved_path VARCHAR(255),                    -- Full path: "ws/safety-video"
    variable_values JSON,                          -- For dynamic: {"ticket": "MAKE-123"}
    clicked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),                        -- IPv6 support
    user_agent VARCHAR(500),
    referrer VARCHAR(2048),
    FOREIGN KEY (short_link_id) REFERENCES short_links(id) ON DELETE CASCADE,
    INDEX(short_link_id, clicked_at),
    INDEX(clicked_at)
);

-- Reserved keywords (system paths that can't be used as aliases)
CREATE TABLE reserved_aliases (
    alias VARCHAR(20) PRIMARY KEY,
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Initial reserved aliases
INSERT INTO reserved_aliases (alias, reason) VALUES
    ('short-links', 'Management interface'),
    ('api', 'API endpoints'),
    ('admin', 'Admin routes'),
    ('auth', 'Authentication'),
    ('static', 'Static assets'),
    ('go', 'Redirect handler'),
    ('login', 'Login page'),
    ('logout', 'Logout handler');
```

## Permission Model

### Three Tiers

#### 1. Infrastructure Team

- Create/edit/delete namespaces
- Assign namespace ownership to committees
- Manage all short links globally (any namespace)
- View global analytics
- Managed via AD group: "Infrastructure Team"

#### 2. Namespace Managers (Committee Chairs/Officers)

- Create/edit/delete links within their namespace(s)
- View analytics for their namespace(s)
- Cannot create or modify namespaces
- Managed via AD group membership (e.g., "Woodshop Officers")

#### 3. Regular Members

- Create links at root level (auto-generated slugs only)
- Edit/delete their own root-level links
- View analytics for their own links
- Cannot create custom slugs or use namespaces

### Permission Check Logic

```kotlin
fun canCreateLinkInNamespace(user: Member, namespace: Namespace?): Boolean {
  // Infrastructure team can do anything
  if (user.isInGroup("Infrastructure Team")) return true

  // Root-level (null namespace) - any member can create
  if (namespace == null) return true

  // Committee namespace - check if user is in owner group
  if (namespace.ownerGroupId != null) {
    return user.isInGroup(namespace.ownerGroupId)
  }

  return false
}

fun canEditLink(user: Member, link: ShortLink): Boolean {
  // Infrastructure team can edit anything
  if (user.isInGroup("Infrastructure Team")) return true

  // Creator can edit their own links
  if (link.creatorId == user.id) return true

  // Namespace managers can edit links in their namespace
  if (link.namespaceId != null) {
    val namespace = getNamespace(link.namespaceId)
    if (namespace.ownerGroupId != null && user.isInGroup(namespace.ownerGroupId)) {
      return true
    }
  }

  return false
}
```

## User Interface

### Management Interface: `/short-links`

#### For Regular Members

**List View:**

```
My Short Links

[+ Create Short Link]

┌─────────────────────────────────────────────────────────────┐
│ dallas.ms/t6u9i3                                      ↗ 245 │
│ → https://example.com/some/long/url                         │
│ Created: Mar 15, 2025 | Basic redirect                      │
│ [Edit] [Delete] [Analytics]                                 │
├─────────────────────────────────────────────────────────────┤
│ ...                                                         │
└─────────────────────────────────────────────────────────────┘
```

**Create Form:**

```
Create Short Link

Your link will be: dallas.ms/[auto-generated]

Redirect Type:
  ○ Basic      ○ Dynamic

Destination URL: [___________________________________________]
                 https://

Description (optional):
[___________________________________________________________]

[Preview] [Create]
```

#### For Committee Chairs

**List View:**

```
Woodshop Short Links (/ws, /wood, /woodshop)

[+ Create Short Link]

Filter: [All ▼] [Basic/Dynamic ▼] [Active/Inactive ▼] [Search___]

┌─────────────────────────────────────────────────────────────┐
│ dallas.ms/ws/safety-video                           ↗ 1,234 │
│ Also: /wood/safety-video, /woodshop/safety-video            │
│ → https://youtube.com/watch?v=abc123                        │
│ Created by: John Doe | 2024-03-15 | Basic redirect          │
│ [Edit] [Delete] [Analytics]                                 │
├─────────────────────────────────────────────────────────────┤
│ dallas.ms/ws/event/{id}                               ↗ 89  │
│ → https://events.dms.org/{id}                               │
│ Created by: Jane Smith | 2024-03-10 | Dynamic redirect      │
│ [Edit] [Delete] [Analytics]                                 │
└─────────────────────────────────────────────────────────────┘
```

**Create Form:**

```
Create Short Link

Namespace: [Woodshop ▼]
          Aliases: ws (primary), wood, woodshop

Custom slug: [_____________________]
             (letters, numbers, hyphens, and {variables} for dynamic)

Preview:
  dallas.ms/ws/[slug]
  dallas.ms/wood/[slug]
  dallas.ms/woodshop/[slug]

Redirect Type:
  ○ Basic      ○ Dynamic

[if Basic selected]
Destination URL: [___________________________________________]
                 https://

[if Dynamic selected]
Pattern: event/{id}
Destination URL: [___________________________________________]
                 https://events.dms.org/{id}

Variables detected: {id}

Test pattern:
  If someone visits: dallas.ms/ws/event/[42___]
  They will go to:   https://events.dms.org/42
  [Test]

Description (optional):
[___________________________________________________________]

[Preview] [Create]
```

#### For Infrastructure Team

**Namespace Management:**

```
Manage Namespaces

[+ Create Namespace]

┌─────────────────────────────────────────────────────────────┐
│ Woodshop                                          23 links  │
│ Aliases: ws (primary), wood, woodshop                       │
│ Owner: Woodshop Officers                                    │
│ [Edit] [View Links] [Analytics]                             │
├─────────────────────────────────────────────────────────────┤
│ Ceramics                                           8 links  │
│ Aliases: cer (primary), ceramics                            │
│ Owner: Ceramics Committee                                   │
│ [Edit] [View Links] [Analytics]                             │
└─────────────────────────────────────────────────────────────┘
```

**Create Namespace Form:**

```
Create Namespace

Display Name: [_________________________]
             (e.g., "Woodshop")

Primary Alias: [__________]
              (2-10 lowercase letters, e.g., "ws")

Additional Aliases (optional):
  [+ Add Alias]

Owner Type:
  ○ Committee    ○ System

[if Committee]
Owner Group: [Woodshop Officers ▼]
            (Active Directory group)

Description:
[___________________________________________________________]

[Create Namespace]
```

**Edit Namespace:**

```
Edit Namespace: Woodshop

Display Name: [Woodshop________________]

Aliases:
  ☑ ws (primary) [Remove]
  ☐ wood [Remove]
  ☐ woodshop [Remove]

  [+ Add Alias]

Owner Group: [Woodshop Officers ▼]

Description:
[Links for the Woodshop committee________________________]

Active: ☑ Yes  ☐ No

[Save Changes] [Cancel]
```

**Global Link Management:**
All links across all namespaces, with filters by namespace, creator, type, etc.

### Analytics Dashboard

#### Individual Link Analytics

```
Analytics: dallas.ms/ws/safety-video

Total Clicks: 1,234
Period: Last 30 days

Clicks Over Time:
[Line chart showing daily clicks]

Top Referrers:
1. facebook.com - 456 clicks
2. Direct/None - 234 clicks
3. instagram.com - 123 clicks

Devices:
  Mobile: 67%
  Desktop: 28%
  Tablet: 5%

[Export CSV] [View Full History]
```

#### Namespace Analytics (for Committee Managers)

```
Woodshop Namespace Analytics (/ws, /wood, /woodshop)

Total Clicks: 2,456 (last 30 days)
Total Links: 23 (18 basic, 5 dynamic)
Most Popular: safety-video (1,234 clicks)

Top Links:
1. /ws/safety-video - 1,234 clicks
2. /ws/cnc-training - 456 clicks
3. /ws/project-gallery - 234 clicks

[View All Links] [Export Report]
```

#### Global Analytics (for Infrastructure)

```
Global Short Links Analytics

Total Clicks: 15,789 (last 30 days)
Total Links: 1,234
Active Namespaces: 12

By Namespace:
┌──────────────────────────────────────────────┐
│ Woodshop (/ws)      2,456 clicks  (23 links) │
│ Ceramics (/cer)       432 clicks   (8 links) │
│ Automotive (/auto)    891 clicks  (15 links) │
│ Root (auto-gen)     5,123 clicks (347 links) │
│ ...                                          │
└──────────────────────────────────────────────┘

[View Details] [Export Full Report]
```

## Validation Rules

### Namespace Aliases

- Length: 2-10 characters
- Format: Lowercase letters only (a-z)
- Must not be in reserved list
- Must be globally unique
- At least one alias required per namespace
- Exactly one primary alias per namespace

### Slugs (within namespace)

- Length: 1-50 characters
- Basic format: Lowercase letters, numbers, hyphens
- Dynamic format: Same as basic, plus `{variable_name}` syntax
- Variable names: Lowercase letters, numbers, underscores
- Must be unique within namespace (not globally)
- Cannot start or end with hyphen

### Captured Variables (dynamic redirects)

- Allowed characters: Alphanumeric, hyphens, underscores
- Max length per variable: 100 characters
- No path traversal: Block `../`, `./`, `\`, etc.
- URL encode before substitution

### Destination URLs

- Must be valid HTTP/HTTPS URL
- Max length: 2048 characters
- Optional: Whitelist allowed domains for security
- For dynamic: Must contain all variables defined in pattern

## Security Considerations

### 1. Open Redirect Prevention

- Validate destination URLs
- Consider domain whitelist for sensitive namespaces
- Log all redirects for audit

### 2. Path Traversal Prevention

```kotlin
fun sanitizeVariable(value: String): String {
  // Block path traversal attempts
  if (value.contains("..") || value.contains("./") || value.contains("\\")) {
    throw SecurityException("Invalid characters in path")
  }
  return URLEncoder.encode(value, "UTF-8")
}
```

### 3. Rate Limiting

- Limit click tracking to prevent log spam
- Consider de-duplication (same IP within 1 minute = 1 click)
- Rate limit link creation per user

### 4. Authorization

- Always verify AD group membership server-side
- Don't trust client-side permission checks
- Log all administrative actions

### 5. Slug Enumeration

- Don't reveal total count of auto-generated links
- Consider using random slugs vs sequential encoding
- Rate limit 404 responses

## Analytics Tracking

### Click Event Data

**Capture:**

- Timestamp (with timezone)
- IP address (anonymize after 30 days for GDPR)
- User agent (browser/device)
- Referrer URL
- Resolved path (for dynamic links)
- Variable values (for dynamic links)

**Do NOT capture:**

- Personally identifiable information
- Authentication tokens
- Session data

### Aggregation Levels

1. **Individual link**: Specific slug performance
2. **Dynamic pattern**: All invocations of a pattern (e.g., all `/jira/*`)
3. **Namespace**: All links in a committee namespace
4. **Global**: System-wide statistics

### Retention

- Raw click events: 90 days
- Aggregated daily stats: 2 years
- IP addresses: Anonymize after 30 days

## Implementation Phases

### Phase 1: Core Functionality

**Scope:**

- Database schema and migrations
- Namespace registry with aliases
- Basic redirects (no dynamic patterns yet)
- Root-level auto-generated links
- Simple CRUD UI for link management
- Permission model (Infrastructure, Committee, Regular)
- `/go/*` redirect handler
- Basic click tracking

**Success Criteria:**

- Infrastructure can create namespaces
- Committee chairs can create basic links in their namespace
- Regular members can create auto-generated links
- All links redirect correctly
- Aliases work (multiple paths to same link)

### Phase 2: Dynamic Redirects

**Scope:**

- Dynamic pattern parsing and matching
- Variable substitution engine
- Pattern testing UI
- Enhanced analytics for dynamic patterns
- Variable validation and sanitization

**Success Criteria:**

- Users can create patterns like `jira/{ticket}`
- Pattern matching works correctly
- Variables are safely substituted
- Analytics distinguish between pattern and invocations

### Phase 3: Analytics Dashboard

**Scope:**

- Click-over-time charts
- Referrer analysis
- Device/browser breakdown
- Namespace-level rollups
- Export to CSV
- Top links reports

**Success Criteria:**

- Committee managers can see their namespace analytics
- Infrastructure can see global analytics
- Charts render correctly
- Exports work

### Phase 4: Advanced Features

**Scope:**

- QR code generation
- Link expiration dates
- Bulk import/export
- Link preview with OpenGraph metadata
- A/B testing (multiple destinations)
- Custom 404 pages per namespace
- Audit logs

**Future Consideration:**

- Browser extension for quick creation
- API for programmatic link creation
- Webhook notifications on click thresholds
- Integration with Google Analytics

## Success Metrics

### Adoption

- Number of namespaces created (target: 10+ in first 3 months)
- Number of short links created per month (target: 50+ in first month)
- Percentage of committees using the feature (target: 50% in 6 months)
- Active users creating links (target: 20+ in first 3 months)

### Engagement

- Click-through rate across all links (monitor for quality)
- Average clicks per link (measure usefulness)
- Ratio of dynamic to basic links (measure feature adoption)

### Technical

- Average redirect response time (target: <100ms)
- 99th percentile redirect time (target: <500ms)
- Uptime (target: 99.9%)
- Zero security incidents

### User Satisfaction

- Survey committee managers quarterly
- Collect feedback on management UI
- Track support requests related to short links

## Open Questions

1. **Cloudflare Setup**: Who manages the `dallas.ms` domain? Do we need DNS changes or just Cloudflare rules?

2. **Namespace Limits**: Should there be a maximum number of links per namespace to prevent abuse?

3. **Link Approval**: Do committee-created links need Infrastructure team approval, or are committees trusted?

4. **Public Directory**: Should there be a public directory of all short links, or keep them discoverable only by
   knowing the URL?

5. **Transfer Ownership**: If a committee member leaves, should their links transfer to the committee namespace owner?

6. **Namespace Archival**: When a committee disbands, should the namespace be archived (read-only) or deleted?

7. **Vanity Domains**: In the future, could we support `dallasmakerspace.org/go/*` in addition to `dallas.ms/*`?

8. **Link Versioning**: Should we keep history when a link's destination changes?

9. **Temporary Links**: Should we support TTL/expiration on links for time-limited campaigns?

10. **API Access**: Should there be a REST API for programmatic link creation (e.g., from automation tools)?

## Migration & Rollout

### Initial Namespaces to Create

**Committees:**

- `/ws`, `/wood`, `/woodshop` → Woodshop
- `/cer`, `/ceramics` → Ceramics
- `/auto`, `/automotive` → Automotive
- `/metal`, `/metalshop` → Metal Shop
- `/craft` → Craft Lab
- `/elec`, `/electronics` → Electronics
- `/3dp`, `/3dprint` → 3D Printing
- `/laser` → Laser Cutting
- `/cnc` → CNC

**System:**

- `/event`, `/events` → Event promotion (Infrastructure-managed)
- `/infra` → Infrastructure team's own links

### Rollout Plan

1. **Alpha**: Infrastructure team only (2 weeks)
    - Create namespaces
    - Test all features
    - Refine UI

2. **Beta**: Select 2-3 committees (4 weeks)
    - Woodshop, Ceramics, Automotive
    - Gather feedback
    - Fix bugs

3. **General Availability**: All members
    - Announce in newsletter
    - Training session for committee chairs
    - Documentation and video tutorial

### Communication

- Announcement email to all members
- Newsletter article with examples
- Training session for committee leaders
- Wiki documentation page
- Quick reference card (PDF)

## Appendix: Example Use Cases

### Marketing Campaign

```
Type: Basic
Namespace: Root (auto-generated)
Short Link: dallas.ms/x7k2m9
Destination: https://dallasmakerspace.org/open-house-2024
Use: Printed on flyers for open house
```

### Committee Resource

```
Type: Basic
Namespace: Woodshop (/ws, /wood, /woodshop)
Slug: safety-video
Short Link: dallas.ms/ws/safety-video
Aliases: dallas.ms/wood/safety-video, dallas.ms/woodshop/safety-video
Destination: https://youtube.com/watch?v=abc123
Use: Shared with new members during orientation
```

### Dynamic Pattern - Issue Tracker

```
Type: Dynamic
Namespace: Root
Pattern: jira/{ticket}
Short Link: dallas.ms/jira/MAKE-123
Destination: https://jira.dallasmakerspace.org/browse/{ticket}
Use: Quick access to tickets in Slack/email
```

### Dynamic Pattern - Wiki Pages

```
Type: Dynamic
Namespace: Root
Pattern: wiki/{page}
Short Link: dallas.ms/wiki/3d-printing-guide
Destination: https://source.dallasmakerspace.org/{page}
Use: Shareable wiki links
```

### Event Promotion

```
Type: Dynamic
Namespace: Events (/event, /events)
Pattern: {event_id}
Short Link: dallas.ms/event/2024-maker-faire
Destination: https://events.dallasmakerspace.org/{event_id}
Use: Social media posts
```

### Member Profile

```
Type: Dynamic
Namespace: Root
Pattern: member/{member_id}
Short Link: dallas.ms/member/12345
Destination: https://members.dallasmakerspace.org/profile/{member_id}
Use: Member networking, signature links
```

---

## Implementation Progress

### Phase 1 - Completed

- [x] Database schema (`short_links_namespaces`, `short_links_namespace_aliases`, `short_links`, `short_links_clicks`,
  `short_links_reserved_aliases`)
- [x] Domain models in `common-models` (`Namespace`, `NamespaceAlias`, `ShortLink`, `ShortLinkClick`)
- [x] Repository layer (`ShortLinksRepository`)
- [x] Service layer with validation (`ShortLinksService`)
- [x] API routes for CRUD operations (service `Routing.kt`)
- [x] Dependency injection (`ShortLinksModule`, `AppComponent`)
- [x] UI management page (`/short-links`) with Thymeleaf template
- [x] Backend API proxy for POST/PATCH/DELETE operations
- [x] `/go/{path...}` redirect handler (UI proxies to service)
- [x] Basic click tracking
- [x] Namespace management (create, list, update, delete)
- [x] Short link management (create with auto-generated slug, list, update, delete)
- [x] Popular links endpoint
- [x] Creator ID tracking from authenticated user

### Phase 1 - Remaining

- [ ] Permission enforcement in UI (show/hide based on user groups)
- [ ] Alias management UI (add/remove aliases for namespaces)
- [ ] Link filtering by namespace in UI
- [ ] Validation error display in UI
- [ ] Reserved alias checking

### Phase 2 - Not Started

- [ ] Dynamic pattern parsing and matching
- [ ] Variable substitution engine
- [ ] Pattern testing UI

### Phase 3 - Not Started

- [ ] Analytics dashboard
- [ ] Click-over-time charts
- [ ] Referrer/device breakdown

---

**Document Version**: 1.1
**Last Updated**: 2024-12-21
**Author**: Infrastructure Team
**Status**: Phase 1 In Progress
