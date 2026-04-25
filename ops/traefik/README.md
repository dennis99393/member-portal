# Traefik Reverse Proxy

Traefik is the reverse proxy fronting the UI and service containers. It is **not managed by CI** — it is brought up once
manually on the host and treated as infrastructure alongside the docker networks.

nginx sits in front and proxies `:80` → `:8000` on the docker host. Traefik listens on `:8000` and handles
blue-green traffic splitting internally via Docker network DNS — containers do not bind host ports.

## Host layout

```
/opt/traefik/
  traefik.yml                  # static config (copy from ops/traefik/)
  docker-compose.traefik.yml   # compose file (copy from ops/traefik/)
  dynamic/
    ui-weights.yml             # live blue/green weights (copy from ops/traefik/dynamic/)
/opt/portal/state/
  ui.live                      # current live color: "blue" or "green"
  service.live                 # current live color: "blue" or "green"
```

## Start / stop

```bash
# Start (only needed once, or after host reboots)
cd /opt/traefik
docker compose -f docker-compose.traefik.yml up -d

# Stop
docker compose -f docker-compose.traefik.yml down

# Reload config without restart (static config changes require restart; dynamic config reloads automatically)
docker compose -f docker-compose.traefik.yml restart
```

## Dashboard

The Traefik dashboard is available at `http://localhost:8082` on the host (not exposed externally).

```bash
# SSH tunnel to view dashboard locally
ssh -L 8082:localhost:8082 <docker-host>
# Then open http://localhost:8082 in your browser
```

Useful dashboard views:

- **HTTP Routers** — confirms the `ui` router is active and pointing to `ui-weighted@file`
- **HTTP Services** — shows `ui-weighted` and its weighted backends with current weights
- **Health** — shows healthcheck status for each backend

## Inspecting from the CLI

```bash
# Confirm Traefik is running and healthy
docker ps --filter name=traefik
docker inspect traefik --format '{{.State.Health.Status}}'
  
# Tail access log
docker logs traefik -f --tail 50

# Check current blue/green weights
cat /opt/traefik/dynamic/ui-weights.yml

# Check which color is live
cat /opt/portal/state/ui.live
cat /opt/portal/state/service.live
```

## Updating weights manually

If you ever need to manually shift traffic (e.g., emergency rollback):

```bash
# Roll back to blue
cat > /opt/traefik/dynamic/ui-weights.yml <<EOF
http:
  services:
    ui-weighted:
      weighted:
        services:
          - name: "ui-blue@docker"
            weight: 100
          - name: "ui-green@docker"
            weight: 0
EOF
# Traefik reloads automatically within ~1s
```

## Updating config files

If `traefik.yml`, `docker-compose.traefik.yml`, or `ui-weights.yml` change in the repo, copy them to the host:

```bash
scp ops/traefik/traefik.yml              <docker-host>:/tmp/traefik.yml
scp ops/traefik/docker-compose.traefik.yml <docker-host>:/tmp/docker-compose.traefik.yml
scp ops/traefik/dynamic/ui-weights.yml   <docker-host>:/tmp/ui-weights.yml

ssh <docker-host> "sudo mv /tmp/traefik.yml /opt/traefik/ && \
                   sudo mv /tmp/docker-compose.traefik.yml /opt/traefik/ && \
                   sudo mv /tmp/ui-weights.yml /opt/traefik/dynamic/"

# Static config changes require a restart; dynamic changes are picked up automatically
cd /opt/traefik && docker compose -f docker-compose.traefik.yml restart
```

    