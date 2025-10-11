#!/bin/bash

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

QUEUE_SERVICE="http://localhost:8083"
EVENT_ID=1
CLIENT_ID="alice"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Queue Service 테스트${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 1. 대기열 진입
echo -e "${GREEN}[1] 대기열 진입 (Enqueue)${NC}"
echo "POST $QUEUE_SERVICE/queue/$EVENT_ID/enqueue?clientId=$CLIENT_ID"
echo ""
ENQUEUE_RESULT=$(curl -s -X POST "$QUEUE_SERVICE/queue/$EVENT_ID/enqueue?clientId=$CLIENT_ID")
echo "$ENQUEUE_RESULT" | jq '.'
echo ""

# 순번 추출
POSITION=$(echo "$ENQUEUE_RESULT" | jq -r '.position')
echo -e "${YELLOW}현재 대기 순번: $POSITION${NC}"
echo ""

# 2. 대기 상태 확인 (폴링)
echo -e "${GREEN}[2] 대기 상태 확인 (Status)${NC}"
echo "GET $QUEUE_SERVICE/queue/$EVENT_ID/status?clientId=$CLIENT_ID"
echo ""

MAX_ATTEMPTS=10
ATTEMPT=1

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
  echo -e "${YELLOW}폴링 시도 $ATTEMPT/$MAX_ATTEMPTS...${NC}"

  STATUS_RESULT=$(curl -s -X GET "$QUEUE_SERVICE/queue/$EVENT_ID/status?clientId=$CLIENT_ID")
  echo "$STATUS_RESULT" | jq '.'

  CAN_PROCEED=$(echo "$STATUS_RESULT" | jq -r '.canProceed')
  PASS_TOKEN=$(echo "$STATUS_RESULT" | jq -r '.passToken')

  if [ "$CAN_PROCEED" = "true" ]; then
    echo ""
    echo -e "${GREEN}✅ 대기열 통과! Pass Token 발급됨${NC}"
    echo -e "${GREEN}Pass Token: $PASS_TOKEN${NC}"
    echo ""

    # Pass Token 저장
    echo "$PASS_TOKEN" > /tmp/pass_token.txt

    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}다음 단계: 티켓 예약${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo "예약 요청 예시:"
    echo ""
    echo "curl -X POST http://localhost:8080/api/reservations?eventId=$EVENT_ID&clientId=$CLIENT_ID \\"
    echo "  -H 'Authorization: Bearer YOUR_JWT_TOKEN' \\"
    echo "  -H 'X-Pass-Token: $PASS_TOKEN' \\"
    echo "  -H 'Content-Type: application/json' \\"
    echo "  -d '{\"productId\": 1, \"quantity\": 2}'"
    echo ""

    exit 0
  fi

  POSITION=$(echo "$STATUS_RESULT" | jq -r '.position')
  echo -e "${YELLOW}대기 중... 현재 순번: $POSITION${NC}"
  echo ""

  sleep 3
  ATTEMPT=$((ATTEMPT + 1))
done

echo -e "${YELLOW}⚠️  최대 시도 횟수 도달. 아직 대기 중입니다.${NC}"
echo -e "${YELLOW}계속 기다리려면 스크립트를 다시 실행하세요.${NC}"
