package push

import (
	"sync"
	"sync/atomic"
	"time"
)

// Metrics contains the small set of counters needed to understand FCM health
// without adding a metrics dependency to the server.
type Metrics struct {
	requestsTotal    atomic.Uint64
	inFlight         atomic.Int64
	concurrencyWaits atomic.Uint64
	timeoutsTotal    atomic.Uint64
	durationCount    atomic.Uint64
	durationSumNanos atomic.Uint64
	durationMaxNanos atomic.Uint64
	statusCounts     sync.Map // map[string]*atomic.Uint64
}

// MetricsSnapshot is the JSON-safe representation exposed by the API.
type MetricsSnapshot struct {
	RequestsTotal    uint64            `json:"requests_total"`
	InFlight         int64             `json:"in_flight"`
	ConcurrencyWaits uint64            `json:"concurrency_waits_total"`
	TimeoutsTotal    uint64            `json:"timeouts_total"`
	DurationCount    uint64            `json:"duration_count"`
	DurationSumMs    float64           `json:"duration_sum_ms"`
	DurationMaxMs    float64           `json:"duration_max_ms"`
	StatusCounts     map[string]uint64 `json:"status_total"`
}

func (m *Metrics) begin() func(status string, timedOut bool) {
	m.requestsTotal.Add(1)
	m.inFlight.Add(1)
	started := time.Now()

	return func(status string, timedOut bool) {
		duration := time.Since(started)
		m.inFlight.Add(-1)
		m.durationCount.Add(1)
		m.durationSumNanos.Add(uint64(duration))
		for {
			old := m.durationMaxNanos.Load()
			if uint64(duration) <= old || m.durationMaxNanos.CompareAndSwap(old, uint64(duration)) {
				break
			}
		}
		if timedOut {
			m.timeoutsTotal.Add(1)
		}
		m.incrementStatus(status)
	}
}

func (m *Metrics) incrementStatus(status string) {
	value := &atomic.Uint64{}
	actual, loaded := m.statusCounts.LoadOrStore(status, value)
	if loaded {
		value = actual.(*atomic.Uint64)
	}
	value.Add(1)
}

func (m *Metrics) Snapshot() MetricsSnapshot {
	snapshot := MetricsSnapshot{
		RequestsTotal:    m.requestsTotal.Load(),
		InFlight:         m.inFlight.Load(),
		ConcurrencyWaits: m.concurrencyWaits.Load(),
		TimeoutsTotal:    m.timeoutsTotal.Load(),
		DurationCount:    m.durationCount.Load(),
		DurationSumMs:    float64(m.durationSumNanos.Load()) / float64(time.Millisecond),
		DurationMaxMs:    float64(m.durationMaxNanos.Load()) / float64(time.Millisecond),
		StatusCounts:     make(map[string]uint64),
	}
	m.statusCounts.Range(func(key, value any) bool {
		snapshot.StatusCounts[key.(string)] = value.(*atomic.Uint64).Load()
		return true
	})
	return snapshot
}
