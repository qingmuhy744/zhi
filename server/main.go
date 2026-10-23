package main

import (
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/zhi-app/zhi-server/internal/api"
	"github.com/zhi-app/zhi-server/internal/config"
	"github.com/zhi-app/zhi-server/internal/push"
	"github.com/zhi-app/zhi-server/internal/storage"
)

func main() {
	configPath := flag.String("config", "config.yaml", "Path to config YAML file")
	flag.Parse()

	cfg, err := config.LoadConfig(*configPath)
	if err != nil {
		log.Printf("[WARN] Failed to load config from %s: %v. Using defaults.", *configPath, err)
	}

	log.Printf("[INFO] Starting Zhi Push Server on port %s...", cfg.Port)

	// Initialize SQLite Database
	store, err := storage.NewSQLiteStorage(cfg.DatabasePath)
	if err != nil {
		log.Fatalf("[FATAL] Failed to initialize SQLite storage at %s: %v", cfg.DatabasePath, err)
	}
	defer store.Close()
	log.Printf("[INFO] SQLite storage initialized at %s", cfg.DatabasePath)

	// Initialize FCM Pusher
	pusher, err := push.NewFCMPusher(cfg.FCMServiceAccount)
	if err != nil {
		log.Fatalf("[FATAL] Failed to initialize FCM pusher: %v", err)
	}

	// Initialize HTTP Router
	handler := api.NewServerHandler(store, pusher)
	router := api.SetupRouter(handler)

	// Graceful shutdown setup
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		addr := fmt.Sprintf(":%s", cfg.Port)
		if err := router.Run(addr); err != nil {
			log.Fatalf("[FATAL] Server failed: %v", err)
		}
	}()

	log.Printf("[INFO] Zhi Server is ready and listening on :%s", cfg.Port)
	<-quit
	log.Println("[INFO] Shutting down Zhi Server...")
}
