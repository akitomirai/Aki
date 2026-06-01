<template>
  <div class="login-page" data-testid="login-page">
    <div class="login-shell">
      <div class="login-header">
        <div class="login-logo">
          <img src="/favicon.ico" alt="Trace Admin">
        </div>
        <h1>{{ isMobileLogin ? '现场作业移动端' : '农产品追溯管理后台' }}</h1>
        <p>{{ isMobileLogin ? '操作员登录' : '后台登录' }}</p>
      </div>

      <el-card class="login-card" shadow="hover">
        <section class="login-panel">
          <div class="panel-head">
            <strong>{{ isMobileLogin ? '现场操作员登录' : '欢迎回来' }}</strong>
          </div>

        <el-form :model="form" label-position="top" class="login-form" @submit.prevent>
            <el-form-item label="用户名">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                clearable
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item label="密码">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                show-password
                clearable
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <div class="login-options">
              <el-checkbox v-model="remember">记住用户名</el-checkbox>
            </div>

            <div class="login-actions">
              <el-button
                type="primary"
                class="action-btn"
                :loading="loading"
                data-testid="login-submit"
                @click="handleLogin"
              >
              {{ isMobileLogin ? '登录并进入现场作业' : '登录并进入系统' }}
            </el-button>
          </div>
        </el-form>
        </section>
      </el-card>

      <div class="login-footer-link">
        <template v-if="isMobileLogin">
          <span>管理员登录？</span>
          <button type="button" class="text-link" @click="router.push('/login')">返回后台入口</button>
        </template>
        <template v-else>
          <span>操作员使用手机访问？</span>
          <button type="button" class="text-link" @click="router.push('/mobile-login')">移动端登录</button>
          <span class="footer-divider">/</span>
          <span>没有账号？</span>
          <button type="button" class="text-link" @click="router.push('/register')">开户注册</button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useLogin } from '../../composables/useLogin'

const router = useRouter()
const {
  form,
  loading,
  remember,
  isMobileLogin,
  handleLogin
} = useLogin()
</script>

<style src="./login.css" scoped></style>
