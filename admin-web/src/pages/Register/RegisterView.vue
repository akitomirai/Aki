<template>
  <div class="register-page" data-testid="register-page">
    <div class="register-shell">
      <div class="register-header">
        <div class="register-logo">
          <img src="/favicon.ico" alt="Trace Admin">
        </div>
        <h1>农产品追溯管理后台</h1>
        <p>后台注册</p>
      </div>

      <el-card class="register-card" shadow="hover">
        <section class="register-panel">
          <div class="panel-head">
            <strong>创建账户</strong>
          </div>

          <el-form :model="form" label-position="top" class="register-form" @submit.prevent>
            <el-form-item label="用户名">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                clearable
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <el-form-item label="密码">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                show-password
                clearable
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <el-form-item label="确认密码">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                show-password
                clearable
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <el-form-item label="邀请码">
              <el-input
                v-model="form.inviteCode"
                placeholder="请输入邀请码"
                clearable
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <div class="register-actions">
              <el-button
                type="primary"
                class="action-btn"
                :loading="loading"
                data-testid="register-submit"
                @click="handleRegister"
              >
                创建账户
              </el-button>
            </div>
          </el-form>
        </section>
      </el-card>

      <div class="register-footer-link">
        <span>已有账户？</span>
        <button type="button" class="text-link" @click="goLogin">登录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRegister } from '../../composables/useRegister'

const {
  form,
  loading,
  handleRegister,
  goLogin
} = useRegister()
</script>

<style src="./register.css" scoped></style>
