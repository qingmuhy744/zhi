package model

import "time"

// PushRequest represents a push notification request
type PushRequest struct {
	DeviceKey    string `json:"device_key,omitempty" form:"device_key"`
	Topic        string `json:"topic,omitempty" form:"topic"`
	Title        string `json:"title,omitempty" form:"title"`
	Body         string `json:"body,omitempty" form:"body"`
	Group        string `json:"group,omitempty" form:"group"`
	Category     string `json:"category,omitempty" form:"category"` // alias for group
	Icon         string `json:"icon,omitempty" form:"icon"`
	Image        string `json:"image,omitempty" form:"image"` // Big picture image URL
	Sound        string `json:"sound,omitempty" form:"sound"`
	URL          string `json:"url,omitempty" form:"url"`
	Copy         string `json:"copy,omitempty" form:"copy"`
	Badge        int    `json:"badge,omitempty" form:"badge"`
	Level        string `json:"level,omitempty" form:"level"` // active, timeSensitive, passive
	CollapseKey  string `json:"collapse_key,omitempty" form:"collapse_key"`
	TTL          *int   `json:"ttl,omitempty" form:"ttl"`                     // Time to live in seconds
	Actions      string `json:"actions,omitempty" form:"actions"`             // Webhook action buttons (e.g. "同意:https://...|拒绝:https://..." or JSON array)
	SystemNotify *int   `json:"system_notify,omitempty" form:"system_notify"` // 1: attach FCM notification block; default 0: pure data-only high priority
	IsArchive    *int   `json:"isArchive,omitempty" form:"isArchive"`
	Display      *int   `json:"display,omitempty" form:"display"`       // 0: process/archive without showing a notification
	Ciphertext   string `json:"ciphertext,omitempty" form:"ciphertext"` // E2EE encrypted payload (Scheme C)
	IV           string `json:"iv,omitempty" form:"iv"`                 // Optional custom IV
}

// GetEffectiveGroup returns Group or Category
func (p *PushRequest) GetEffectiveGroup() string {
	if p.Group != "" {
		return p.Group
	}
	return p.Category
}

// Device represents a registered device
type Device struct {
	Key        string    `json:"key"`
	FCMToken   string    `json:"fcm_token"`
	DeviceName string    `json:"device_name"`
	CreatedAt  time.Time `json:"created_at"`
	UpdatedAt  time.Time `json:"updated_at"`
}

// RegisterRequest represents the payload to register or update a device
type RegisterRequest struct {
	DeviceKey  string `json:"device_key" form:"device_key" binding:"required"`
	FCMToken   string `json:"fcm_token" form:"fcm_token" binding:"required"`
	DeviceName string `json:"device_name,omitempty" form:"device_name"`
}

// CommonResponse represents standard Bark-compatible response
type CommonResponse struct {
	Code      int         `json:"code"`
	Message   string      `json:"message"`
	Data      interface{} `json:"data,omitempty"`
	Timestamp int64       `json:"timestamp"`
}
