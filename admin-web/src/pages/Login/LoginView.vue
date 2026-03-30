<template>
  <div class="login-page" data-testid="login-page">
    <el-card class="login-card" shadow="hover">
      <div class="login-brand">
        <p class="eyebrow">后台登录</p>
        <h1>农产品追溯管理后台</h1>
        <p class="lead">输入账号后直接进入系统，统一处理产品、企业、批次、质检和风险业务。</p>
      </div>

      <el-form :model="form" label-width="74px" class="login-form" @submit.prevent>
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
            登录并进入系统
          </el-button>
        </div>
      </el-form>

      <div class="account-board">
        <div class="board-head">
          <span class="board-title">测试账号</span>
          <span class="board-tip">点击即可快速填充</span>
        </div>
        <div class="account-list">
          <button type="button" class="account-chip" @click="fillAccount('platform')">
            <strong>平台管理员</strong>
            <span>platform / 123456</span>
          </button>
          <button type="button" class="account-chip" @click="fillAccount('enterprise_admin')">
            <strong>企业管理员</strong>
            <span>enterprise_admin / 123456</span>
          </button>
          <button type="button" class="account-chip" @click="fillAccount('operator')">
            <strong>现场操作员</strong>
            <span>operator / 123456</span>
          </button>
          <button type="button" class="account-chip" @click="fillAccount('regulator')">
            <strong>监管人员</strong>
            <span>regulator / 123456</span>
          </button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useLogin } from '../../composables/useLogin'

const {
  form,
  loading,
  remember,
  fillAccount,
  handleLogin
} = useLogin()
</script>

<style src="./login.css" scoped></style>
