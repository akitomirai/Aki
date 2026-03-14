<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <template #header>
        <div class="title-wrap">
          <div class="title">农产品溯源系统后台登录</div>
          <div class="sub-title">Agricultural Traceability Admin</div>
        </div>
      </template>

      <el-form
          :model="form"
          label-width="72px"
          class="login-form"
          @submit.prevent
      >
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
              @click="handleLogin"
          >
            登录
          </el-button>

          <el-button
              class="action-btn register-btn"
              @click="goRegister"
          >
            注册
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { useLogin } from '../../composables/useLogin'

const {
  form,
  loading,
  remember,
  handleLogin,
  goRegister
} = useLogin()
</script>

<style src="./login.css" scoped></style>