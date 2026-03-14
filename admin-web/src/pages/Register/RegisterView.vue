<template>
  <div class="register-page">
    <el-card class="register-card" shadow="hover">
      <template #header>
        <div class="title-wrap">
          <div class="title">农产品溯源系统用户注册</div>
          <div class="sub-title">Agricultural Traceability Register</div>
        </div>
      </template>

      <el-form
          :model="form"
          label-width="72px"
          class="register-form"
          @submit.prevent
      >
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

        <el-form-item label="真实姓名">
          <el-input
              v-model="form.realName"
              placeholder="请输入真实姓名"
              clearable
              @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item label="手机号">
          <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
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
              @click="handleRegister"
          >
            注册
          </el-button>

          <el-button
              class="action-btn back-btn"
              @click="goLogin"
          >
            返回登录
          </el-button>
        </div>
      </el-form>
    </el-card>
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