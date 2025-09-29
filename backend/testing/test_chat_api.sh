#!/bin/bash

# Chat API Testing Script
# Testing all backend Chat APIs and endpoints

echo "================================================"
echo "          CHAT API TESTING SCRIPT"
echo "================================================"
echo "Backend URL: http://localhost:8080"
echo "Started at: $(date)"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test result tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Helper function to run tests
run_test() {
    local test_name="$1"
    local curl_command="$2"
    local expected_status="$3"

    echo -e "\n${BLUE}Testing: $test_name${NC}"
    echo "Command: $curl_command"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    # Execute curl command and capture response
    response=$(eval "$curl_command" 2>/dev/null)
    status_code=$(eval "$curl_command -w '%{http_code}' -o /dev/null -s" 2>/dev/null)

    echo "Status Code: $status_code"
    echo "Response: $response"

    if [ "$status_code" = "$expected_status" ]; then
        echo -e "${GREEN}✓ PASSED${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}✗ FAILED (Expected: $expected_status, Got: $status_code)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    echo "----------------------------------------"
}

# Helper function to test with data
run_test_with_data() {
    local test_name="$1"
    local method="$2"
    local url="$3"
    local data="$4"
    local expected_status="$5"

    echo -e "\n${BLUE}Testing: $test_name${NC}"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    if [ "$method" = "POST" ]; then
        curl_command="curl -s -X POST -H 'Content-Type: application/json' -d '$data' '$url'"
        echo "Command: $curl_command"

        response=$(curl -s -X POST -H "Content-Type: application/json" -d "$data" "$url")
        status_code=$(curl -s -X POST -H "Content-Type: application/json" -d "$data" "$url" -w '%{http_code}' -o /dev/null)
    else
        curl_command="curl -s -X $method '$url'"
        echo "Command: $curl_command"

        response=$(curl -s -X "$method" "$url")
        status_code=$(curl -s -X "$method" "$url" -w '%{http_code}' -o /dev/null)
    fi

    echo "Status Code: $status_code"
    echo "Response: $response"

    if [ "$status_code" = "$expected_status" ]; then
        echo -e "${GREEN}✓ PASSED${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}✗ FAILED (Expected: $expected_status, Got: $status_code)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    echo "----------------------------------------"
}

echo -e "\n${YELLOW}=== PHASE 1: CHAT ROOM MANAGEMENT ENDPOINTS ===${NC}"

# Test 1: Get User Chat Rooms (without userId - should fail)
run_test "Get User Chat Rooms (Missing userId)" \
    "curl -s 'http://localhost:8080/api/chat/rooms'" \
    "400"

# Test 2: Get User Chat Rooms (with invalid userId)
run_test "Get User Chat Rooms (Invalid userId)" \
    "curl -s 'http://localhost:8080/api/chat/rooms?userId=999999'" \
    "404"

# Test 3: Get User Chat Rooms (with valid userId - assuming user 1 exists)
run_test "Get User Chat Rooms (Valid userId=1)" \
    "curl -s 'http://localhost:8080/api/chat/rooms?userId=1'" \
    "200"

# Test 4: Get User Chat Rooms with pagination
run_test "Get User Chat Rooms (Paginated)" \
    "curl -s 'http://localhost:8080/api/chat/rooms?userId=1&page=0&size=10'" \
    "200"

# Test 5: Get specific chat room (without userId)
run_test "Get Chat Room Details (Missing userId)" \
    "curl -s 'http://localhost:8080/api/chat/rooms/1'" \
    "400"

# Test 6: Get specific chat room (with invalid roomId)
run_test "Get Chat Room Details (Invalid roomId)" \
    "curl -s 'http://localhost:8080/api/chat/rooms/999999?userId=1'" \
    "404"

# Test 7: Get specific chat room (valid roomId and userId)
run_test "Get Chat Room Details (Valid IDs)" \
    "curl -s 'http://localhost:8080/api/chat/rooms/1?userId=1'" \
    "200"

# Test 8: Get chat room participants
run_test "Get Chat Room Participants" \
    "curl -s 'http://localhost:8080/api/chat/rooms/1/participants?userId=1'" \
    "200"

echo -e "\n${YELLOW}=== PHASE 2: CHAT ROOM CREATION ENDPOINTS ===${NC}"

# Test 9: Create Group Chat
run_test_with_data "Create Group Chat" \
    "POST" \
    "http://localhost:8080/api/chat/rooms/group" \
    '{"chatName":"Test Group Chat","creatorId":1,"participantIds":[1,2]}' \
    "200"

# Test 10: Create/Get Private Chat
run_test_with_data "Create/Get Private Chat" \
    "POST" \
    "http://localhost:8080/api/chat/rooms/private" \
    '{"userId1":1,"userId2":2}' \
    "200"

# Test 11: Create Group Chat (Invalid data)
run_test_with_data "Create Group Chat (Invalid Data)" \
    "POST" \
    "http://localhost:8080/api/chat/rooms/group" \
    '{"chatName":"","creatorId":null,"participantIds":[]}' \
    "400"

echo -e "\n${YELLOW}=== PHASE 3: MESSAGE ENDPOINTS ===${NC}"

# Test 12: Get Message History
run_test "Get Message History" \
    "curl -s 'http://localhost:8080/api/chat/rooms/1/messages?userId=1'" \
    "200"

# Test 13: Get Message History with pagination
run_test "Get Message History (Paginated)" \
    "curl -s 'http://localhost:8080/api/chat/rooms/1/messages?userId=1&page=0&size=20'" \
    "200"

# Test 14: Get Recent Messages
run_test "Get Recent Messages" \
    "curl -s 'http://localhost:8080/api/chat/rooms/1/messages/recent?userId=1&limit=50'" \
    "200"

# Test 15: Get Recent Messages (Custom limit)
run_test "Get Recent Messages (Custom Limit)" \
    "curl -s 'http://localhost:8080/api/chat/rooms/1/messages/recent?userId=1&limit=10'" \
    "200"

# Test 16: Get Unread Messages
run_test "Get Unread Messages" \
    "curl -s 'http://localhost:8080/api/chat/rooms/1/messages/unread?userId=1'" \
    "200"

# Test 17: Mark Messages as Read
run_test_with_data "Mark Messages as Read" \
    "POST" \
    "http://localhost:8080/api/chat/rooms/1/messages/mark-read?userId=1" \
    "" \
    "200"

# Test 18: Search Messages
run_test "Search Messages" \
    "curl -s 'http://localhost:8080/api/messages/search?roomId=1&userId=1&searchTerm=hello'" \
    "200"

# Test 19: Search Messages (Empty term)
run_test "Search Messages (Empty Term)" \
    "curl -s 'http://localhost:8080/api/messages/search?roomId=1&userId=1&searchTerm='" \
    "200"

echo -e "\n${YELLOW}=== PHASE 4: USER MANAGEMENT ENDPOINTS ===${NC}"

# Test 20: Add User to Group
run_test_with_data "Add User to Group" \
    "POST" \
    "http://localhost:8080/api/chat/rooms/1/participants" \
    '{"userId":3,"requestingUserId":1}' \
    "200"

# Test 21: Remove User from Group
run_test "Remove User from Group" \
    "curl -s -X DELETE 'http://localhost:8080/api/chat/rooms/1/participants/3?requestingUserId=1'" \
    "200"

echo -e "\n${YELLOW}=== PHASE 5: ERROR HANDLING TESTS ===${NC}"

# Test 22: Missing userId parameter
run_test "Missing userId Parameter" \
    "curl -s 'http://localhost:8080/api/chat/rooms/1/messages'" \
    "400"

# Test 23: Invalid roomId
run_test "Invalid Room ID" \
    "curl -s 'http://localhost:8080/api/chat/rooms/999999/messages?userId=1'" \
    "404"

# Test 24: Invalid JSON for POST requests
run_test_with_data "Invalid JSON Data" \
    "POST" \
    "http://localhost:8080/api/chat/rooms/group" \
    '{"invalid":json}' \
    "400"

echo -e "\n${YELLOW}=== PHASE 6: WEBSOCKET ENDPOINT VERIFICATION ===${NC}"

# Test 25: WebSocket endpoint (should return upgrade required)
run_test "WebSocket Endpoint" \
    "curl -s 'http://localhost:8080/ws'" \
    "400"

echo -e "\n================================================"
echo -e "${BLUE}            TEST SUMMARY${NC}"
echo "================================================"
echo -e "Total Tests:  ${TOTAL_TESTS}"
echo -e "Passed:       ${GREEN}${PASSED_TESTS}${NC}"
echo -e "Failed:       ${RED}${FAILED_TESTS}${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}🎉 ALL TESTS PASSED! Backend is ready for frontend integration.${NC}"
else
    echo -e "\n${YELLOW}⚠️  Some tests failed. Review the output above for details.${NC}"
fi

echo "================================================"
echo "Test completed at: $(date)"
echo "================================================"