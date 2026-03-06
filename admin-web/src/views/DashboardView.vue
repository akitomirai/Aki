<template>
  <AdminLayout>
    <el-card>
      <template #header>
        <span>后台首页</span>
      </template>

      <p>登录成功，后台管理端已接通。</p>
      <p>当前已完成后端主线：</p>
      <ul>
        <li>JWT 登录认证</li>
        <li>批次创建</li>
        <li>事件录入</li>
        <li>二维码生成与扫码查询</li>
        <li>PV / UV 统计</li>
        <li>文件上传</li>
        <li>质检报告 + 哈希存证</li>
      </ul>
    </el-card>
  </AdminLayout>
</template>

<script setup>
/**
 * 后台首页
 */
import AdminLayout from '../components/AdminLayout.vue'
</script>