# Requirements

cd frontend
npm install

Seems to work? Had to downgrade from Vite 7.0.4 and react 4.6.0 to Vite 5.4.19 and react 4.6.0

# Packages

npm install react-router-dom
npm install tailwindcss/cli

# 4. Production Build and Deployment
- When ready to deploy, build the React app:
- Apply to SecurityConf...
- This creates a frontend/dist folder with static files.
- Copy the contents of frontend/dist to src/main/resources/static in your Spring Boot project:
- Apply to SecurityConf...
- Spring Boot will now serve your React app at /.