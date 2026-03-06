// Test script for mock payment functionality
// Run this with: node test-mock-payment.js

const https = require('https');

// Test data for mock payment
const testPaymentData = {
  tenantId: "test_tenant_123",
  propertyId: "test_property_456", 
  amount: 10,
  status: "SUCCESS",
  currency: "USD"
};

// Function to test the mock payment endpoint
function testMockPayment() {
  const data = JSON.stringify(testPaymentData);
  
  const options = {
    hostname: 'localhost', // Change to your functions URL when deployed
    port: 5001, // Default Firebase Functions port
    path: '/verifyPesePay', // Your function endpoint
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': data.length
    }
  };

  const req = https.request(options, (res) => {
    console.log(`Status: ${res.statusCode}`);
    console.log(`Headers: ${JSON.stringify(res.headers)}`);
    
    res.setEncoding('utf8');
    res.on('data', (chunk) => {
      console.log(`Response: ${chunk}`);
    });
  });

  req.on('error', (error) => {
    console.error(`Problem with request: ${error.message}`);
  });

  req.write(data);
  req.end();
}

// Test different scenarios
console.log('Testing mock payment scenarios...');

// Test 1: Successful payment
console.log('\n1. Testing successful payment:');
testPaymentData.status = "SUCCESS";
testPaymentData.tenantId = "test_tenant_success";
testPaymentData.propertyId = "test_property_success";

// Test 2: Failed payment  
console.log('\n2. Testing failed payment:');
testPaymentData.status = "FAILED";
testPaymentData.tenantId = "test_tenant_fail";
testPaymentData.propertyId = "test_property_fail";

// Test 3: Invalid amount
console.log('\n3. Testing invalid amount:');
testPaymentData.status = "SUCCESS";
testPaymentData.amount = 5; // Below minimum
testPaymentData.tenantId = "test_tenant_invalid";
testPaymentData.propertyId = "test_property_invalid";

console.log('\nTo run these tests:');
console.log('1. Start your Firebase Functions locally: firebase emulators:start');
console.log('2. Set PESEPAY_INTEGRATION_KEY=demo_key_for_testing in your environment');
console.log('3. Run: node test-mock-payment.js');
console.log('\nOr use curl commands:');
console.log(`curl -X POST http://localhost:5001/your-project/us-central1/verifyPesePay \\
  -H "Content-Type: application/json" \\
  -d '${JSON.stringify(testPaymentData)}'`);
