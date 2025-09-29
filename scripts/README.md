# Chat API Testing Scripts

This folder contains scripts for testing the backend Chat APIs and WebSocket functionality.

## Scripts Available

### 1. REST API Testing
**File:** `test_chat_api.sh`
**Purpose:** Test all REST API endpoints for chat functionality

```bash
# Run from project root or scripts folder
cd scripts
./test_chat_api.sh
```

**What it tests:**
- Chat room management endpoints
- Message operations (history, recent, unread, search)
- User management in groups
- Error handling and validation
- All CRUD operations

### 2. WebSocket Testing
**File:** `websocket_test.js`
**Purpose:** Test real-time messaging via WebSocket/STOMP

```bash
# Run from scripts folder
cd scripts
node websocket_test.js
```

**Prerequisites:**
- Node.js installed
- Backend running on localhost:8080
- ws module installed (package.json included)

**What it tests:**
- WebSocket connection to /ws endpoint
- STOMP protocol handshake
- Join/leave chat room notifications
- Send and receive messages
- Real-time message delivery

## Dependencies

The scripts use:
- `curl` for REST API testing
- `node` and `ws` module for WebSocket testing
- Backend must be running on `http://localhost:8080`

## Test Results

See `../instructions/CHAT_API_TEST_RESULTS.md` for comprehensive test results and backend readiness assessment.

## Usage Notes

1. **Start Backend First:**
   ```bash
   cd ../backend
   ./mvnw spring-boot:run
   ```

2. **Run REST API Tests:**
   ```bash
   cd scripts
   ./test_chat_api.sh
   ```

3. **Run WebSocket Tests:**
   ```bash
   cd scripts
   node websocket_test.js
   ```

## Test Data

The scripts create test users and chat rooms automatically:
- Alice Johnson (ID: 5)
- Bob Smith (ID: 6)
- Charlie Wilson (ID: 7)
- Test Group Chat (ID: 2)

## Expected Results

- REST API: ~20+ endpoints tested with 95%+ success rate
- WebSocket: Connection and real-time messaging verification
- Comprehensive error handling validation
- Backend readiness confirmation for frontend integration