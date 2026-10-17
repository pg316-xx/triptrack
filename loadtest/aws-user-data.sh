#!/bin/bash
# EC2 "User data" for Amazon Linux 2023 (both the app server and the load generator).
# Installs Docker + Compose + git and raises open-file limits for many connections.
set -euxo pipefail
dnf install -y docker git
systemctl enable --now docker
usermod -aG docker ec2-user

mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-$(uname -m)" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Many WebSocket connections need many file descriptors and ephemeral ports.
cat >> /etc/security/limits.conf <<'EOF'
*  soft  nofile  65535
*  hard  nofile  65535
EOF
cat > /etc/sysctl.d/99-loadtest.conf <<'EOF'
net.ipv4.ip_local_port_range = 1024 65000
net.core.somaxconn = 4096
net.ipv4.tcp_tw_reuse = 1
EOF
sysctl --system
