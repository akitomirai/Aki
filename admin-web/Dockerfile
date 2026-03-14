# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
# 使用国内镜像源加速
RUN npm config set registry https://registry.npmmirror.com
RUN npm install
COPY . .
# 设置构建时的 API 基础路径为相对路径 /api，由 Nginx 转发
ENV VITE_API_BASE_URL=/api
RUN npm run build

# Production stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
