package push

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"strconv"
	"sync"
	"time"

	"github.com/zhi-app/zhi-server/internal/model"
	"golang.org/x/oauth2"
	"golang.org/x/oauth2/google"
)

const maxConcurrentFCMSends = 24

// Pusher defines the interface for pushing notifications
type Pusher interface {
	Send(ctx context.Context, fcmToken string, req *model.PushRequest) error
	SendTopic(ctx context.Context, topic string, req *model.PushRequest) error
	IsConfigured() bool
}

// MetricsProvider exposes delivery metrics without expanding the Pusher API.
type MetricsProvider interface {
	Metrics() MetricsSnapshot
}

type FCMPusher struct {
	projectID   string
	tokenSource oauth2.TokenSource
	httpClient  *http.Client
	isMock      bool
	mu          sync.RWMutex
	semaphore   chan struct{}
	metrics     Metrics
}

// These IDs are part of the client/server notification contract. The Android
// app creates the same channels before it receives a system-notification
// fallback, so FCM can route the fallback into the user's configured channel
// instead of creating its "Miscellaneous" fallback channel.
const (
	defaultNotificationChannelID = "zhi_channel_default_v4"
	urgentNotificationChannelID  = "zhi_channel_urgent_v4"
)

func systemNotificationChannelID(req *model.PushRequest) string {
	channelID := defaultNotificationChannelID
	if req.Level == "timeSensitive" {
		return urgentNotificationChannelID
	}
	if group := req.GetEffectiveGroup(); group != "" {
		return "zhi_group_" + group
	}
	return channelID
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
			semaphore:  make(chan struct{}, maxConcurrentFCMSends),
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
		semaphore:   make(chan struct{}, maxConcurrentFCMSends),
	}, nil
}

func (p *FCMPusher) IsConfigured() bool {
	return !p.isMock && p.projectID != ""
}

func (p *FCMPusher) Metrics() MetricsSnapshot {
	return p.metrics.Snapshot()
}

// Send sends notification to a single FCM device token
func (p *FCMPusher) Send(ctx context.Context, fcmToken string, req *model.PushRequest) error {
	if fcmToken == "" {
		return fmt.Errorf("empty fcm token")
	}
	return p.dispatch(ctx, "token", fcmToken, req)
}

// SendTopic sends notification to an FCM topic
func (p *FCMPusher) SendTopic(ctx context.Context, topic string, req *model.PushRequest) error {
	if topic == "" {
		return fmt.Errorf("empty topic")
	}
	return p.dispatch(ctx, "topic", topic, req)
}

func (p *FCMPusher) dispatch(ctx context.Context, targetKey, targetVal string, req *model.PushRequest) error {
	select {
	case p.semaphore <- struct{}{}:
	default:
		p.metrics.concurrencyWaits.Add(1)
		select {
		case p.semaphore <- struct{}{}:
		case <-ctx.Done():
			return ctx.Err()
		}
	}
	defer func() { <-p.semaphore }()

	finish := p.metrics.begin()
	status := "transport_error"
	timedOut := false
	defer func() { finish(status, timedOut) }()

	if p.isMock {
		if req.Ciphertext != "" {
			log.Printf("[MOCK PUSH - E2EE] Would send to FCM %s [%s]: Ciphertext='%s...' (Length=%d)",
				targetKey, targetVal, req.Ciphertext[:min(len(req.Ciphertext), 16)], len(req.Ciphertext))
		} else {
			log.Printf("[MOCK PUSH] Would send to FCM %s [%s]: Title='%s', Body='%s', Group='%s', Image='%s', TTL=%v",
				targetKey, targetVal, req.Title, req.Body, req.GetEffectiveGroup(), req.Image, req.TTL)
		}
		status = "mock"
		return nil
	}

	// Fetch OAuth2 Bearer token
	token, err := p.tokenSource.Token()
	if err != nil {
		status = "auth_error"
		timedOut = isTimeout(err)
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
		"image":     req.Image,
		"sound":     req.Sound,
		"url":       req.URL,
		"copy":      req.Copy,
		"badge":     strconv.Itoa(req.Badge),
		"level":     req.Level,
		"timestamp": strconv.FormatInt(time.Now().UnixMilli(), 10),
	}
	if req.CollapseKey != "" {
		dataPayload["collapse_key"] = req.CollapseKey
	}
	if req.Actions != "" {
		dataPayload["actions"] = req.Actions
	}
	if req.IsArchive != nil {
		dataPayload["isArchive"] = strconv.Itoa(*req.IsArchive)
	}
	if req.Ciphertext != "" {
		dataPayload["ciphertext"] = req.Ciphertext
	}
	if req.IV != "" {
		dataPayload["iv"] = req.IV
	}

	androidConfig := map[string]interface{}{
		"priority": "HIGH",
	}
	if req.CollapseKey != "" {
		androidConfig["collapse_key"] = req.CollapseKey
	}
	if req.TTL != nil && *req.TTL >= 0 {
		androidConfig["ttl"] = fmt.Sprintf("%ds", *req.TTL)
	}

	msgPayload := map[string]interface{}{
		targetKey: targetVal,
		"data":    dataPayload,
		"android": androidConfig,
	}

	// Optional fallback: if system_notify=1 is explicitly requested, attach FCM system notification block
	if req.SystemNotify != nil && *req.SystemNotify == 1 {
		channelID := systemNotificationChannelID(req)

		notificationTitle := title
		notificationBody := req.Body
		if req.Ciphertext != "" {
			if req.Title == "" {
				notificationTitle = "Zhi"
			}
			notificationBody = "收到一条端到端加密消息"
		}

		androidNotification := map[string]interface{}{
			"channel_id":              channelID,
			"sound":                   "default",
			"default_vibrate_timings": true,
		}
		if req.Image != "" {
			androidNotification["image"] = req.Image
		}
		androidConfig["notification"] = androidNotification

		notificationMap := map[string]string{
			"title": notificationTitle,
			"body":  notificationBody,
		}
		if req.Image != "" {
			notificationMap["image"] = req.Image
		}
		msgPayload["notification"] = notificationMap
	}

	fcmMsg := map[string]interface{}{
		"message": msgPayload,
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
		timedOut = isTimeout(err)
		return fmt.Errorf("FCM request error: %w", err)
	}
	defer resp.Body.Close()

	respBytes, _ := io.ReadAll(resp.Body)
	status = strconv.Itoa(resp.StatusCode)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return fmt.Errorf("FCM server responded with %d: %s", resp.StatusCode, string(respBytes))
	}

	log.Printf("[INFO] FCM push succeeded for %s [%s]", targetKey, targetVal)
	return nil
}

func isTimeout(err error) bool {
	if errors.Is(err, context.DeadlineExceeded) {
		return true
	}
	var netErr net.Error
	return errors.As(err, &netErr) && netErr.Timeout()
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
