package storage

import (
	"database/sql"
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/zhi-app/zhi-server/internal/model"
	_ "modernc.org/sqlite"
)

type Storage interface {
	SaveDevice(device *model.Device) error
	GetDevice(key string) (*model.Device, error)
	DeleteDevice(key string) error
	ListDevices() ([]*model.Device, error)
	Close() error
}

type SQLiteStorage struct {
	db *sql.DB
}

func NewSQLiteStorage(dbPath string) (*SQLiteStorage, error) {
	if dbPath == "" {
		dbPath = "zhi.db"
	}

	dir := filepath.Dir(dbPath)
	if dir != "" && dir != "." {
		if err := os.MkdirAll(dir, 0755); err != nil {
			return nil, fmt.Errorf("failed to create db directory: %w", err)
		}
	}

	db, err := sql.Open("sqlite", dbPath)
	if err != nil {
		return nil, fmt.Errorf("failed to open sqlite db: %w", err)
	}

	// Optimize connection settings
	db.SetMaxOpenConns(1) // SQLite works best with 1 writer
	db.SetMaxIdleConns(1)

	s := &SQLiteStorage{db: db}
	if err := s.initSchema(); err != nil {
		db.Close()
		return nil, err
	}

	return s, nil
}

func (s *SQLiteStorage) initSchema() error {
	schema := `
	CREATE TABLE IF NOT EXISTS devices (
		key TEXT PRIMARY KEY,
		fcm_token TEXT NOT NULL,
		device_name TEXT DEFAULT '',
		created_at DATETIME NOT NULL,
		updated_at DATETIME NOT NULL
	);
	CREATE INDEX IF NOT EXISTS idx_devices_updated_at ON devices(updated_at);
	`
	_, err := s.db.Exec(schema)
	return err
}

func (s *SQLiteStorage) SaveDevice(device *model.Device) error {
	now := time.Now()
	query := `
	INSERT INTO devices (key, fcm_token, device_name, created_at, updated_at)
	VALUES (?, ?, ?, ?, ?)
	ON CONFLICT(key) DO UPDATE SET
		fcm_token = excluded.fcm_token,
		device_name = excluded.device_name,
		updated_at = excluded.updated_at;
	`
	_, err := s.db.Exec(query, device.Key, device.FCMToken, device.DeviceName, now, now)
	return err
}

func (s *SQLiteStorage) GetDevice(key string) (*model.Device, error) {
	query := `SELECT key, fcm_token, device_name, created_at, updated_at FROM devices WHERE key = ?`
	row := s.db.QueryRow(query, key)

	var d model.Device
	var createdAt, updatedAt time.Time
	err := row.Scan(&d.Key, &d.FCMToken, &d.DeviceName, &createdAt, &updatedAt)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	d.CreatedAt = createdAt
	d.UpdatedAt = updatedAt
	return &d, nil
}

func (s *SQLiteStorage) DeleteDevice(key string) error {
	query := `DELETE FROM devices WHERE key = ?`
	_, err := s.db.Exec(query, key)
	return err
}

func (s *SQLiteStorage) ListDevices() ([]*model.Device, error) {
	query := `SELECT key, fcm_token, device_name, created_at, updated_at FROM devices ORDER BY updated_at DESC`
	rows, err := s.db.Query(query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var list []*model.Device
	for rows.Next() {
		var d model.Device
		var createdAt, updatedAt time.Time
		if err := rows.Scan(&d.Key, &d.FCMToken, &d.DeviceName, &createdAt, &updatedAt); err != nil {
			return nil, err
		}
		d.CreatedAt = createdAt
		d.UpdatedAt = updatedAt
		list = append(list, &d)
	}
	return list, nil
}

func (s *SQLiteStorage) Close() error {
	return s.db.Close()
}
