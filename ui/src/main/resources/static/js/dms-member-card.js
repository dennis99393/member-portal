import { LitElement, html, css } from 'https://cdn.jsdelivr.net/npm/lit@3.3.0/+esm';

/**
 * DmsMemberCard - A web component for displaying DMS member information in various states.
 *
 * @element dms-member-card
 * @prop {String} username - The member's username (required)
 * @prop {String} displayName - The member's display name (optional, defaults to username)
 * @prop {String} avatarUrl - URL to the member's avatar image (optional)
 * @prop {String} state - Display state: 'inline', 'mini', 'medium', 'large' (default: 'inline')
 * @prop {Boolean} openInNewWindow - Whether profile links should open in new window (default: false)
 *
 * States:
 * - inline: Simple @username link (no card/border styling)
 * - mini: Avatar, username, and display name in compact format (no card/border styling)
 * - medium: (Future implementation) Enhanced card with additional info
 * - large: (Future implementation) Full member profile card
 *
 * Usage:
 * <dms-member-card username="johndoe" display-name="John Doe" state="inline"></dms-member-card>
 * <dms-member-card username="janedoe" display-name="Jane Doe" avatar-url="/avatars/jane.jpg" state="mini"></dms-member-card>
 */
class DmsMemberCard extends LitElement {
    static properties = {
        username: { type: String },
        displayName: { type: String },
        avatarUrl: { type: String },
        state: { type: String },
        openInNewWindow: { type: Boolean }
    };

    static styles = css`
        :host {
            display: block;
            width: 100%;
            max-width: 100%;
        }

        /* Inline state styles */
        .inline-card {
            display: inline;
        }

        .inline-link {
            color: #4a90e2;
            text-decoration: none;
            font-weight: 500;
        }

        .inline-link:hover {
            text-decoration: underline;
            color: #357abd;
        }

        /* Mini state styles - no card/border styling */
        .mini-card {
            display: flex;
            align-items: center;
            text-decoration: none;
            color: inherit;
            transition: opacity 0.2s ease;
            width: 100%;
        }

        .mini-card:hover {
            opacity: 0.8;
            text-decoration: none;
            color: inherit;
        }

        .avatar {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            border: 1px solid #ccc;
            margin-right: 12px;
            flex-shrink: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 600;
            color: #333;
            font-size: 16px;
            overflow: hidden;
            text-transform: uppercase;
        }

        .avatar img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            border-radius: 50%;
        }

        .member-info {
            flex: 1;
            min-width: 0; /* Allow text truncation */
        }

        .username {
            font-weight: 500;
            color: #4a90e2;
            font-size: 14px;
            margin: 0 0 2px 0;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            line-height: 1.2;
        }

        .display-name {
            font-size: 13px;
            color: #333;
            margin: 0;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            line-height: 1.2;
        }

        /* Future state placeholders */
        .medium-card,
        .large-card {
            /* Placeholder for future implementations */
            display: block;
            padding: 16px;
            border: 2px dashed #ccc;
            border-radius: 8px;
            text-align: center;
            color: #666;
            font-style: italic;
        }

        /* Responsive adjustments */
        @media (max-width: 768px) {
            .avatar {
                width: 36px;
                height: 36px;
                margin-right: 10px;
                font-size: 14px;
            }

            .username {
                font-size: 13px;
            }

            .display-name {
                font-size: 12px;
            }
        }
    `;

    constructor() {
        super();
        this.username = '';
        this.displayName = '';
        this.avatarUrl = '';
        this.state = 'inline';
        this.openInNewWindow = false;
    }

    /**
     * Generate the profile URL for the member
     */
    _getProfileUrl() {
        return `/profile/@${this.username}`;
    }

    /**
     * Get the display name, falling back to username if not provided
     */
    _getDisplayName() {
        return this.displayName || this.username;
    }

    /**
     * Get the target attribute for links
     */
    _getLinkTarget() {
        return this.openInNewWindow ? '_blank' : '_self';
    }

    /**
     * Get the rel attribute for links (security for new window)
     */
    _getLinkRel() {
        return this.openInNewWindow ? 'noopener noreferrer' : '';
    }

    /**
     * Generate background color for avatar - using simple light gray
     */
    _getAvatarBackgroundColor(username) {
        // Simple light gray background for all avatars
        return '#f0f0f0';
    }

    /**
     * Simple hash function that approximates MD5 behavior for color selection
     * This creates a consistent hash that works well for color distribution
     */
    _createSimpleHash(str) {
        let hash = 0;
        for (let i = 0; i < str.length; i++) {
            const char = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & 0xffffffff; // Convert to 32-bit integer
        }

        // Convert to hex and pad to ensure we have enough characters
        let hex = Math.abs(hash).toString(16);
        while (hex.length < 8) {
            hex = '0' + hex;
        }

        return hex;
    }

    /**
     * Generate avatar content (image or letter with background)
     */
    _renderAvatar() {
        if (this.avatarUrl) {
            return html`<img src="${this.avatarUrl}" alt="${this._getDisplayName()}" loading="lazy">`;
        }

        // Generate letter from display name or username
        const name = this._getDisplayName();
        const letter = name.charAt(0).toUpperCase();
        const backgroundColor = this._getAvatarBackgroundColor(name);

        return html`
            <div style="background-color: ${backgroundColor}; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; border-radius: 50%;">
                ${letter}
            </div>
        `;
    }

    /**
     * Render the inline state
     */
    _renderInline() {
        return html`
            <div class="inline-card">
                <a
                    href="${this._getProfileUrl()}"
                    class="inline-link"
                    target="${this._getLinkTarget()}"
                    rel="${this._getLinkRel()}"
                    title="${this._getDisplayName()}"
                >
                    @${this.username}
                </a>
            </div>
        `;
    }

    /**
     * Render the mini state
     */
    _renderMini() {
        return html`
            <a
                href="${this._getProfileUrl()}"
                class="mini-card"
                target="${this._getLinkTarget()}"
                rel="${this._getLinkRel()}"
                title="View ${this._getDisplayName()}'s profile"
            >
                <div class="avatar">
                    ${this._renderAvatar()}
                </div>
                <div class="member-info">
                    <div class="username">@${this.username}</div>
                    <div class="display-name">${this._getDisplayName()}</div>
                </div>
            </a>
        `;
    }

    /**
     * Render placeholder for future states
     */
    _renderFutureState(stateName) {
        return html`
            <div class="${stateName}-card">
                ${stateName.charAt(0).toUpperCase() + stateName.slice(1)} state - Coming soon
            </div>
        `;
    }

    render() {
        // Validate required username
        if (!this.username) {
            return html`
                <div style="color: red; font-style: italic;">
                    Error: username is required for dms-member-card
                </div>
            `;
        }

        switch (this.state) {
            case 'inline':
                return this._renderInline();
            case 'mini':
                return this._renderMini();
            case 'medium':
                return this._renderFutureState('medium');
            case 'large':
                return this._renderFutureState('large');
            default:
                console.warn(`Unknown state "${this.state}" for dms-member-card, defaulting to inline`);
                return this._renderInline();
        }
    }
}

customElements.define('dms-member-card', DmsMemberCard);
export { DmsMemberCard };
