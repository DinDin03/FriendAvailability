const WebSocket = require('ws');

console.log('================================================');
console.log('          WEBSOCKET TESTING CLIENT');
console.log('================================================');
console.log('WebSocket URL: ws://localhost:8080/ws');
console.log('Started at:', new Date().toISOString());
console.log('================================================');

// Test result tracking
let testResults = {
    connection: false,
    sendMessage: false,
    receiveMessage: false,
    joinRoom: false,
    leaveRoom: false,
    totalTests: 5,
    passedTests: 0
};

// Helper function to log test results
function logResult(testName, passed, details = '') {
    console.log(`\n${testName}: ${passed ? '✅ PASSED' : '❌ FAILED'}`);
    if (details) console.log(`Details: ${details}`);
    if (passed) testResults.passedTests++;
}

// Helper function to send STOMP frame
function sendStompFrame(ws, command, headers = {}, body = '') {
    const headerLines = Object.entries(headers)
        .map(([key, value]) => `${key}:${value}`)
        .join('\n');

    const frame = `${command}\n${headerLines}\n\n${body}\0`;
    console.log(`\nSending STOMP frame:\n${frame.replace('\0', '\\0')}`);
    ws.send(frame);
}

// Test 1: WebSocket Connection
console.log('\n=== TEST 1: WebSocket Connection ===');
const ws = new WebSocket('ws://localhost:8080/ws');

ws.on('open', function open() {
    console.log('WebSocket connection opened');
    testResults.connection = true;
    logResult('WebSocket Connection', true, 'Connected successfully');

    // Test 2: STOMP CONNECT
    console.log('\n=== TEST 2: STOMP CONNECT ===');
    sendStompFrame(ws, 'CONNECT', {
        'accept-version': '1.1,1.0',
        'heart-beat': '10000,10000'
    });
});

ws.on('message', function message(data) {
    const frame = data.toString();
    console.log(`\nReceived STOMP frame:\n${frame}`);

    // Parse STOMP frame
    const lines = frame.split('\n');
    const command = lines[0];

    if (command === 'CONNECTED') {
        console.log('STOMP CONNECTED received');

        // Test 3: Subscribe to chat room
        console.log('\n=== TEST 3: Subscribe to Chat Room ===');
        sendStompFrame(ws, 'SUBSCRIBE', {
            'id': 'sub-0',
            'destination': '/topic/chat/2'
        });

        // Test 4: Join chat room
        setTimeout(() => {
            console.log('\n=== TEST 4: Join Chat Room ===');
            sendStompFrame(ws, 'SEND', {
                'destination': '/app/chat.connectToChat',
                'content-type': 'application/json'
            }, JSON.stringify({
                roomId: 2,
                userId: 5
            }));
        }, 1000);

        // Test 5: Send a message
        setTimeout(() => {
            console.log('\n=== TEST 5: Send Message ===');
            sendStompFrame(ws, 'SEND', {
                'destination': '/app/chat.sendMessage',
                'content-type': 'application/json'
            }, JSON.stringify({
                roomId: 2,
                senderId: 5,
                content: 'Hello from WebSocket test!'
            }));
        }, 2000);

        // Test 6: Leave chat room
        setTimeout(() => {
            console.log('\n=== TEST 6: Leave Chat Room ===');
            sendStompFrame(ws, 'SEND', {
                'destination': '/app/chat.disconnectFromChat',
                'content-type': 'application/json'
            }, JSON.stringify({
                roomId: 2,
                userId: 5
            }));
        }, 3000);

        // Close connection after tests
        setTimeout(() => {
            console.log('\n=== Closing Connection ===');
            ws.close();
        }, 4000);

    } else if (command === 'MESSAGE') {
        // Parse message content
        const bodyStartIndex = frame.indexOf('\n\n') + 2;
        const bodyEndIndex = frame.lastIndexOf('\0');
        const messageBody = frame.substring(bodyStartIndex, bodyEndIndex);

        try {
            const messageData = JSON.parse(messageBody);
            console.log('Parsed message data:', messageData);

            // Check message types
            if (messageData.content && messageData.content.includes('Hello from WebSocket test!')) {
                testResults.sendMessage = true;
                testResults.receiveMessage = true;
                logResult('Send Message', true, 'Message sent and received successfully');
                logResult('Receive Message', true, 'Real-time message delivery working');
            }

            if (messageData.content && messageData.content.includes('joined the chat')) {
                testResults.joinRoom = true;
                logResult('Join Room', true, 'User join notification received');
            }

            if (messageData.content && messageData.content.includes('left the chat')) {
                testResults.leaveRoom = true;
                logResult('Leave Room', true, 'User leave notification received');
            }

        } catch (e) {
            console.log('Could not parse message as JSON:', messageBody);
        }
    }
});

ws.on('error', function error(err) {
    console.error('WebSocket error:', err);
    logResult('WebSocket Connection', false, `Error: ${err.message}`);
});

ws.on('close', function close() {
    console.log('\nWebSocket connection closed');

    // Final test summary
    setTimeout(() => {
        console.log('\n================================================');
        console.log('              WEBSOCKET TEST SUMMARY');
        console.log('================================================');
        console.log(`Total Tests: ${testResults.totalTests}`);
        console.log(`Passed: ${testResults.passedTests}`);
        console.log(`Failed: ${testResults.totalTests - testResults.passedTests}`);

        console.log('\nDetailed Results:');
        console.log(`- WebSocket Connection: ${testResults.connection ? '✅' : '❌'}`);
        console.log(`- Send Message: ${testResults.sendMessage ? '✅' : '❌'}`);
        console.log(`- Receive Message: ${testResults.receiveMessage ? '✅' : '❌'}`);
        console.log(`- Join Room: ${testResults.joinRoom ? '✅' : '❌'}`);
        console.log(`- Leave Room: ${testResults.leaveRoom ? '✅' : '❌'}`);

        if (testResults.passedTests === testResults.totalTests) {
            console.log('\n🎉 ALL WEBSOCKET TESTS PASSED! Real-time messaging is working perfectly.');
        } else {
            console.log('\n⚠️  Some WebSocket tests failed. Check the logs above for details.');
        }

        console.log('================================================');
        console.log('Test completed at:', new Date().toISOString());
        console.log('================================================');

        process.exit(0);
    }, 1000);
});

// Error handling for connection timeout
setTimeout(() => {
    if (!testResults.connection) {
        console.error('\n❌ WebSocket connection timeout after 10 seconds');
        logResult('WebSocket Connection', false, 'Connection timeout');
        ws.close();
    }
}, 10000);