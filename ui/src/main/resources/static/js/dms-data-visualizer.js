import { LitElement, html, css } from 'https://esm.sh/lit@3.3.0';
import { unsafeHTML } from 'https://esm.sh/lit@3.3.0/directives/unsafe-html.js';
import { DmsTableVisualizer } from './dms-table-visualizer.js';
import { DmsLineChartVisualizer } from './dms-line-chart-visualizer.js';
import { DmsBarChartVisualizer } from './dms-bar-chart-visualizer.js';
import { DmsTableBase } from './dms-table-base.js';

/**
 * DmsDataVisualizer - Main container component that handles data fetching
 * and renders the appropriate visualization based on configuration.
 *
 * @element dms-data-visualizer
 * @prop {String} dataUrl - URL to fetch JSON data from
 * @prop {String} renderAs - Visualization type: 'table', 'line', or 'bar'
 * @prop {String} title - Optional title for the visualization
 * @prop {String} description - Optional description text
 * @prop {Boolean} alwaysShowRawData - For charts, whether to always show the data table
 * @prop {Number} yAxisMin - Optional minimum value for the y-axis (for charts)
 * @prop {Boolean} filterable - Whether to enable filtering for tables
 * @prop {Boolean} paginated - Whether to enable pagination for tables
 * @prop {Number} pageSize - Number of rows per page (default: 20)
 * @prop {Number} paginateAfter - Enable pagination automatically when rows exceed this number (default: 20)
 * @prop {Boolean} horizontal - For bar charts, whether to display horizontally (default: false)
 * @prop {Boolean} stacked - For bar charts, whether bars should be stacked (default: false)
 */
class DmsDataVisualizer extends LitElement {
    static properties = {
        dataUrl: { type: String },
        renderAs: { type: String },
        title: { type: String },
        description: { type: String },
        alwaysShowRawData: { type: Boolean, state: false},
        yAxisMin: { type: Number },
        horizontal: { type: Boolean },
        stacked: { type: Boolean },
        filterable: { type: Boolean },
        paginated: { type: Boolean },
        pageSize: { type: Number },
        paginateAfter: { type: Number },
        showCount: { type: Boolean },
        _chartData: { type: Object },
        _errorMessage: { type: String },
        _loading: { type: Boolean },
        _showTable: { type: Boolean, state: true },
        _showMetadata: { type: Boolean, state: true },
    };

    static styles = css`
    :host {
      display: block;
      padding-bottom: 16px;
      font-family: sans-serif;
    }
    .container {
      margin-top: 16px;
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
        this.yAxisMin = null;
        this.horizontal = false;
        this.stacked = false;
        this.filterable = false;
        this.paginated = false;
        this.pageSize = 20;
        this.paginateAfter = 20;
        this.alwaysShowRawData = false;
        this.showCount = false;
        this._chartData = null;
        this._errorMessage = '';
        this._loading = false;
        this._showTable = false;
        this._showMetadata = false;
    }

    _trackAction(actionName) {
        const payload = JSON.stringify({
            actionType: 'button_action',
            actionName: actionName,
            actionCategory: 'reports',
            uri: window.location.pathname
        });
        if (navigator.sendBeacon) {
            navigator.sendBeacon('/api/track', new Blob([payload], { type: 'application/json' }));
        }
    }

    connectedCallback() {
        super.connectedCallback();
        this._fetchData();
    }

    reload() {
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

    get _displayTitle() {
        if (this.showCount && this._chartData?.data != null) {
            return `${this.title} (${this._chartData.data.length} found)`;
        }
        return this.title;
    }

    render() {
        return html`
      <div class="container">
        ${this._displayTitle ? html`<h2>${this._displayTitle}</h2>` : ''}
        ${this.description ? html`<p>${unsafeHTML(this.description)}</p>` : ''}
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
            contentHtml = html`<dms-table-visualizer
                .data=${this._chartData}
                .errorMessage=${this._errorMessage}
                .filterable=${this.filterable}
                .paginated=${this.paginated}
                .pageSize=${this.pageSize}
                .paginateAfter=${this.paginateAfter}>
            </dms-table-visualizer>`;
        } else if (this.renderAs === 'line') {
            const toggleTable = () => {
                this._showTable = !this._showTable;
                this._trackAction(this._showTable ? 'Show Raw Data' : 'Hide Raw Data');
                this.requestUpdate();
            };

            // Track annotation visibility state
            this._annotationsVisible = true;

            contentHtml = html`
                <dms-line-chart-visualizer
                    .data=${this._chartData}
                    .title=${this.title}
                    .errorMessage=${this._errorMessage}
                    .yAxisMin=${this.yAxisMin}
                    @updated=${this._onChartUpdated}>
                </dms-line-chart-visualizer>

                <div class="buttons-container">
                    ${!this.alwaysShowRawData ? html`
                        <button @click="${toggleTable}">
                            ${this._showTable ? 'Hide' : 'Show'} Raw Data
                        </button>
                    ` : ''}
                    <span id="annotation-button-container"></span>
                </div>

                ${this.alwaysShowRawData || this._showTable ? html`
                    <dms-table-visualizer
                        .data=${this._chartData}
                        .errorMessage=${this._errorMessage}
                        .filterable=${this.filterable}
                        .paginated=${this.paginated}
                        .pageSize=${this.pageSize}
                        .paginateAfter=${this.paginateAfter}>
                    </dms-table-visualizer>
                ` : ''}
            `;
        } else if (this.renderAs === 'bar') {
            const toggleTable = () => {
                this._showTable = !this._showTable;
                this._trackAction(this._showTable ? 'Show Raw Data' : 'Hide Raw Data');
                this.requestUpdate();
            };

            // Track annotation visibility state
            this._annotationsVisible = true;

            contentHtml = html`
                <dms-bar-chart-visualizer
                    .data=${this._chartData}
                    .title=${this.title}
                    .errorMessage=${this._errorMessage}
                    .yAxisMin=${this.yAxisMin}
                    .horizontal=${this.horizontal}
                    .stacked=${this.stacked}
                    @updated=${this._onChartUpdated}>
                </dms-bar-chart-visualizer>

                <div class="buttons-container">
                    ${!this.alwaysShowRawData ? html`
                        <button @click="${toggleTable}">
                            ${this._showTable ? 'Hide' : 'Show'} Raw Data
                        </button>
                    ` : ''}
                    <span id="annotation-button-container"></span>
                </div>

                ${this.alwaysShowRawData || this._showTable ? html`
                    <dms-table-visualizer
                        .data=${this._chartData}
                        .errorMessage=${this._errorMessage}
                        .filterable=${this.filterable}
                        .paginated=${this.paginated}
                        .pageSize=${this.pageSize}
                        .paginateAfter=${this.paginateAfter}>
                    </dms-table-visualizer>
                ` : ''}
            `;
        } else {
            contentHtml = html`<div class="error">
                Unsupported render mode: ${this.renderAs}. Use 'table', 'line', or 'bar'.
            </div>`;
        }

        const toggleMetadata = () => {
            this._showMetadata = !this._showMetadata;
            this._trackAction(this._showMetadata ? 'Show Metadata' : 'Hide Metadata');
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

    _onChartUpdated(event) {
        console.log('Chart updated:', event.detail);

        const { chart, lineChart, barChart } = event.detail;

        // Determine which chart component we're dealing with
        const chartComponent = lineChart || barChart;

        // Expose the chart and chart component to external consumers
        // via a custom event
        this.dispatchEvent(new CustomEvent('chartRendered', {
            detail: {
                chart: chart,
                lineChart: lineChart,
                barChart: barChart
            },
            bubbles: true,
            composed: true // Allow the event to cross shadow DOM boundaries
        }));

        // Add Show/Hide Annotations button only if there are annotations
        setTimeout(() => {
            if (chartComponent && chartComponent.annotations && chartComponent.annotations.length > 0) {
                // Get the container for the annotations button
                const buttonContainer = this.shadowRoot.querySelector('#annotation-button-container');
                if (buttonContainer) {
                    // Clear any existing content
                    buttonContainer.innerHTML = '';

                    // Create the button element
                    const button = document.createElement('button');
                    button.innerText = chartComponent.showAnnotations ? 'Hide Annotations' : 'Show Annotations';
                    button.addEventListener('click', () => {
                        chartComponent.toggleAnnotations();
                        this._trackAction(chartComponent.showAnnotations ? 'Show Annotations' : 'Hide Annotations');
                        button.innerText = chartComponent.showAnnotations ? 'Hide Annotations' : 'Show Annotations';
                    });

                    // Add the button to the container
                    buttonContainer.appendChild(button);
                }
            }
        }, 100); // Small delay to ensure chart is fully rendered
    }

    _renderMetadataTable() {
        if (!this._chartData || !this._chartData.metadata) {
            return html`<div class="error">No metadata available.</div>`;
        }

        const metadataEntries = Object.entries(this._chartData.metadata);

        return html`
            <dms-table-base
                .rows=${metadataEntries}
                .isMetadata=${true}
                .filterable=${this.filterable}
                .paginated=${this.paginated}
                .pageSize=${this.pageSize}
                .paginateAfter=${this.paginateAfter}>
            </dms-table-base>
        `;
    }
}

customElements.define('dms-data-visualizer', DmsDataVisualizer);
