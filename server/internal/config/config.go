package config

import (
	"os"

	"gopkg.in/yaml.v3"
)

type Config struct {
	Port               string `yaml:"port"`
	DatabasePath       string `yaml:"database_path"`
	FCMServiceAccount  string `yaml:"fcm_service_account"`
	DefaultServerURL   string `yaml:"default_server_url"`
}

func LoadConfig(path string) (*Config, error) {
	cfg := &Config{
		Port:              "8080",
		DatabasePath:      "data/zhi.db",
		FCMServiceAccount: "firebase-service-account.json",
		DefaultServerURL:  "https://push.qingmuhy.com",
	}

	if path != "" {
		if data, err := os.ReadFile(path); err == nil {
			_ = yaml.Unmarshal(data, cfg)
		}
	}

	// Environment variable overrides
	if p := os.Getenv("PORT"); p != "" {
		cfg.Port = p
	}
	if db := os.Getenv("DATABASE_PATH"); db != "" {
		cfg.DatabasePath = db
	}
	if fcm := os.Getenv("FCM_SERVICE_ACCOUNT_FILE"); fcm != "" {
		cfg.FCMServiceAccount = fcm
	}
	if url := os.Getenv("SERVER_URL"); url != "" {
		cfg.DefaultServerURL = url
	}

	return cfg, nil
}
