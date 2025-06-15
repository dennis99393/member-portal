import { LitElement, html, css } from 'https://cdn.jsdelivr.net/npm/lit@3.3.0/+esm';

/**
 * DmsTableBase - A base table component for rendering both regular and metadata tables.
 *
 * @element dms-table-base
 * @prop {Array} headers - Column headers for regular tables (not used for metadata)
 * @prop {Array} rows - Data rows. For regular tables: array of arrays. For metadata: array of [key, value] pairs.
 * @prop {Boolean} isMetadata - Whether to render as a metadata table (key-value pairs)
 * @prop {Boolean} filterable - Whether to enable filtering for the table
 */
class DmsTableBase extends LitElement {
    static properties = {
        headers: { type: Array },
        rows: { type: Array },
        isMetadata: { type: Boolean },
        filterable: { type: Boolean },
        _filterText: { type: String, state: true },
        _filteredRows: { type: Array, state: true }
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
        .filter-container {
            display: flex;
            align-items: center;
            margin-bottom: 8px;
            position: relative;
            max-width: 300px;
            margin-left: auto; /* Align to the right */
        }
        .filter-input {
            flex: 1;
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 14px;
            width: 100%;
        }
        .clear-filter {
            position: absolute;
            right: 8px;
            background: none;
            border: none;
            cursor: pointer;
            font-weight: bold;
            color: #777;
        }
        .clear-filter:hover {
            color: #333;
        }
        .empty-state {
            padding: 20px;
            text-align: center;
            color: #666;
            font-style: italic;
            border: 1px solid #ddd;
            margin-top: 16px;
        }
        @media (max-width: 768px) {
            .filter-container {
                max-width: 100%;
            }
        }
    `;

    constructor() {
        super();
        this.headers = [];
        this.rows = [];
        this.isMetadata = false;
        this.filterable = false;
        this._filterText = '';
        this._filteredRows = [];
    }

    updated(changedProperties) {
        if (changedProperties.has('rows') || changedProperties.has('_filterText')) {
            this._filterRows();
        }
    }

    _filterRows() {
        if (!this._filterText || this._filterText.trim() === '') {
            this._filteredRows = this.rows;
            return;
        }

        const searchTerm = this._filterText.toLowerCase();

        if (this.isMetadata) {
            this._filteredRows = this.rows.filter(([key, value]) => {
                const keyStr = String(key).toLowerCase();
                const valueStr = String(value).toLowerCase();
                return keyStr.includes(searchTerm) || valueStr.includes(searchTerm);
            });
        } else {
            this._filteredRows = this.rows.filter(row => {
                return row.some(cell => {
                    if (cell === null) return false;
                    return String(cell).toLowerCase().includes(searchTerm);
                });
            });
        }
    }

    _handleFilterInput(e) {
        this._filterText = e.target.value;
    }

    _handleFilterFocus(e) {
        // Check if on mobile device
        if (window.innerWidth <= 768) {
            // Scroll the filter to the top of the viewport with some padding
            setTimeout(() => {
                const filterContainer = this.shadowRoot.querySelector('.filter-container');
                if (filterContainer) {
                    filterContainer.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    // Add a little extra padding at the top
                    window.scrollBy(0, -15);
                }
            }, 100);
        }
    }

    _clearFilter() {
        this._filterText = '';
        const input = this.shadowRoot.querySelector('.filter-input');
        if (input) input.value = '';
    }

    render() {
        const hasFilteredData = this._filterText && this._filteredRows.length === 0;

        return html`
            <div class="table-container">
                ${this.filterable ? html`
                    <div class="filter-container">
                        <input
                            type="text"
                            class="filter-input"
                            placeholder="Type to filter"
                            .value="${this._filterText}"
                            @input="${this._handleFilterInput}"
                            @focus="${this._handleFilterFocus}">
                        ${this._filterText ? html`
                            <button class="clear-filter" @click="${this._clearFilter}">×</button>
                        ` : ''}
                    </div>
                ` : ''}
                ${hasFilteredData ? html`
                    <div class="empty-state">
                        No records match the filter. Clear the filter to see all records.
                    </div>
                ` : html`
                    <table class="${this.isMetadata ? 'metadata-table' : ''}">
                        ${this.isMetadata ? this._renderMetadataTable() : this._renderRegularTable()}
                    </table>
                `}
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
                ${(this._filteredRows.length > 0 ? this._filteredRows : this.rows).map(row => html`
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
                ${(this._filteredRows.length > 0 ? this._filteredRows : this.rows).map(([key, value]) => html`
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
