import { LitElement, html, css } from 'https://cdn.jsdelivr.net/npm/lit@3.3.0/+esm';

/**
 * DmsTableBase - A base table component for rendering both regular and metadata tables.
 *
 * @element dms-table-base
 * @prop {Array} headers - Column headers for regular tables (not used for metadata)
 * @prop {Array} rows - Data rows. For regular tables: array of arrays. For metadata: array of [key, value] pairs.
 * @prop {Boolean} isMetadata - Whether to render as a metadata table (key-value pairs)
 */
class DmsTableBase extends LitElement {
    static properties = {
        headers: { type: Array },
        rows: { type: Array },
        isMetadata: { type: Boolean },
    };

    static styles = css`
        .table-container {
            overflow-x: auto;
            margin-top: 16px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 16px;
            border: 1px solid #ddd;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 8px;
            text-align: left;
            vertical-align: top;
        }
        th {
            background-color: #f2f2f2;
        }
        .metadata-table td:first-child {
            font-weight: bold;
            width: 150px;
            background-color: #f2f2f2;
        }
        .metadata-table pre {
            white-space: pre-wrap;
            white-space: -moz-pre-wrap;
            white-space: -pre-wrap;
            white-space: -o-pre-wrap;
            word-wrap: break-word;
            margin: 0;
            font-family: monospace;
        }
    `;

    constructor() {
        super();
        this.headers = [];
        this.rows = [];
        this.isMetadata = false;
    }

    render() {
        return html`
            <div class="table-container">
                <table class="${this.isMetadata ? 'metadata-table' : ''}">
                    ${this.isMetadata ? this._renderMetadataTable() : this._renderRegularTable()}
                </table>
            </div>
        `;
    }

    _renderRegularTable() {
        return html`
            <thead>
                <tr>
                    ${this.headers.map(header => html`<th>${header}</th>`)}
                </tr>
            </thead>
            <tbody>
                ${this.rows.map(row => html`
                    <tr>
                        ${row.map(cell => html`<td>${cell === null ? 'null' : cell}</td>`)}
                    </tr>
                `)}
            </tbody>
        `;
    }

    _renderMetadataTable() {
        return html`
            <tbody>
                ${this.rows.map(([key, value]) => html`
                    <tr>
                        <td>${key}</td>
                        <td>${key === 'SQL Query' ? html`<pre>${value}</pre>` : value}</td>
                    </tr>
                `)}
            </tbody>
        `;
    }
}

customElements.define('dms-table-base', DmsTableBase);
export { DmsTableBase };
