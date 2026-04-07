#!/bin/bash
# Terms API Zero Script QA

BASE_URL="http://localhost:8080"
PASS=0
FAIL=0

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

pass() { echo -e "${GREEN}✅ PASS${NC} — $1"; ((PASS++)); }
fail() { echo -e "${RED}❌ FAIL${NC} — $1"; echo "   응답: $2"; ((FAIL++)); }

call() {
    RESP=$(curl -s "$@")
    CODE=$(curl -s -o /dev/null -w "%{http_code}" "$@")
    echo "$CODE|$RESP"
}

echo "========================================"
echo "  Terms API 테스트"
echo "========================================"

# -------------------------------------------------------
# TC-01: GET /terms → 최신 버전(v2.0) 3건만 반환
# -------------------------------------------------------
echo ""
echo "[TC-01] GET /terms — 최신 버전 약관 조회"
RESULT=$(call "$BASE_URL/terms")
CODE="${RESULT%%|*}"; BODY="${RESULT#*|}"

if [[ "$CODE" == "200" ]]; then
    V2=$(echo "$BODY" | grep -o '"version":"2.0"' | wc -l | tr -d ' ')
    V1=$(echo "$BODY" | grep -o '"version":"1.0"' | wc -l | tr -d ' ')
    if [[ "$V2" -eq 3 && "$V1" -eq 0 ]]; then
        pass "v2.0 약관 3건 반환, v1.0 미포함"
    else
        fail "버전 필터 오류 (v2.0=${V2}건, v1.0=${V1}건)" "$BODY"
    fi
else
    fail "HTTP $CODE" "$BODY"
fi

# -------------------------------------------------------
# TC-02: GET /terms?version=1.0 → v1.0 약관 3건
# -------------------------------------------------------
echo ""
echo "[TC-02] GET /terms?version=1.0 — 특정 버전 약관 조회"
RESULT=$(call "$BASE_URL/terms?version=1.0")
CODE="${RESULT%%|*}"; BODY="${RESULT#*|}"

if [[ "$CODE" == "200" ]]; then
    V1=$(echo "$BODY" | grep -o '"version":"1.0"' | wc -l | tr -d ' ')
    if [[ "$V1" -eq 3 ]]; then
        pass "v1.0 약관 3건 반환"
    else
        fail "v1.0 약관 수 오류 (${V1}건)" "$BODY"
    fi
else
    fail "HTTP $CODE" "$BODY"
fi

# -------------------------------------------------------
# TC-03: GET /terms?version=2.0 → v2.0 약관 3건
# -------------------------------------------------------
echo ""
echo "[TC-03] GET /terms?version=2.0 — v2.0 버전 약관 조회"
RESULT=$(call "$BASE_URL/terms?version=2.0")
CODE="${RESULT%%|*}"; BODY="${RESULT#*|}"

if [[ "$CODE" == "200" ]]; then
    V2=$(echo "$BODY" | grep -o '"version":"2.0"' | wc -l | tr -d ' ')
    if [[ "$V2" -eq 3 ]]; then
        pass "v2.0 약관 3건 반환"
    else
        fail "v2.0 약관 수 오류 (${V2}건)" "$BODY"
    fi
else
    fail "HTTP $CODE" "$BODY"
fi

# -------------------------------------------------------
# TC-04: GET /terms?version=9.9 → 존재하지 않는 버전 → 에러
# -------------------------------------------------------
echo ""
echo "[TC-04] GET /terms?version=9.9 — 존재하지 않는 버전"
RESULT=$(call "$BASE_URL/terms?version=9.9")
CODE="${RESULT%%|*}"; BODY="${RESULT#*|}"

if [[ "$CODE" == "400" || "$CODE" == "404" ]]; then
    pass "에러 응답 (HTTP $CODE)"
else
    fail "에러를 반환해야 함 (HTTP $CODE)" "$BODY"
fi

# -------------------------------------------------------
# TC-05: 응답 필드 구조 검증
# -------------------------------------------------------
echo ""
echo "[TC-05] GET /terms — 응답 필드 구조 검증"
BODY=$(curl -s "$BASE_URL/terms")

REQUIRED_FIELDS=("termsId" "termsCode" "title" "content" "required" "version" "effectiveDate")
MISSING=()
for FIELD in "${REQUIRED_FIELDS[@]}"; do
    if ! echo "$BODY" | grep -q "\"$FIELD\""; then
        MISSING+=("$FIELD")
    fi
done

if [[ ${#MISSING[@]} -eq 0 ]]; then
    pass "모든 필드 포함"
else
    fail "누락 필드: ${MISSING[*]}" "$BODY"
fi

# -------------------------------------------------------
# 결과 요약
# -------------------------------------------------------
echo ""
echo "========================================"
echo "  결과: ✅ $PASS / ❌ $FAIL"
echo "========================================"
