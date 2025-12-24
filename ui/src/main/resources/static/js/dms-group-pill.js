import { LitElement, html, css } from 'https://cdn.jsdelivr.net/npm/lit@3.3.0/+esm';

/**
 * DmsGroupPill - A web component for displaying group links as styled pills.
 *
 * @element dms-group-pill
 * @prop {String} name - The group name to display (required)
 * @prop {String} slug - The group slug for generating the URL (optional if href is provided)
 * @prop {String} href - Custom URL for the link (optional, overrides slug)
 * @prop {Boolean} openInNewWindow - Whether link should open in new window (default: false)
 *
 * Usage:
 * <dms-group-pill name="Woodworking Teachers" slug="woodworking-teachers"></dms-group-pill>
 * <dms-group-pill name="3D Printing" href="/groups/3d-printing"></dms-group-pill>
 */
class DmsGroupPill extends LitElement {
    static properties = {
        name: { type: String },
        slug: { type: String },
        href: { type: String },
        openInNewWindow: { type: Boolean }
    };

    static styles = css`
        :host {
            display: inline-block;
        }

        .group-pill {
            background-color: #e1f2f7;
            border-radius: 15px;
            padding: 4px 12px;
            font-size: 0.85rem;
            text-decoration: none;
            color: #0a7ca0;
            transition: all 0.2s ease;
            white-space: nowrap;
            display: inline-block;
        }

        .group-pill:hover {
            background-color: #0b9cc9;
            color: white;
        }
    `;

    constructor() {
        super();
        this.name = '';
        this.slug = '';
        this.href = '';
        this.openInNewWindow = false;
    }

    /**
     * Generate the group URL
     */
    _getGroupUrl() {
        if (this.href) {
            return this.href;
        }
        return `/groups/${this.slug}`;
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

    render() {
        if (!this.name) {
            return html`
                <span style="color: red; font-style: italic;">
                    Error: name is required for dms-group-pill
                </span>
            `;
        }

        return html`
            <a
                href="${this._getGroupUrl()}"
                class="group-pill"
                target="${this._getLinkTarget()}"
                rel="${this._getLinkRel()}"
                title="${this.name}"
            >
                ${this.name}
            </a>
        `;
    }
}

customElements.define('dms-group-pill', DmsGroupPill);
export { DmsGroupPill };
