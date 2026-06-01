<template>
  <div class="register-page" data-testid="register-page">
    <div class="register-shell">
      <div class="register-header">
        <div class="register-logo">
          <img src="/favicon.ico" alt="Trace Admin">
        </div>
        <h1>农产品追溯管理后台</h1>
        <p>开户注册</p>
      </div>

      <el-card class="register-card" shadow="hover">
        <section class="register-panel">
          <div class="panel-head">
            <strong>由管理员分级开户注册</strong>
            <span>账号创建后默认启用，首次登录必须修改初始密码。</span>
          </div>

          <div class="register-flow-list">
            <article
              v-for="flow in accountFlows"
              :key="flow.roleName"
              class="register-flow-item"
            >
              <div class="register-flow-main">
                <strong>{{ flow.roleName }}</strong>
                <span>{{ flow.userScope }}</span>
              </div>
              <dl>
                <div>
                  <dt>开户方</dt>
                  <dd>{{ flow.openedBy }}</dd>
                </div>
                <div>
                  <dt>管理方</dt>
                  <dd>{{ flow.managedBy }}</dd>
                </div>
              </dl>
            </article>
          </div>

          <div class="register-actions">
            <el-button type="primary" class="action-btn" data-testid="register-go-login" @click="goLogin">
              返回后台登录
            </el-button>
            <el-button class="action-btn" data-testid="register-go-mobile" @click="goMobileLogin">
              操作员移动端登录
            </el-button>
          </div>
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
  accountFlows,
  goLogin,
  goMobileLogin
} = useRegister()
</script>

<style src="./register.css" scoped></style>
