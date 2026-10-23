# Multi-stage build for ultra-small container image
FROM golang:1.25-alpine AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o zhi-server .

FROM alpine:3.20

WORKDIR /app
RUN apk add --no-cache ca-certificates tzdata

COPY --from=builder /app/zhi-server /app/zhi-server
COPY config.example.yaml /app/config.yaml

EXPOSE 8080
VOLUME ["/app/data"]

ENTRYPOINT ["/app/zhi-server"]
CMD ["-config", "config.yaml"]
