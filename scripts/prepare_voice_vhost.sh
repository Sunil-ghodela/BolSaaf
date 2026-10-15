#!/bin/bash
# Prepares voice.tranquilwaters.in vhost on the voice server (HTTP-only; certbot
# will add TLS after the DNS A record exists). Mirrors the working voice config
# from /etc/nginx/sites-enabled/selfshadowwork.
set -e
CONF=/etc/nginx/sites-available/voice.tranquilwaters.in
if [ -f "$CONF" ]; then echo "vhost already exists, skipping"; exit 0; fi
cat > "$CONF" <<'NGINX'
server {
    listen 80;
    server_name voice.tranquilwaters.in;

    location = /voice/privacy {
        default_type text/html;
        alias /var/www/static-legal/privacy.html;
        add_header Cache-Control "public, max-age=3600";
    }

    location = /voice/terms {
        default_type text/html;
        alias /var/www/static-legal/terms.html;
        add_header Cache-Control "public, max-age=3600";
    }

    # Voice — status polling: light, high frequency
    location ~ ^/voice/(status|cleaned_output|health|backgrounds)/ {
        limit_req zone=voice_status burst=30 nodelay;
        client_max_body_size 1M;
        proxy_pass http://unix:/run/voice-gunicorn.sock;
        proxy_http_version 1.1;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Server $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Host $http_host;
        proxy_pass_request_headers on;
        proxy_read_timeout 60s;
        proxy_connect_timeout 30s;
    }

    # Voice — heavy upload endpoints (audio, video, reel, etc.)
    location /voice/ {
        limit_req zone=voice_upload burst=8 nodelay;
        client_max_body_size 50M;
        proxy_pass http://unix:/run/voice-gunicorn.sock:/voice/;
        proxy_http_version 1.1;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Host $http_host;
        proxy_pass_request_headers on;
        proxy_read_timeout 300s;
        proxy_connect_timeout 300s;
    }

    location ^~ /media/ {
        alias /var/www/simplelms/backend/media/;
        expires 30d;
        add_header Cache-Control "public";
        access_log off;
    }
}
NGINX
ln -sf "$CONF" /etc/nginx/sites-enabled/voice.tranquilwaters.in
nginx -t && systemctl reload nginx
echo "vhost enabled OK"
