// ES-module singleton — all dms-member-card instances on a page share this state.
// Cards that connect within the same microtask tick are batched into one POST.

const cache = new Map();        // username -> MemberData
const pending = new Map();      // username -> [{resolve, reject}]
let flushScheduled = false;

async function flushBatch() {
    flushScheduled = false;
    if (pending.size === 0) return;

    const resolvers = new Map(pending);
    pending.clear();

    try {
        const response = await fetch('/backend-api/members/batch', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ usernames: [...resolvers.keys()] }),
        });

        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const json = await response.json();
        const data = json.data ?? {};

        for (const [username, callbacks] of resolvers) {
            const member = data[username] ?? null;
            if (member) cache.set(username, member);
            for (const { resolve } of callbacks) resolve(member);
        }
    } catch (err) {
        for (const callbacks of resolvers.values()) {
            for (const { reject } of callbacks) reject(err);
        }
    }
}

export function fetchMember(username) {
    if (cache.has(username)) return Promise.resolve(cache.get(username));

    return new Promise((resolve, reject) => {
        if (!pending.has(username)) pending.set(username, []);
        pending.get(username).push({ resolve, reject });

        if (!flushScheduled) {
            flushScheduled = true;
            queueMicrotask(flushBatch);
        }
    });
}
