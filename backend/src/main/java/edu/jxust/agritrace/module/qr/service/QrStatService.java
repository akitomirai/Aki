package edu.jxust.agritrace.module.qr.service;

/**
 * 二维码统计服务（可插拔）：
 * - 有 Redis：走 Redis INCR（高并发）
 * - 无 Redis：走 MySQL UPDATE（先跑通）
 */
public interface QrStatService {

    /**
     * PV +1，并返回自增后的 PV（用于前端展示热度）
     */
    long incrAndGetPv(long qrId);
}