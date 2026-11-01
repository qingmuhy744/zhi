# syntax=docker/dockerfile:1
# The runtime image contains only Alpine, CA certificates, tzdata, and the compiled binary.
# Go stays in the builder stage. Database, config, and the Firebase key are mounted in.
FROM golang:1.26-alpine AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN --mount=type=cache,target=/go/pkg/mod \
    go mod download

COPY . .
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o zhi-server .

FROM alpine:3.24

WORKDIR /app
RUN --mount=type=cache,target=/var/cache/apk \
    apk add --update ca-certificates tzdata

COPY --from=builder /app/zhi-server /app/zhi-server
COPY config.example.yaml /app/config.yaml

EXPOSE 8080
VOLUME ["/app/data"]

ENTRYPOINT ["/app/zhi-server"]
CMD ["-config", "config.yaml"]
