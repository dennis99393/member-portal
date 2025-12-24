import { LitElement, html, css } from 'https://cdn.jsdelivr.net/npm/lit@3.3.0/+esm';

/**
 * DmsRelativeDate - A web component for displaying relative dates.
 *
 * @element dms-relative-date
 * @prop {String} timestamp - ISO 8601 date/time string (required)
 * @prop {String} prefix - Optional prefix text (e.g., "(")
 * @prop {String} suffix - Optional suffix text (e.g., ")")
 * @prop {Boolean} relativeOnly - If true, only show when date is relative (Today, Yesterday, X days/weeks ago)
 *
 * Output examples:
 * - "Today", "Yesterday", "3 days ago", "2 weeks ago", "Dec 18"
 *
 * Usage:
 * <dms-relative-date timestamp="2025-12-18T00:11:00"></dms-relative-date>
 * <dms-relative-date timestamp="2025-12-18" prefix="(" suffix=")"></dms-relative-date>
 * <dms-relative-date timestamp="2025-12-18" relativeOnly="true"></dms-relative-date>
 */
class DmsRelativeDate extends LitElement {
    static properties = {
        timestamp: { type: String },
        prefix: { type: String },
        suffix: { type: String },
        relativeOnly: { type: Boolean }
    };

    static styles = css`
        :host {
            display: inline;
        }

        .relative-date {
            color: #6c757d;
            font-size: inherit;
        }
    `;

    constructor() {
        super();
        this.timestamp = '';
        this.prefix = '';
        this.suffix = '';
        this.relativeOnly = false;
    }

    /**
     * Parse the timestamp and return a Date object in Chicago timezone
     * Handles multiple formats:
     * - ISO 8601: "2025-12-18T00:11:00"
     * - SQL format: "2025-12-18 00:11:00"
     * - Date only: "2025-12-18"
     */
    _parseTimestamp() {
        if (!this.timestamp) return null;

        try {
            // Normalize format: replace space with T for SQL-style dates
            let normalizedTimestamp = this.timestamp.trim();
            if (normalizedTimestamp.includes(' ') && !normalizedTimestamp.includes('T')) {
                normalizedTimestamp = normalizedTimestamp.replace(' ', 'T');
            }

            const date = new Date(normalizedTimestamp);
            if (isNaN(date.getTime())) return null;
            return date;
        } catch {
            return null;
        }
    }

    /**
     * Get today's date in Chicago timezone (date only, no time)
     */
    _getTodayInChicago() {
        const now = new Date();
        const chicagoTime = new Date(now.toLocaleString('en-US', { timeZone: 'America/Chicago' }));
        return new Date(chicagoTime.getFullYear(), chicagoTime.getMonth(), chicagoTime.getDate());
    }

    /**
     * Get the event date in Chicago timezone (date only, no time)
     */
    _getEventDateInChicago(date) {
        const chicagoTime = new Date(date.toLocaleString('en-US', { timeZone: 'America/Chicago' }));
        return new Date(chicagoTime.getFullYear(), chicagoTime.getMonth(), chicagoTime.getDate());
    }

    /**
     * Calculate days between two dates
     */
    _daysBetween(date1, date2) {
        const msPerDay = 24 * 60 * 60 * 1000;
        return Math.floor((date2 - date1) / msPerDay);
    }

    /**
     * Format the relative date string
     * @returns {Object} { text: string, isRelative: boolean }
     */
    _formatRelativeDate() {
        const eventDate = this._parseTimestamp();
        if (!eventDate) return { text: 'Invalid date', isRelative: false };

        const today = this._getTodayInChicago();
        const eventDateOnly = this._getEventDateInChicago(eventDate);
        const daysAgo = this._daysBetween(eventDateOnly, today);

        if (daysAgo === 0) {
            return { text: 'Today', isRelative: true };
        } else if (daysAgo === 1) {
            return { text: 'Yesterday', isRelative: true };
        } else if (daysAgo > 1 && daysAgo < 7) {
            return { text: `${daysAgo} days ago`, isRelative: true };
        } else if (daysAgo >= 7 && daysAgo < 30) {
            const weeks = Math.floor(daysAgo / 7);
            return { text: `${weeks} week${weeks > 1 ? 's' : ''} ago`, isRelative: true };
        } else if (daysAgo < 0) {
            // Future date
            const daysUntil = Math.abs(daysAgo);
            if (daysUntil === 1) {
                return { text: 'Tomorrow', isRelative: true };
            } else if (daysUntil < 7) {
                return { text: `in ${daysUntil} days`, isRelative: true };
            }
        }

        // For dates more than 30 days ago or far in future, show "MMM d" format
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                        'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        const chicagoDate = this._getEventDateInChicago(eventDate);
        return { text: `${months[chicagoDate.getMonth()]} ${chicagoDate.getDate()}`, isRelative: false };
    }

    render() {
        if (!this.timestamp) {
            return html`
                <span style="color: red; font-style: italic;">
                    Error: timestamp is required for dms-relative-date
                </span>
            `;
        }

        const { text, isRelative } = this._formatRelativeDate();

        // If relativeOnly is true and this is not a relative date, render nothing
        if (this.relativeOnly && !isRelative) {
            return html``;
        }

        return html`
            <span class="relative-date">${this.prefix}${text}${this.suffix}</span>
        `;
    }
}

customElements.define('dms-relative-date', DmsRelativeDate);
export { DmsRelativeDate };
