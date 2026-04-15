# Stage 1: Build the Angular App
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build --configuration production

# Stage 2: Serve with Nginx
FROM nginx:alpine
# Copy the custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copy the built SPA files to Nginx HTML folder
# Make sure the angular.json output path is matching this (default is dist/omnicharge-frontend/browser)
COPY --from=build /app/dist/omnicharge-frontend/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
