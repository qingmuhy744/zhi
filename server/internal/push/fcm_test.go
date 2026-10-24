package push

import (
	"testing"

	"github.com/zhi-app/zhi-server/internal/model"
)

func TestSystemNotificationChannelIDMatchesAndroidChannels(t *testing.T) {
	tests := []struct {
		name  string
		input model.PushRequest
		want  string
	}{
		{name: "default", want: defaultNotificationChannelID},
		{name: "urgent", input: model.PushRequest{Level: "timeSensitive"}, want: urgentNotificationChannelID},
		{name: "group", input: model.PushRequest{Group: "ops"}, want: "zhi_group_ops"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := systemNotificationChannelID(&tt.input); got != tt.want {
				t.Fatalf("systemNotificationChannelID() = %q, want %q", got, tt.want)
			}
		})
	}
}
