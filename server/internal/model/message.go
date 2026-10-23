package model

import "time"

// PushRequest represents a push notification request
type PushRequest struct {
	DeviceKey string `json:"device_key,omitempty" form:"device_key"`
	Title     string `json:"title,omitempty" form:"title"`
	Body      string `json:"body,omitempty" form:"body"`
	Group     string `json:"group,omitempty" form:"group"`
	Category  string `json:"category,omitempty" form:"category"` // alias for group
	Icon      string `json:"icon,omitempty" form:"icon"`
	Sound     string `json:"sound,omitempty" form:"sound"`
	URL       string `json:"url,omitempty" form:"url"`
	Copy      string `json:"copy,omitempty" form:"copy"`
	Badge     int    `json:"badge,omitempty" form:"badge"`
	Level     string `json:"level,omitempty" form:"level"` // active, timeSensitive, passive
	IsArchive *int   `json:"isArchive,omitempty" form:"isArchive"`
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
