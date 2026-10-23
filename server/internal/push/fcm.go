package push

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strconv"
	"sync"
	"time"

	"github.com/zhi-app/zhi-server/internal/model"
	"golang.org/x/oauth2"
	"golang.org/x/oauth2/google"
)

// Pusher defines the interface for pushing notifications
type Pusher interface {
	Send(ctx context.Context, fcmToken string, req *model.PushRequest) error
	IsConfigured() bool
}

type FCMPusher struct {
	projectID   string
	tokenSource oauth2.TokenSource
	httpClient  *http.Client
	isMock      bool
	mu          sync.RWMutex
}

type serviceAccountJSON struct {
	ProjectID string `json:"project_id"`
}

// NewFCMPusher creates a new FCM HTTP v1 pusher
func NewFCMPusher(serviceAccountPath string) (*FCMPusher, error) {
	if serviceAccountPath == "" {
		serviceAccountPath = os.Getenv("FCM_SERVICE_ACCOUNT_FILE")
	}

	if serviceAccountPath == "" {
		log.Println("[WARN] No Firebase Service Account specified (FCM_SERVICE_ACCOUNT_FILE is empty). Running in MOCK / TEST mode.")
		return &FCMPusher{
			isMock:     true,
			httpClient: &http.Client{Timeout: 10 * time.Second},
		}, nil
	}

	data, err := os.ReadFile(serviceAccountPath)
	if err != nil {
		log.Printf("[WARN] Firebase service account file '%s' not found (%v). Running in MOCK / TEST mode.", serviceAccountPath, err)
		log.Printf("[TIP] Place your Google Firebase Service Account JSON at '%s' for production push delivery.", serviceAccountPath)
		return &FCMPusher{
			isMock:     true,
			httpClient: &http.Client{Timeout: 10 * time.Second},
		}, nil
	}

	var sa serviceAccountJSON
	if err := json.Unmarshal(data, &sa); err != nil {
		return nil, fmt.Errorf("invalid service account JSON: %w", err)
	}
	if sa.ProjectID == "" {
		return nil, fmt.Errorf("project_id missing in service account file")
	}

	conf, err := google.JWTConfigFromJSON(data, "https://www.googleapis.com/auth/firebase.messaging")
	if err != nil {
		return nil, fmt.Errorf("failed to create JWT config: %w", err)
	}

	ctx := context.Background()
	ts := conf.TokenSource(ctx)

	log.Printf("[INFO] FCM Pusher initialized successfully for project: %s", sa.ProjectID)

	return &FCMPusher{
		projectID:   sa.ProjectID,
		tokenSource: ts,
		httpClient:  &http.Client{Timeout: 15 * time.Second},
		isMock:      false,
	}, nil
}

func (p *FCMPusher) IsConfigured() bool {
	return !p.isMock && p.projectID != ""
}

// Send sends notification to FCM
func (p *FCMPusher) Send(ctx context.Context, fcmToken string, req *model.PushRequest) error {
	if fcmToken == "" {
		return fmt.Errorf("empty fcm token")
	}

	if p.isMock {
		log.Printf("[MOCK PUSH] Would send to FCM token [%s...]: Title='%s', Body='%s', Group='%s'",
			fcmToken[:min(len(fcmToken), 10)], req.Title, req.Body, req.GetEffectiveGroup())
		return nil
	}

	// Fetch OAuth2 Bearer token
	token, err := p.tokenSource.Token()
	if err != nil {
		return fmt.Errorf("failed to obtain FCM access token: %w", err)
	}

	title := req.Title
	if title == "" {
		title = "Zhi"
	}

	// Prepare data payload for Android client to parse
	dataPayload := map[string]string{
		"title":     title,
		"body":      req.Body,
		"group":     req.GetEffectiveGroup(),
		"icon":      req.Icon,
		"sound":     req.Sound,
		"url":       req.URL,
		"copy":      req.Copy,
		"badge":     strconv.Itoa(req.Badge),
		"level":     req.Level,
		"timestamp": strconv.FormatInt(time.Now().UnixMilli(), 10),
	}
	if req.IsArchive != nil {
		dataPayload["isArchive"] = strconv.Itoa(*req.IsArchive)
	}

	// Channel ID matching group or level
	channelID := "zhi_default"
	if req.Level == "timeSensitive" {
		channelID = "zhi_urgent"
	} else if req.GetEffectiveGroup() != "" {
		channelID = "zhi_group_" + req.GetEffectiveGroup()
	}

	fcmMsg := map[string]interface{}{
		"message": map[string]interface{}{
			"token": fcmToken,
			"notification": map[string]string{
				"title": title,
				"body":  req.Body,
			},
			"data": dataPayload,
			"android": map[string]interface{}{
				"priority": "HIGH",
				"notification": map[string]interface{}{
					"channel_id": channelID,
					"sound":      "default",
					"default_vibrate_timings": true,
				},
			},
		},
	}

	bodyBytes, err := json.Marshal(fcmMsg)
	if err != nil {
		return fmt.Errorf("failed to marshal FCM message: %w", err)
	}

	url := fmt.Sprintf("https://fcm.googleapis.com/v1/projects/%s/messages:send", p.projectID)
	httpReq, err := http.NewRequestWithContext(ctx, "POST", url, bytes.NewReader(bodyBytes))
	if err != nil {
		return fmt.Errorf("failed to create http request: %w", err)
	}

	httpReq.Header.Set("Authorization", "Bearer "+token.AccessToken)
	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := p.httpClient.Do(httpReq)
	if err != nil {
		return fmt.Errorf("FCM request error: %w", err)
	}
	defer resp.Body.Close()

	respBytes, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return fmt.Errorf("FCM server responded with %d: %s", resp.StatusCode, string(respBytes))
	}

	log.Printf("[INFO] FCM push succeeded for token [%s...]", fcmToken[:min(len(fcmToken), 10)])
	return nil
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
