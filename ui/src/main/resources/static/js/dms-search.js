class DMSSearch extends HTMLElement {
    constructor() {
        super();
        this.attachShadow({ mode: 'open' });
        this.members = [];
        this.maxResults = 10; // Default value
        this.dataFetched = false;
    }

    static get observedAttributes() {
        return ['max-results'];
    }

    attributeChangedCallback(name, oldValue, newValue) {
        if (name === 'max-results') {
            this.maxResults = parseInt(newValue, 10);
        }
    }

    connectedCallback() {
        this.render();
        this.setupEventListeners();
    }

    render() {
        this.shadowRoot.innerHTML = `
        <style>
        :host {
          --dms-search-bg-color: white;
          --dms-search-text-color: black;
          --dms-search-highlight-color: #f0f0f0;
        }
        .dms-search-container {
          font-family: Arial, sans-serif;
          max-width: 600px;
          min-width: 300px;
          position: relative;
        }
        .search-input-container {
          display: flex;
          align-items: center;
          border: 1px solid #dfe1e5;
          border-radius: 24px;
          padding: 5px 15px;
          box-shadow: 0 1px 6px rgba(32,33,36,0.28);
          background-color: var(--dms-search-bg-color);
          transition: border-radius 0.3s ease;
        }
        .search-input-container.results-visible {
          border-radius: 24px 24px 0 0;
          border-bottom: none;
        }
        input {
          flex-grow: 1;
          border: none;
          outline: none;
          font-size: 16px;
          padding: 10px 0;
          background-color: transparent;
        }
        .clear-button {
          background: none;
          border: none;
          cursor: pointer;
          font-size: 18px;
          color: #5f6368;
          display: none;
        }
        .results-container {
          width: 100%;
          box-sizing: border-box;
          border: 1px solid #dfe1e5;
          border-top: none;
          border-radius: 0 0 24px 24px;
          box-shadow: 0 4px 6px rgba(32,33,36,0.28);
          background-color: var(--dms-search-bg-color);
          display: none;
          z-index: 1000;
        }
        .results-list {
          list-style-type: none;
          padding: 0;
          margin: 0;
          max-height: 300px;
          overflow-y: auto;
        }
        .results-list li {
          padding: 10px 15px;
          cursor: pointer;
        }
        .results-list li:hover {
          background-color: var(--dms-search-highlight-color);
        }
        .results-list li:last-child {
          border-radius: 0 0 24px 24px;
        }
        .loading-indicator {
          font-style: italic;
          text-align: center;
          padding: 10px;
        }
      </style>
      <div class="dms-search-container">
      <div class="search-input-container">
        <input type="text" placeholder="Search members...">
        <button class="clear-button">✕</button>
      </div>
      <div class="results-container">
        <div class="loading-indicator" style="display: none;">Loading...</div>
        <ul class="results-list"></ul>
      </div>
    </div>
    `;
    }

    setupEventListeners() {
        const input = this.shadowRoot.querySelector('input');
        const clearButton = this.shadowRoot.querySelector('.clear-button');

        input.addEventListener('focus', () => this.handleFocus());
        input.addEventListener('input', this.debounce(() => this.handleSearch(), 300));
        clearButton.addEventListener('click', () => this.clearSearch());
        input.addEventListener('input', () => this.toggleClearButton());
        document.addEventListener('click', (e) => this.handleOutsideClick(e));
    }

    async handleFocus() {
        if (!this.dataFetched) {
            await this.fetchMembers();
            this.dataFetched = true;
        }
    }

    async fetchMembers() {
        try {
            // Replace this URL with your actual API endpoint
            const response = await fetch('/search-preload');
            let json = await response.json();
            this.members = json["data"];
        } catch (error) {
            console.error('Error fetching search preload:', error);
        }
    }

    handleSearch() {
        const input = this.shadowRoot.querySelector('input');
        const query = input.value.toLowerCase();
        const results = this.members.filter(member =>
        (member.displayName?.toLowerCase().includes(query) || '') ||
        (member.username?.toLowerCase().includes(query) || '') ||
        (member.discourseUsername?.toLowerCase().includes(query) || '')
        );
        this.displayResults(results);
    }

    displayResults(results) {
        const resultsList = this.shadowRoot.querySelector('.results-list');
        const resultsContainer = this.shadowRoot.querySelector('.results-container');
        const searchInputContainer = this.shadowRoot.querySelector('.search-input-container');

        resultsList.innerHTML = '';

        if (results.length > 0) {
            results.slice(0, this.maxResults).forEach(member => {
                const li = document.createElement('li');
                li.textContent = `${member.displayName} @${member.username}`;
                li.addEventListener('click', () => this.handleMemberClick(member));
                resultsList.appendChild(li);
            });
            resultsContainer.style.display = 'block';
            searchInputContainer.classList.add('results-visible');
        } else {
            resultsContainer.style.display = 'none';
            searchInputContainer.classList.remove('results-visible');
        }
    }

    handleMemberClick(member) {
        console.log(`Clicked on member: ${member.username}`);
        window.location.href = `/profile/@${member.username}`;
    }

    clearSearch() {
        const input = this.shadowRoot.querySelector('input');
        input.value = '';
        this.toggleClearButton();
        this.displayResults([]);
    }

    toggleClearButton() {
        const input = this.shadowRoot.querySelector('input');
        const clearButton = this.shadowRoot.querySelector('.clear-button');
        clearButton.style.display = input.value ? 'block' : 'none';
    }

    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
}

customElements.define('dms-search', DMSSearch);