package api

import (
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/zhi-app/zhi-server/internal/model"
	"github.com/zhi-app/zhi-server/internal/push"
	"github.com/zhi-app/zhi-server/internal/storage"
)

type ServerHandler struct {
	store  storage.Storage
	pusher push.Pusher
}

func NewServerHandler(store storage.Storage, pusher push.Pusher) *ServerHandler {
	return &ServerHandler{
		store:  store,
		pusher: pusher,
	}
}

func SetupRouter(h *ServerHandler) *gin.Engine {
	r := gin.Default()

	// CORS middleware
	r.Use(func(c *gin.Context) {
		c.Writer.Header().Set("Access-Control-Allow-Origin", "*")
		c.Writer.Header().Set("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE")
		c.Writer.Header().Set("Access-Control-Allow-Headers", "Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization")
		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(http.StatusNoContent)
			return
		}
		c.Next()
	})

	// Base endpoints
	r.GET("/ping", h.handlePing)
	r.GET("/info", h.handleInfo)
	r.POST("/register", h.handleRegister)

	// Push endpoints (Standard & Bark compatibility)
	r.POST("/push", h.handlePush)
	r.POST("/:key", h.handleKeyPost)

	// Bark GET paths:
	// /:key/:body
	// /:key/:title/:body
	// /:key/:category/:title/:body
	r.GET("/:key/:param1", h.handleKeyGet1)
	r.GET("/:key/:param1/:param2", h.handleKeyGet2)
	r.GET("/:key/:param1/:param2/:param3", h.handleKeyGet3)

	return r
}

func (h *ServerHandler) handlePing(c *gin.Context) {
	c.JSON(http.StatusOK, model.CommonResponse{
		Code:      200,
		Message:   "pong",
		Timestamp: time.Now().Unix(),
	})
}

func (h *ServerHandler) handleInfo(c *gin.Context) {
	devices, _ := h.store.ListDevices()
	c.JSON(http.StatusOK, model.CommonResponse{
		Code:    200,
		Message: "Zhi Push Server is running",
		Data: gin.H{
			"version":        "1.0.0",
			"fcm_configured": h.pusher.IsConfigured(),
			"device_count":   len(devices),
			"name":           "Zhi Push Server",
			"description":    "Bark-compatible Android push notification service powered by FCM",
		},
		Timestamp: time.Now().Unix(),
	})
}

func (h *ServerHandler) handleRegister(c *gin.Context) {
	var req model.RegisterRequest
	if err := c.ShouldBind(&req); err != nil {
		c.JSON(http.StatusBadRequest, model.CommonResponse{
			Code:      400,
			Message:   fmt.Sprintf("Invalid parameters: %v", err),
			Timestamp: time.Now().Unix(),
		})
		return
	}

	req.DeviceKey = strings.TrimSpace(req.DeviceKey)
	req.FCMToken = strings.TrimSpace(req.FCMToken)
	if req.DeviceKey == "" || req.FCMToken == "" {
		c.JSON(http.StatusBadRequest, model.CommonResponse{
			Code:      400,
			Message:   "device_key and fcm_token cannot be empty",
			Timestamp: time.Now().Unix(),
		})
		return
	}

	dev := &model.Device{
		Key:        req.DeviceKey,
		FCMToken:   req.FCMToken,
		DeviceName: req.DeviceName,
	}

	if err := h.store.SaveDevice(dev); err != nil {
		c.JSON(http.StatusInternalServerError, model.CommonResponse{
			Code:      500,
			Message:   fmt.Sprintf("Failed to save device: %v", err),
			Timestamp: time.Now().Unix(),
		})
		return
	}

	c.JSON(http.StatusOK, model.CommonResponse{
		Code:      200,
		Message:   "Device registered successfully",
		Data:      dev,
		Timestamp: time.Now().Unix(),
	})
}

func (h *ServerHandler) handlePush(c *gin.Context) {
	var req model.PushRequest
	if err := c.ShouldBind(&req); err != nil {
		c.JSON(http.StatusBadRequest, model.CommonResponse{
			Code:      400,
			Message:   fmt.Sprintf("Invalid push request: %v", err),
			Timestamp: time.Now().Unix(),
		})
		return
	}

	h.enrichFromQueryParams(c, &req)
	h.executePush(c, &req)
}

func (h *ServerHandler) handleKeyPost(c *gin.Context) {
	key := c.Param("key")
	if key == "register" || key == "ping" || key == "info" || key == "push" {
		c.Next()
		return
	}

	var req model.PushRequest
	_ = c.ShouldBind(&req)
	req.DeviceKey = key

	h.enrichFromQueryParams(c, &req)
	h.executePush(c, &req)
}

// handleKeyGet1 handles /:key/:body
func (h *ServerHandler) handleKeyGet1(c *gin.Context) {
	key := c.Param("key")
	p1 := c.Param("param1")

	req := model.PushRequest{
		DeviceKey: key,
		Body:      p1,
	}

	h.enrichFromQueryParams(c, &req)
	h.executePush(c, &req)
}

// handleKeyGet2 handles /:key/:title/:body
func (h *ServerHandler) handleKeyGet2(c *gin.Context) {
	key := c.Param("key")
	p1 := c.Param("param1")
	p2 := c.Param("param2")

	req := model.PushRequest{
		DeviceKey: key,
		Title:     p1,
		Body:      p2,
	}

	h.enrichFromQueryParams(c, &req)
	h.executePush(c, &req)
}

// handleKeyGet3 handles /:key/:category/:title/:body
func (h *ServerHandler) handleKeyGet3(c *gin.Context) {
	key := c.Param("key")
	p1 := c.Param("param1")
	p2 := c.Param("param2")
	p3 := c.Param("param3")

	req := model.PushRequest{
		DeviceKey: key,
		Category:  p1,
		Title:     p2,
		Body:      p3,
	}

	h.enrichFromQueryParams(c, &req)
	h.executePush(c, &req)
}

func (h *ServerHandler) enrichFromQueryParams(c *gin.Context, req *model.PushRequest) {
	if t := c.Query("title"); t != "" {
		req.Title = t
	}
	if b := c.Query("body"); b != "" {
		req.Body = b
	}
	if g := c.Query("group"); g != "" {
		req.Group = g
	}
	if cat := c.Query("category"); cat != "" {
		req.Category = cat
	}
	if icon := c.Query("icon"); icon != "" {
		req.Icon = icon
	}
	if img := c.Query("image"); img != "" {
		req.Image = img
	}
	if sound := c.Query("sound"); sound != "" {
		req.Sound = sound
	}
	if u := c.Query("url"); u != "" {
		req.URL = u
	}
	if cp := c.Query("copy"); cp != "" {
		req.Copy = cp
	}
	if ck := c.Query("collapse_key"); ck != "" {
		req.CollapseKey = ck
	} else if ck2 := c.Query("collapseKey"); ck2 != "" {
		req.CollapseKey = ck2
	}
	if ttlStr := c.Query("ttl"); ttlStr != "" {
		if t, err := strconv.Atoi(ttlStr); err == nil {
			req.TTL = &t
		}
	}
	if top := c.Query("topic"); top != "" {
		req.Topic = top
	}
	if act := c.Query("actions"); act != "" {
		req.Actions = act
	}
	if sn := c.Query("system_notify"); sn != "" {
		if s, err := strconv.Atoi(sn); err == nil {
			req.SystemNotify = &s
		}
	}
	if badgeStr := c.Query("badge"); badgeStr != "" {
		if b, err := strconv.Atoi(badgeStr); err == nil {
			req.Badge = b
		}
	}
	if lvl := c.Query("level"); lvl != "" {
		req.Level = lvl
	}
	if arc := c.Query("isArchive"); arc != "" {
		if a, err := strconv.Atoi(arc); err == nil {
			req.IsArchive = &a
		}
	}
	if ct := c.Query("ciphertext"); ct != "" {
		req.Ciphertext = ct
	}
	if iv := c.Query("iv"); iv != "" {
		req.IV = iv
	}
}

func (h *ServerHandler) executePush(c *gin.Context, req *model.PushRequest) {
	// Check if this is a topic broadcast push
	targetTopic := req.Topic
	if targetTopic == "" && (strings.HasPrefix(req.DeviceKey, "topic_") || strings.HasPrefix(req.DeviceKey, "group_")) {
		targetTopic = req.DeviceKey
	}

	if targetTopic == "" && req.DeviceKey == "" {
		c.JSON(http.StatusBadRequest, model.CommonResponse{
			Code:      400,
			Message:   "device_key or topic secret is required",
			Timestamp: time.Now().Unix(),
		})
		return
	}

	if req.Body == "" && req.Title == "" && req.Ciphertext == "" {
		c.JSON(http.StatusBadRequest, model.CommonResponse{
			Code:      400,
			Message:   "notification content (body, title, or ciphertext) cannot be empty",
			Timestamp: time.Now().Unix(),
		})
		return
	}

	// 1. Branch: Topic Broadcast
	if targetTopic != "" {
		if len(targetTopic) < 8 {
			c.JSON(http.StatusBadRequest, model.CommonResponse{
				Code:      400,
				Message:   "Topic secret must be at least 8 characters long for security",
				Timestamp: time.Now().Unix(),
			})
			return
		}

		if err := h.pusher.SendTopic(c.Request.Context(), targetTopic, req); err != nil {
			c.JSON(http.StatusInternalServerError, model.CommonResponse{
				Code:      500,
				Message:   fmt.Sprintf("Failed to broadcast topic message: %v", err),
				Timestamp: time.Now().Unix(),
			})
			return
		}

		c.JSON(http.StatusOK, model.CommonResponse{
			Code:      200,
			Message:   fmt.Sprintf("Topic broadcast succeeded for [%s]", targetTopic),
			Timestamp: time.Now().Unix(),
		})
		return
	}

	// 2. Branch: Direct Device Push
	device, err := h.store.GetDevice(req.DeviceKey)
	if err != nil {
		c.JSON(http.StatusInternalServerError, model.CommonResponse{
			Code:      500,
			Message:   fmt.Sprintf("Database error: %v", err),
			Timestamp: time.Now().Unix(),
		})
		return
	}

	if device == nil {
		c.JSON(http.StatusNotFound, model.CommonResponse{
			Code:      404,
			Message:   fmt.Sprintf("Device key '%s' not found. Please register first.", req.DeviceKey),
			Timestamp: time.Now().Unix(),
		})
		return
	}

	if err := h.pusher.Send(c.Request.Context(), device.FCMToken, req); err != nil {
		c.JSON(http.StatusInternalServerError, model.CommonResponse{
			Code:      500,
			Message:   fmt.Sprintf("Failed to push message: %v", err),
			Timestamp: time.Now().Unix(),
		})
		return
	}

	c.JSON(http.StatusOK, model.CommonResponse{
		Code:      200,
		Message:   "success",
		Timestamp: time.Now().Unix(),
	})
}
