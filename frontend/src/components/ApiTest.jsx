import { useState } from 'react';
import { authService } from '../services/authService';
import { userService } from '../services/userService';

export const ApiTest = () => {
  const [testResults, setTestResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loginData, setLoginData] = useState({ email: '', password: '' });

  const addResult = (test, success, message, data = null) => {
    const result = {
      test,
      success,
      message,
      data,
      timestamp: new Date().toLocaleTimeString()
    };
    setTestResults(prev => [result, ...prev]);
  };

  const testBackendConnection = async () => {
    setLoading(true);
    try {
      const users = await userService.getAllUsers();
      addResult('Backend Connection', true, `Successfully connected! Found ${users.length} users`, users);
    } catch (error) {
      addResult('Backend Connection', false, `Connection failed: ${error.message}`);
    }
    setLoading(false);
  };

  const testLogin = async () => {
    if (!loginData.email || !loginData.password) {
      addResult('Login Test', false, 'Please enter email and password');
      return;
    }

    setLoading(true);
    try {
      const response = await authService.login(loginData.email, loginData.password);
      addResult('Login Test', true, 'Login successful!', response);
    } catch (error) {
      addResult('Login Test', false, `Login failed: ${error.message}`);
    }
    setLoading(false);
  };

  const testRegister = async () => {
    const testUser = {
      name: 'Test User',
      email: 'test@example.com',
      password: 'testpassword123'
    };

    setLoading(true);
    try {
      const response = await authService.register(testUser);
      addResult('Registration Test', true, 'Registration successful!', response);
    } catch (error) {
      addResult('Registration Test', false, `Registration failed: ${error.message}`);
    }
    setLoading(false);
  };

  const clearResults = () => {
    setTestResults([]);
  };

  return (
    <div className="max-w-4xl mx-auto p-6 bg-white rounded-lg shadow-lg">
      <h2 className="text-2xl font-bold mb-6 text-gray-800">API Connection Test</h2>
      
      {/* Test Buttons */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <button
          onClick={testBackendConnection}
          disabled={loading}
          className="bg-blue-500 hover:bg-blue-600 disabled:bg-gray-400 text-white px-4 py-2 rounded transition-colors"
        >
          {loading ? 'Testing...' : 'Test Backend Connection'}
        </button>
        
        <button
          onClick={testRegister}
          disabled={loading}
          className="bg-green-500 hover:bg-green-600 disabled:bg-gray-400 text-white px-4 py-2 rounded transition-colors"
        >
          {loading ? 'Testing...' : 'Test Registration'}
        </button>
        
        <button
          onClick={clearResults}
          className="bg-gray-500 hover:bg-gray-600 text-white px-4 py-2 rounded transition-colors"
        >
          Clear Results
        </button>
      </div>

      {/* Login Test Form */}
      <div className="bg-gray-50 p-4 rounded-lg mb-6">
        <h3 className="text-lg font-semibold mb-3">Login Test</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <input
            type="email"
            placeholder="Email"
            value={loginData.email}
            onChange={(e) => setLoginData(prev => ({ ...prev, email: e.target.value }))}
            className="border border-gray-300 px-3 py-2 rounded focus:outline-none focus:border-blue-500"
          />
          <input
            type="password"
            placeholder="Password"
            value={loginData.password}
            onChange={(e) => setLoginData(prev => ({ ...prev, password: e.target.value }))}
            className="border border-gray-300 px-3 py-2 rounded focus:outline-none focus:border-blue-500"
          />
          <button
            onClick={testLogin}
            disabled={loading}
            className="bg-purple-500 hover:bg-purple-600 disabled:bg-gray-400 text-white px-4 py-2 rounded transition-colors"
          >
            {loading ? 'Testing...' : 'Test Login'}
          </button>
        </div>
      </div>

      {/* Results */}
      <div className="space-y-3">
        <h3 className="text-lg font-semibold">Test Results:</h3>
        {testResults.length === 0 ? (
          <p className="text-gray-500 italic">No tests run yet. Click a test button above.</p>
        ) : (
          testResults.map((result, index) => (
            <div
              key={index}
              className={`p-4 rounded-lg border-l-4 ${
                result.success 
                  ? 'bg-green-50 border-green-500 text-green-800' 
                  : 'bg-red-50 border-red-500 text-red-800'
              }`}
            >
              <div className="flex justify-between items-start mb-2">
                <h4 className="font-semibold">{result.test}</h4>
                <span className="text-sm opacity-75">{result.timestamp}</span>
              </div>
              <p className="mb-2">{result.message}</p>
              {result.data && (
                <details className="mt-2">
                  <summary className="cursor-pointer text-sm font-medium">View Response Data</summary>
                  <pre className="mt-2 p-2 bg-gray-100 rounded text-xs overflow-auto">
                    {JSON.stringify(result.data, null, 2)}
                  </pre>
                </details>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};
