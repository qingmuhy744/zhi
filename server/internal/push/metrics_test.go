package push

import "testing"

func TestMetricsSnapshot(t *testing.T) {
	var metrics Metrics
	finish := metrics.begin()
	finish("200", false)
	finish = metrics.begin()
	finish("transport_error", true)

	snapshot := metrics.Snapshot()
	if snapshot.RequestsTotal != 2 {
		t.Fatalf("requests_total = %d, want 2", snapshot.RequestsTotal)
	}
	if snapshot.InFlight != 0 {
		t.Fatalf("in_flight = %d, want 0", snapshot.InFlight)
	}
	if snapshot.TimeoutsTotal != 1 {
		t.Fatalf("timeouts_total = %d, want 1", snapshot.TimeoutsTotal)
	}
	if snapshot.StatusCounts["200"] != 1 || snapshot.StatusCounts["transport_error"] != 1 {
		t.Fatalf("status_total = %#v", snapshot.StatusCounts)
	}
}
