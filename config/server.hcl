datacenter       = "dc1"
server           = true
bootstrap_expect = 1

# Słuchaj na wszystkich interfejsach (host będzie mógł wejść na UI/API)
client_addr = "0.0.0.0"

# Gdzie trzymamy stan Rafta, snapshoty KV itd.
data_dir = "/consul/data"

# UI przeglądarkowe
ui_config {
  enabled = true
}

# ACL w trybie dev
acl {
  enabled                  = false
//   enabled                  = true
//   default_policy           = "deny"
//   enable_token_persistence = true
}

# Logi trochę ciszej/głośniej wg uznania
log_level = "INFO"
