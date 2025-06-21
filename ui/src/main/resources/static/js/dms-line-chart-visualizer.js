import { LitElement, html, css } from 'https://cdn.jsdelivr.net/npm/lit@3.3.0/+esm';

/**
 * DmsLineChartVisualizer - Component for rendering line charts from structured data.
 * Requires Chart.js to be available in the global scope.
 *
 * @element dms-line-chart-visualizer
 * @prop {Object} data - Data object with dataFields (column definitions) and data (rows)
 * @prop {String} title - Optional chart title
 * @prop {String} errorMessage - Optional error message to display
 * @prop {Number} yAxisMin - Optional minimum value for the y-axis
 * @prop {Array} annotations - Optional array of annotations to display on the chart
 * @prop {Boolean} showAnnotations - Whether to display annotations (default: true)
 */
class DmsLineChartVisualizer extends LitElement {
    static properties = {
        data: { type: Object },
        title: { type: String },
        errorMessage: { type: String },
        yAxisMin: { type: Number },
        annotations: { type: Array },
        showAnnotations: { type: Boolean },
    };

    static styles = css`
        :host {
            display: block;
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
    `;

    constructor() {
        super();
        this.data = null;
        this.title = '';
        this.errorMessage = '';
        this.yAxisMin = null;
        this.annotations = [];
        this.showAnnotations = true;
    }

    render() {
        if (this.errorMessage) {
            return html`<div class="error">${this.errorMessage}</div>`;
        }

        if (!this.data || !this.data.data || !this.data.dataFields) {
            return html`<div class="error">Invalid data format for chart rendering.</div>`;
        }

        // Basic data extraction
        const labelsField = this.data.dataFields.find(
            field => field.type === 'STRING' || field.type === 'DATE'
        );
        const valueFields = this.data.dataFields.filter(
            field => field.type === 'NUMBER'
        );

        if (!labelsField || valueFields.length === 0) {
            return html`<div class="error">
                Data format not suitable for line chart. Needs one STRING field for
                labels and at least one NUMBER field for data.
            </div>`;
        }

        // Use a unique ID for the canvas
        const canvasId = `line-chart-${Math.random().toString(36).substring(7)}`;

        // Render the canvas element
        return html`
            <div class="chart-container">
                <canvas id="${canvasId}"></canvas>
            </div>
        `;
    }

    updated() {
        this._renderChart();
    }

    _renderChart() {
        if (!this.data || !this.data.data || !this.data.dataFields) {
            return;
        }

        const labelsField = this.data.dataFields.find(
            field => field.type === 'STRING' || field.type === 'DATE'
        );
        const valueFields = this.data.dataFields.filter(
            field => field.type === 'NUMBER'
        );

        if (!labelsField || valueFields.length === 0) {
            return;
        }

        const labels = this.data.data.map(item => item.values[labelsField.name]);
        const datasets = valueFields.map((field, index) => ({
            label: field.label || field.name,
            data: this.data.data.map(item => item.values[field.name]),
            borderColor: this._getColor(index),
            fill: false,
        }));

        // Find the canvas in the shadow DOM
        const canvasElement = this.shadowRoot.querySelector('canvas');
        if (!canvasElement || typeof Chart === 'undefined') {
            this.errorMessage = typeof Chart === 'undefined'
                ? 'Chart.js is required to render line charts.'
                : 'Canvas element not found.';
            return;
        }

        // Register the annotation plugin if it exists
        if (Chart.annotation) {
            Chart.register(Chart.annotation);
        }

        // Create the chart
        const chart = new Chart(canvasElement, {
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
                    },
                    annotation: {
                        annotations: this.showAnnotations ? this.annotations || [] : [],
                        enter(ctx, event) {
                            // Change cursor to pointer when hovering over an annotation
                            ctx.chart.canvas.style.cursor = 'pointer';
                        },
                        leave(ctx, event) {
                            // Change cursor back to default when not hovering over an annotation
                            ctx.chart.canvas.style.cursor = 'default';
                        },
                        click: (ctx, event) => {
                            // Find the clicked annotation
                            if (this.annotations) {
                                for (const annotation of this.annotations) {
                                    // Check if this annotation was clicked and has a URL
                                    if (annotation === event.annotation && annotation.url) {
                                        window.open(annotation.url, '_blank');
                                        break;
                                    }
                                }
                            }
                        }
                    },
                    tooltip: {
                        callbacks: {
                            // Custom tooltips for annotations
                            label: (context) => {
                                const annotationIndex = context.dataIndex;
                                const annotation = this.annotations ? this.annotations[annotationIndex] : null;

                                if (annotation && annotation.label && annotation.label.content) {
                                    return annotation.label.content;
                                }
                                return context.formattedValue;
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        min: this.yAxisMin !== null ? this.yAxisMin : undefined,
                    },
                },
                onClick: (e, elements, chart) => {
                    // This is a fallback if the annotation plugin click doesn't work
                    const canvasPosition = Chart.helpers.getRelativePosition(e, chart);

                    // Check if any annotation was clicked
                    if (this.annotations) {
                        for (const annotation of this.annotations) {
                            if (annotation.url && this._isClickOnAnnotation(canvasPosition, annotation, chart)) {
                                window.open(annotation.url, '_blank');
                                break;
                            }
                        }
                    }
                }
            },
        });

        // Store chart instance for potential later access
        this._chart = chart;

        // Dispatch an event when the chart is rendered
        this.dispatchEvent(new CustomEvent('updated', {
            detail: {
                chart: this._chart,
                lineChart: this
            },
            bubbles: true,
            composed: true
        }));
    }

    // Helper method to check if a click is on an annotation
    _isClickOnAnnotation(clickPosition, annotation, chart) {
        if (!annotation || (!annotation.xMin && !annotation.xMax && !annotation.xValue)) {
            return false;
        }

        const xScale = chart.scales.x;
        const yScale = chart.scales.y;

        // For line annotations
        if (annotation.type === 'line' && annotation.xValue !== undefined) {
            const xPos = xScale.getPixelForValue(annotation.xValue);

            // Check if clicking on the line (generous 15px margin)
            if (Math.abs(clickPosition.x - xPos) <= 15) {
                return true;
            }

            // Check if clicking on the label (if present)
            if (annotation.label && annotation.label.display) {
                const content = annotation.label.content || '';
                // Approximate label position based on its position property
                const labelWidth = content.length * 8; // Rough estimate of label width
                const labelHeight = 25; // Rough estimate of label height

                // Position of label depends on label.position
                let labelX, labelY;

                if (annotation.label.position === 'start') {
                    // Start position is at the top of the chart
                    labelX = xPos - (labelWidth / 2);
                    labelY = chart.chartArea.top;
                } else if (annotation.label.position === 'end') {
                    labelX = xPos - (labelWidth / 2);
                    labelY = chart.chartArea.bottom - labelHeight;
                } else if (annotation.label.position === 'center') {
                    labelX = xPos - (labelWidth / 2);
                    labelY = (chart.chartArea.top + chart.chartArea.bottom) / 2 - (labelHeight / 2);
                } else {
                    // Default positioning
                    labelX = xPos - (labelWidth / 2);
                    labelY = chart.chartArea.top;
                }

                // Add padding for easier clicking (20px on each side)
                if (clickPosition.x >= labelX - 20 &&
                clickPosition.x <= labelX + labelWidth + 20 &&
                clickPosition.y >= labelY - 10 &&
                clickPosition.y <= labelY + labelHeight + 10) {
                    return true;
                }
            }
        }

        // For box annotations
        if (annotation.type === 'box' && annotation.xMin !== undefined && annotation.xMax !== undefined) {
            const xMinPos = xScale.getPixelForValue(annotation.xMin);
            const xMaxPos = xScale.getPixelForValue(annotation.xMax);
            return clickPosition.x >= xMinPos && clickPosition.x <= xMaxPos;
        }

        return false;
    }

    // Helper method to add annotations programmatically
    addAnnotation(annotationConfig) {
        if (!this.annotations) {
            this.annotations = [];
        }

        this.annotations.push(annotationConfig);

        // Re-render the chart if it exists
        if (this._chart) {
            this._chart.options.plugins.annotation.annotations = this.annotations;
            this._chart.update();
        }
    }

    // Method to toggle annotations visibility
    toggleAnnotations() {
        this.showAnnotations = !this.showAnnotations;

        // Update the chart if it exists
        if (this._chart) {
            this._chart.options.plugins.annotation.annotations = this.showAnnotations ? this.annotations : [];
            this._chart.update();
        }
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

customElements.define('dms-line-chart-visualizer', DmsLineChartVisualizer);
export { DmsLineChartVisualizer };
