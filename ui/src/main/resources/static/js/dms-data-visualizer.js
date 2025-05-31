import { LitElement, html, css } from 'https://cdn.jsdelivr.net/npm/lit@3.3.0/+esm';

class DmsDataVisualizer extends LitElement {
    static properties = {
        dataUrl: { type: String },
        renderAs: { type: String },
        title: { type: String },
        description: { type: String },
        _chartData: { type: Object },
        _errorMessage: { type: String },
        _loading: { type: Boolean },
        _showTable: { type: Boolean, state: true },
        _showMetadata: { type: Boolean, state: true },
    };

    static styles = css`
    :host {
      display: block;
      padding: 16px;
      font-family: sans-serif;
    }
    .container {
      margin-top: 16px;
    }
    .table-container, .metadata-table-container {
      overflow-x: auto;
      margin-top: 16px;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 16px;
      border: 1px solid #ddd;
    }
    th,
    td {
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
        width: 150px; /* Adjust as needed */
        background-color: #f2f2f2;
    }
    .metadata-table pre {
        white-space: pre-wrap; /* CSS3 */
        white-space: -moz-pre-wrap; /* Mozilla, since 1999 */
        white-space: -pre-wrap; /* Opera 4-6 */
        white-space: -o-pre-wrap; /* Opera 7 */
        word-wrap: break-word; /* Internet Explorer 5.5+ */
        margin: 0;
        font-family: monospace;
    }
    .chart-container {
      width: 100%;
      height: 300px; /* Adjust as needed */
      margin-top: 16px;
      position: relative; /* Make sure the container is a positioning context */
    }
    .error {
      color: red;
      margin-top: 16px;
    }
    .loading {
      margin-top: 16px;
      display: flex;
      align-items: center;
    }
    .loading-spinner {
      border: 4px solid rgba(0, 0, 0, 0.1);
      border-top: 4px solid #3498db;
      border-radius: 50%;
      width: 20px;
      height: 20px;
      animation: spin 2s linear infinite;
      margin-right: 8px;
    }
    .buttons-container button {
        margin-right: 8px;
        margin-top: 12px;
    }
    @keyframes spin {
      0% {
        transform: rotate(0deg);
      }
      100% {
        transform: rotate(360deg);
      }
    }
  `;

    constructor() {
        super();
        this.dataUrl = '';
        this.renderAs = 'table';
        this.title = '';
        this.description = '';
        this._chartData = null;
        this._errorMessage = '';
        this._loading = false;
        this._showTable = false;
        this._showMetadata = false;
    }

    connectedCallback() {
        super.connectedCallback();
        this._fetchData();
    }

    async _fetchData() {
        if (!this.dataUrl) {
            this._errorMessage = 'No data URL provided.';
            return;
        }

        this._loading = true;
        this._errorMessage = ''; // Clear any previous error
        try {
            const response = await fetch(this.dataUrl);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const data = await response.json();
            this._chartData = data.data; // Assuming the full response is { "data": { actual_data_object } }
        } catch (error) {
            console.error('Error fetching data:', error);
            this._errorMessage = 'Failed to fetch data.';
        } finally {
            this._loading = false;
            this.requestUpdate(); // Ensure re-render after fetch
        }
    }

    render() {
        return html`
      <div class="container">
        ${this.title ? html`<h2>${this.title}</h2>` : ''}
        ${this.description ? html`<p>${this.description}</p>` : ''}
        ${this._loading
            ? html`
              <div class="loading">
                <div class="loading-spinner"></div>
                Loading ${this.title ? this.title : 'data'}...
              </div>
            `
            : ''}
        ${this._errorMessage
            ? html`<div class="error">${this._errorMessage}</div>`
            : this._renderContent()}
      </div>
    `;
    }

    _renderContent() {
        if (!this._chartData) {
            return html``; // Or a placeholder
        }

        let contentHtml;
        if (this.renderAs === 'table') {
            contentHtml = this._renderTable();
        } else if (this.renderAs === 'line') {
            contentHtml = this._renderLineChart();
        } else {
            contentHtml = html`<div class="error">
        Unsupported render mode: ${this.renderAs}. Use 'table' or 'line'.
      </div>`;
        }

        const toggleMetadata = () => {
            this._showMetadata = !this._showMetadata;
            this.requestUpdate();
        };

        return html`
            ${contentHtml}
            <div class="buttons-container">
                ${this._chartData.metadata ? html`
                    <button @click="${toggleMetadata}">
                        ${this._showMetadata ? 'Hide' : 'Show'} Metadata
                    </button>
                ` : ''}
            </div>
            ${this._showMetadata ? this._renderMetadataTable() : ''}
        `;
    }

    _renderMetadataTable() {
        if (!this._chartData || !this._chartData.metadata) {
            return html`<div class="error">No metadata available.</div>`;
        }

        const metadataEntries = Object.entries(this._chartData.metadata);

        return html`
            <div class="metadata-table-container">
                <table class="metadata-table">
                    <tbody>
                        ${metadataEntries.map(([key, value]) => html`
                            <tr>
                                <td>${key}</td>
                                <td>${key === 'SQL Query' ? html`<pre>${value}</pre>` : value}</td>
                            </tr>
                        `)}
                    </tbody>
                </table>
            </div>
        `;
    }


    _renderTable() {
        if (!this._chartData.data || !this._chartData.dataFields) {
            return html`<div class="error">
        Invalid data format for table rendering.
      </div>`;
        }

        const headers = this._chartData.dataFields.map(field => field.label || field.name);
        const rows = this._chartData.data.map(item => {
            const rowData = [];
            for (const field of this._chartData.dataFields) {
                const value = item.values[field.name];
                //  handle nulls and undefineds
                if (value === null || value === undefined) {
                    rowData.push(null);
                }
                else if (typeof value === 'number') {
                    rowData.push(value);
                } else if (typeof value === 'boolean') {
                    rowData.push(value);
                }
                else {
                    rowData.push(String(value)); // Ensure strings
                }
            }
            return rowData;
        });

        return html`
      <div class="table-container">
        <table>
          <thead>
            <tr>
              ${headers.map(header => html`<th>${header}</th>`)}
            </tr>
          </thead>
          <tbody>
            ${rows.map(
            row => html`
                <tr>
                  ${row.map(cell => html`<td>${cell === null ? 'null' : cell}</td>`)}
                </tr>
              `
        )}
          </tbody>
        </table>
      </div>
    `;
    }

    _renderLineChart() {
        if (!this._chartData.data || !this._chartData.dataFields) {
            return html`<div class="error">
    Invalid data format for chart rendering.
  </div>`;
        }

        // Basic data extraction (adjust as needed for your data structure)
        const labelsField = this._chartData.dataFields.find(
            field => field.type === 'STRING' || field.type === 'DATE'
        );
        const valueFields = this._chartData.dataFields.filter(
            field => field.type === 'NUMBER'
        );

        if (!labelsField || valueFields.length === 0) {
            return html`<div class="error">
    Data format not suitable for line chart. Needs one STRING field for
    labels and at least one NUMBER field for data.
  </div>`;
        }

        const labels = this._chartData.data.map(item => item.values[labelsField.name]);
        const datasets = valueFields.map((field, index) => ({
            label: field.label || field.name,
            data: this._chartData.data.map(item => item.values[field.name]),
            borderColor: this._getColor(index), // Basic color assignment
            fill: false,
        }));

        // Use a unique ID for the canvas
        const canvasId = `line-chart-${Math.random().toString(36).substring(7)}`;

        const toggleTable = () => {
            this._showTable = !this._showTable;
            this.requestUpdate();
        };

        // Render the canvas element and toggle button
        const chartHtml = html`
          <div class="chart-container">
            <canvas id="${canvasId}"></canvas>
          </div>
          <div class="buttons-container">
            <button @click="${toggleTable}">
              ${this._showTable ? 'Hide' : 'Show'} Raw Data
            </button>
          </div>
          ${this._showTable ? this._renderTable() : ''}
        `;

        // Use a promise to render the chart after the canvas is added to the DOM
        Promise.resolve().then(() => {
            const ctx = this.shadowRoot.getElementById(canvasId);
            if (ctx && typeof Chart !== 'undefined') {
                new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: datasets,
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            title: {
                                display: this.title ? true : false,
                                text: this.title || '',
                            }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                            },
                        },
                    },
                });
            } else if (typeof Chart === 'undefined') {
                this._errorMessage = 'Chart.js is required to render line charts.';
                this.requestUpdate();
            }
        });
        return chartHtml;
    }

    _getColor(index) {
        const colors = [
            '#3e95cd',
            '#8e5ea2',
            '#3cba9f',
            '#e8c344',
            '#4bc0c0',
            '#9966ff',
            '#ff6384'
        ];
        return colors[index % colors.length];
    }
}

customElements.define('dms-data-visualizer', DmsDataVisualizer);