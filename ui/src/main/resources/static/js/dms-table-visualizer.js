import { LitElement, html, css } from 'https://cdn.jsdelivr.net/npm/lit@3.3.0/+esm';
import { DmsTableBase } from './dms-table-base.js';

/**
 * DmsTableVisualizer - Component for displaying data in a tabular format.
 *
 * @element dms-table-visualizer
 * @prop {Object} data - Data object containing dataFields (column definitions) and data (rows)
 * @prop {String} errorMessage - Optional error message to display
 * @prop {Boolean} filterable - Whether to enable filtering for the table
 * @prop {Boolean} paginated - Whether to enable pagination for the table
 * @prop {Number} pageSize - Number of rows per page (default: 20)
 * @prop {Number} paginateAfter - Enable pagination automatically when rows exceed this number (default: 20)
 */
class DmsTableVisualizer extends LitElement {
    static properties = {
        data: { type: Object },
        errorMessage: { type: String },
        filterable: { type: Boolean },
        paginated: { type: Boolean },
        pageSize: { type: Number },
        paginateAfter: { type: Number }
    };

    static styles = css`
        :host {
            display: block;
        }
        .error {
            color: red;
            margin-top: 16px;
        }
    `;

    constructor() {
        super();
        this.data = null;
        this.errorMessage = '';
        this.filterable = false;
        this.paginated = false;
        this.pageSize = 20;
        this.paginateAfter = 20;
    }

    render() {
        if (this.errorMessage) {
            return html`<div class="error">${this.errorMessage}</div>`;
        }

        if (!this.data || !this.data.data || !this.data.dataFields) {
            return html`<div class="error">Invalid data format for table rendering.</div>`;
        }

        const headers = this.data.dataFields.map(field => field.label || field.name);
        const rows = this.data.data.map(item => {
            const rowData = [];
            for (const field of this.data.dataFields) {
                const value = item.values[field.name];
                // Handle nulls and undefineds
                if (value === null || value === undefined) {
                    rowData.push(null);
                } else if (typeof value === 'number' || typeof value === 'boolean') {
                    rowData.push(value);
                } else {
                    rowData.push(String(value)); // Ensure strings
                }
            }
            return rowData;
        });

        return html`
            <dms-table-base
                .headers=${headers}
                .rows=${rows}
                .filterable=${this.filterable}
                .paginated=${this.paginated}
                .pageSize=${this.pageSize}
                .paginateAfter=${this.paginateAfter}>
            </dms-table-base>
        `;
    }
}

customElements.define('dms-table-visualizer', DmsTableVisualizer);
export { DmsTableVisualizer };
