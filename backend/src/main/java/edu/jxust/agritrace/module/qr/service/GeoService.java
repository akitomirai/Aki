package edu.jxust.agritrace.module.qr.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * 地理解析服务（方案A：离线 ip2region）
 *
 * 功能：
 * - 输入 IP，输出国家/省/市（尽力解析）
 * - 不依赖外部网络服务，适合毕业设计演示
 *
 * 说明：
 * - ip2region 使用 xdb 文件进行查询，性能很高
 * - 这里用“加载整个 xdb 到内存”的方式，查询更快
 */

@Component
public class GeoService {

    // 让它可配置：默认从 resources/ip2region/ip2region_v4.xdb 读
    @Value("${ip2region_4.xdb:ip2region/ip2region_v4.xdb}")
    private String xdbClasspath;

    private Searcher searcher;

    /**
     * 初始化：加载 xdb 到内存并创建 Searcher
     */
    @PostConstruct
    public void init() {
        try {
            Version version = Version.IPv4;

            // 1) 从 classpath 取资源
            ClassPathResource res = new ClassPathResource(xdbClasspath);

            // 2) 复制到临时文件（确保能拿到真实文件路径）
            File tmp = File.createTempFile("ip2region_", ".xdb");
            tmp.deleteOnExit();
            Files.copy(res.getInputStream(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 3) 用“文件模式”创建 searcher
            this.searcher = Searcher.newWithFileOnly(version, tmp.getAbsolutePath());

        } catch (Exception e) {
            throw new IllegalStateException("ip2region 初始化失败，请检查 xdb 是否存在：" + xdbClasspath, e);
        }
    }

    @PreDestroy
    public void destroy() throws Exception {
        if (searcher != null) searcher.close();
    }
    /**
     * 解析 IP -> 归属地（国家/省/市）
     *
     * @param ip 客户端 IP
     * @return 归属地信息（解析不到则返回空字段）
     */
    public GeoInfo resolve(String ip) {
        GeoInfo info = new GeoInfo();
        if (ip == null || ip.isBlank()) return info;

        // 内网/本机地址一般没有意义，直接返回空
        if (isPrivateIp(ip)) return info;

        try {
            // ip2region 常见返回格式类似：国家|区域|省|市|运营商（不同数据文件会有差异）
            String region = searcher.search(ip);

            // 兼容切分（多数是 | 分隔）
            String[] parts = region == null ? new String[0] : region.split("\\|");

            // 尽量填充：国家、省、市
            // 经验上：parts[0]=国家, parts[2]=省, parts[3]=市（不保证 100% 一致，够毕业设计）
            if (parts.length > 0) info.setCountry(safe(parts[0]));
            if (parts.length > 2) info.setProvince(safe(parts[2]));
            if (parts.length > 3) info.setCity(safe(parts[3]));

            // 过滤 0 / null 之类的占位
            cleanup(info);

            return info;
        } catch (Exception e) {
            // 解析失败不影响主流程
            return info;
        }
    }

    private String safe(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private void cleanup(GeoInfo info) {
        for (String bad : Arrays.asList("0", "null", "N/A")) {
            if (bad.equalsIgnoreCase(String.valueOf(info.country))) info.country = null;
            if (bad.equalsIgnoreCase(String.valueOf(info.province))) info.province = null;
            if (bad.equalsIgnoreCase(String.valueOf(info.city))) info.city = null;
        }
    }

    /**
     * 简单判断内网/本机 IP
     */
    private boolean isPrivateIp(String ip) {
        return ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("127.")
                || ip.startsWith("0.")
                || (ip.startsWith("172.") && is172Private(ip));
    }

    private boolean is172Private(String ip) {
        try {
            String[] arr = ip.split("\\.");
            int second = Integer.parseInt(arr[1]);
            return second >= 16 && second <= 31;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 归属地信息
     */
    @Data
    public static class GeoInfo {
        private String country;
        private String province;
        private String city;
    }
}