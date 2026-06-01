package edu.jxust.agritrace.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.common.api.ApiResponse;
import edu.jxust.agritrace.common.exception.ForbiddenException;
import edu.jxust.agritrace.common.exception.UnauthorizedException;
import edu.jxust.agritrace.module.auth.model.AuthUserSession;
import edu.jxust.agritrace.module.batch.mapper.OrgCompanyMapper;
import edu.jxust.agritrace.module.batch.mapper.BaseProductMapper;
import edu.jxust.agritrace.module.batch.mapper.QrCodeMapper;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.batch.mapper.po.BaseProductPO;
import edu.jxust.agritrace.module.batch.mapper.po.OrgCompanyPO;
import edu.jxust.agritrace.module.batch.mapper.po.QrCodePO;
import edu.jxust.agritrace.module.batch.mapper.po.TraceBatchPO;
import edu.jxust.agritrace.module.publictrace.dto.PublicFeedbackHandleRequest;
import edu.jxust.agritrace.module.publictrace.dto.PublicFeedbackRequest;
import edu.jxust.agritrace.module.publictrace.vo.PublicFeedbackVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class PublicFeedbackController {

    private static final Logger log = LoggerFactory.getLogger(PublicFeedbackController.class);
    private static final String TYPE_INFO_MISMATCH = "\u4fe1\u606f\u4e0d\u4e00\u81f4";
    private static final String TYPE_QUALITY = "\u8d28\u91cf\u7591\u95ee";
    private static final String TYPE_QR = "\u4e8c\u7ef4\u7801\u65e0\u6cd5\u8bc6\u522b";
    private static final String TYPE_DISPLAY = "\u9875\u9762\u663e\u793a\u5f02\u5e38";
    private static final String TYPE_OTHER = "\u5176\u4ed6";
    private static final String UNKNOWN_TEXT = "\u672a\u8bc6\u522b";
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            TYPE_INFO_MISMATCH,
            TYPE_QUALITY,
            TYPE_QR,
            TYPE_DISPLAY,
            TYPE_OTHER
    );
    private static final Set<String> HANDLE_STATUSES = Set.of("PENDING", "PROCESSING", "CLOSED");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AtomicLong feedbackIdSequence = new AtomicLong(0L);
    private final List<PublicFeedbackRecord> feedbackRecords = new CopyOnWriteArrayList<>();
    private final TraceBatchMapper traceBatchMapper;
    private final OrgCompanyMapper orgCompanyMapper;
    private final BaseProductMapper baseProductMapper;
    private final QrCodeMapper qrCodeMapper;

    public PublicFeedbackController(
            TraceBatchMapper traceBatchMapper,
            OrgCompanyMapper orgCompanyMapper,
            BaseProductMapper baseProductMapper,
            QrCodeMapper qrCodeMapper
    ) {
        this.traceBatchMapper = traceBatchMapper;
        this.orgCompanyMapper = orgCompanyMapper;
        this.baseProductMapper = baseProductMapper;
        this.qrCodeMapper = qrCodeMapper;
        seedDemoFeedback();
    }

    private void seedDemoFeedback() {
        feedbackRecords.addAll(List.of(
                demoFeedback("大米", "RICE-202604-R1", "rice-202604-r1", TYPE_INFO_MISMATCH,
                        "消费者 13800001001", "公开页中产地说明和包装标签略有差异，请企业核对后更新展示内容。",
                        "2026-04-16 09:12", 1L, "江西稻香生态农业有限公司", "PENDING", "", "", ""),
                demoFeedback("花生", "PEANUT-202604-P1", "peanut-202604-p1", TYPE_QUALITY,
                        "采购商 13800001002", "花生外包装批号与质检摘要展示顺序不一致，希望确认是否为同一批次。",
                        "2026-04-16 09:25", 1L, "江西稻香生态农业有限公司", "PENDING", "", "", ""),
                demoFeedback("赣南脐橙", "ORANGE-202604-Q1", "orange-202604-q1", TYPE_DISPLAY,
                        "门店人员 13800001003", "手机端查看公开页时质检摘要换行较多，已提交截图等待页面优化。",
                        "2026-04-16 10:05", 2L, "赣南果业种植有限公司", "PROCESSING",
                        "已转给企业管理员核对公开页文案与移动端展示。", "平台管理员", "2026-04-16 10:30"),
                demoFeedback("阳光葡萄", "GRAPE-202604-G1", "grape-202604-g1", TYPE_QR,
                        "监管抽查 13800001004", "风险批次扫码后提示处理中，希望补充冷链异常说明和后续处置进度。",
                        "2026-04-16 10:18", 2L, "赣南果业种植有限公司", "PROCESSING",
                        "企业已开始补充冷链复核材料，监管人员继续跟进。", "果业企业管理员", "2026-04-16 10:45"),
                demoFeedback("草莓", "STRAWBERRY-202604-S1", "strawberry-202604-s1", TYPE_INFO_MISMATCH,
                        "经销商 13800001005", "草莓召回提示已经看到，想确认是否还有同批次库存需要下架。",
                        "2026-04-16 11:02", 3L, "赣州鲜果联合合作社", "CLOSED",
                        "已确认同批次库存全部下架，公开页保留召回提示。", "平台管理员", "2026-04-16 11:40"),
                demoFeedback("大米", "RICE-202604-READY-A", "rice-202604-ready-a", TYPE_OTHER,
                        "测试人员 13800001006", "用于测试待发布批次的反馈流转，确认输入输出均可在页面中查看。",
                        "2026-04-16 11:20", 1L, "江西稻香生态农业有限公司", "CLOSED",
                        "测试记录已核对，作为答辩演示样例保留。", "平台管理员", "2026-04-16 11:55")
        ));
    }

    private PublicFeedbackRecord demoFeedback(
            String productName,
            String batchNo,
            String traceCode,
            String feedbackType,
            String contact,
            String content,
            String createdAt,
            Long companyId,
            String companyName,
            String status,
            String handleResult,
            String handlerName,
            String handledAt
    ) {
        return new PublicFeedbackRecord(
                feedbackIdSequence.incrementAndGet(),
                productName,
                batchNo,
                traceCode,
                feedbackType,
                contact,
                content,
                createdAt,
                createdAt,
                companyId,
                companyName,
                "127.0.0.1",
                "Demo Browser",
                status,
                handleResult,
                handlerName,
                handledAt
        );
    }

    @PostMapping("/api/public/feedback")
    public ApiResponse<Map<String, Object>> submitFeedback(
            @Valid @RequestBody PublicFeedbackRequest request,
            HttpServletRequest servletRequest
    ) {
        PublicFeedbackRecord record = validateAndCreateRecord(request, servletRequest);
        feedbackRecords.add(record);
        log.info("public trace feedback submitted: {}", record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("receivedAt", record.receivedAt());
        return ApiResponse.ok("\u53cd\u9988\u5df2\u63d0\u4ea4", result);
    }

    @GetMapping("/api/feedback")
    public ApiResponse<List<PublicFeedbackVO>> listFeedback() {
        AuthUserSession currentUser = requireCurrentUser();
        ensureFeedbackReader(currentUser);
        return ApiResponse.ok(feedbackRecords.stream()
                .filter((record) -> feedbackVisibleTo(record, currentUser))
                .sorted(Comparator.comparing(PublicFeedbackRecord::id).reversed())
                .map(this::toVO)
                .toList());
    }

    @PatchMapping("/api/feedback/{id}")
    public ApiResponse<PublicFeedbackVO> handleFeedback(
            @PathVariable Long id,
            @Valid @RequestBody PublicFeedbackHandleRequest request
    ) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureFeedbackHandler(currentUser);

        PublicFeedbackRecord record = findRecord(id);
        if (!feedbackVisibleTo(record, currentUser)) {
            throw new ForbiddenException("\u5f53\u524d\u8d26\u53f7\u65e0\u6743\u5904\u7406\u8be5\u53cd\u9988\u3002");
        }

        String status = normalizeBlank(request == null ? null : request.status());
        if (!HANDLE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("\u8bf7\u9009\u62e9\u53cd\u9988\u5904\u7406\u72b6\u6001\u3002");
        }
        String handleResult = normalize(request == null ? null : request.handleResult(), "");
        if ("CLOSED".equals(status) && handleResult.length() < 10) {
            throw new IllegalArgumentException("\u5173\u95ed\u53cd\u9988\u65f6\u9700\u586b\u5199\u4e0d\u5c11\u4e8e 10 \u4e2a\u5b57\u7684\u5904\u7406\u7ed3\u679c\u3002");
        }

        record.setStatus(status);
        record.setHandleResult(handleResult);
        record.setHandledAt(formatDisplayTime(LocalDateTime.now()));
        record.setHandlerName(normalize(currentUser.realName(), currentUser.username()));
        return ApiResponse.ok("\u53cd\u9988\u5904\u7406\u5df2\u4fdd\u5b58", toVO(record));
    }

    private PublicFeedbackRecord validateAndCreateRecord(
            PublicFeedbackRequest request,
            HttpServletRequest servletRequest
    ) {
        if (request == null) {
            throw new IllegalArgumentException("\u53cd\u9988\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a");
        }

        String content = normalizeBlank(request.content());
        if (content == null) {
            throw new IllegalArgumentException("\u53cd\u9988\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (content.length() < 10) {
            throw new IllegalArgumentException("\u53cd\u9988\u5185\u5bb9\u4e0d\u80fd\u5c11\u4e8e 10 \u4e2a\u5b57");
        }
        if (content.length() > 300) {
            throw new IllegalArgumentException("\u53cd\u9988\u5185\u5bb9\u4e0d\u80fd\u8d85\u8fc7 300 \u4e2a\u5b57");
        }

        String feedbackType = normalizeBlank(request.feedbackType());
        if (!SUPPORTED_TYPES.contains(feedbackType)) {
            feedbackType = TYPE_OTHER;
        }

        String receivedAt = LocalDateTime.now().toString();
        BatchSnapshot batchSnapshot = resolveBatchSnapshot(request);
        return new PublicFeedbackRecord(
                feedbackIdSequence.incrementAndGet(),
                normalize(batchSnapshot.productName(), normalize(request.productName(), UNKNOWN_TEXT)),
                normalize(batchSnapshot.batchNo(), normalize(request.batchNo(), UNKNOWN_TEXT)),
                normalize(batchSnapshot.traceCode(), normalize(request.traceCode(), UNKNOWN_TEXT)),
                feedbackType,
                normalize(request.contact(), ""),
                content,
                normalize(request.createdAt(), receivedAt),
                receivedAt,
                batchSnapshot.companyId(),
                batchSnapshot.companyName(),
                clientIp(servletRequest),
                normalize(servletRequest.getHeader("User-Agent"), ""),
                "PENDING",
                "",
                "",
                ""
        );
    }

    private PublicFeedbackRecord findRecord(Long id) {
        return feedbackRecords.stream()
                .filter((record) -> Objects.equals(record.id(), id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("\u53cd\u9988\u8bb0\u5f55\u4e0d\u5b58\u5728\u3002"));
    }

    private PublicFeedbackVO toVO(PublicFeedbackRecord record) {
        return new PublicFeedbackVO(
                record.id(),
                record.productName(),
                record.batchNo(),
                record.traceCode(),
                record.feedbackType(),
                record.contact(),
                record.content(),
                record.status(),
                record.handleResult(),
                record.handlerName(),
                record.handledAt(),
                record.createdAt(),
                record.receivedAt(),
                record.companyName()
        );
    }

    private BatchSnapshot resolveBatchSnapshot(PublicFeedbackRequest request) {
        TraceBatchPO batchPO = null;
        QrCodePO qrCodePO = null;
        String traceCode = normalizeBlank(request.traceCode());
        if (traceCode != null && !UNKNOWN_TEXT.equals(traceCode)) {
            qrCodePO = qrCodeMapper.selectOne(new LambdaQueryWrapper<QrCodePO>()
                    .eq(QrCodePO::getQrToken, traceCode)
                    .last("limit 1"));
            if (qrCodePO != null && qrCodePO.getBatchId() != null) {
                batchPO = traceBatchMapper.selectById(qrCodePO.getBatchId());
            }
        }
        if (batchPO == null) {
            String batchNo = normalizeBlank(request.batchNo());
            if (batchNo != null && !UNKNOWN_TEXT.equals(batchNo)) {
                batchPO = traceBatchMapper.selectOne(new LambdaQueryWrapper<TraceBatchPO>()
                        .eq(TraceBatchPO::getBatchCode, batchNo)
                        .last("limit 1"));
            }
        }
        if (batchPO == null) {
            return new BatchSnapshot(null, "", "", "", traceCode);
        }
        OrgCompanyPO companyPO = batchPO.getCompanyId() == null ? null : orgCompanyMapper.selectById(batchPO.getCompanyId());
        BaseProductPO productPO = batchPO.getProductId() == null ? null : baseProductMapper.selectById(batchPO.getProductId());
        return new BatchSnapshot(
                batchPO.getCompanyId(),
                normalize(companyPO == null ? null : companyPO.getName(), ""),
                normalize(productPO == null ? null : productPO.getName(), ""),
                normalize(batchPO.getBatchCode(), ""),
                normalize(qrCodePO == null ? traceCode : qrCodePO.getQrToken(), "")
        );
    }

    private boolean feedbackVisibleTo(PublicFeedbackRecord record, AuthUserSession currentUser) {
        if (isPlatformAdmin(currentUser) || isRegulator(currentUser)) {
            return true;
        }
        if (isEnterpriseAdmin(currentUser)) {
            return record.companyId() != null && Objects.equals(record.companyId(), currentUser.companyId());
        }
        return false;
    }

    private void ensureFeedbackReader(AuthUserSession currentUser) {
        if (!(isPlatformAdmin(currentUser) || isEnterpriseAdmin(currentUser) || isRegulator(currentUser))) {
            throw new ForbiddenException("\u5f53\u524d\u8d26\u53f7\u65e0\u6743\u67e5\u770b\u53cd\u9988\u3002");
        }
    }

    private void ensureFeedbackHandler(AuthUserSession currentUser) {
        if (!(isPlatformAdmin(currentUser) || isEnterpriseAdmin(currentUser))) {
            throw new ForbiddenException("\u5f53\u524d\u8d26\u53f7\u65e0\u6743\u5904\u7406\u53cd\u9988\u3002");
        }
    }

    private AuthUserSession requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserSession userSession) {
            return userSession;
        }
        throw new UnauthorizedException("\u5f53\u524d\u767b\u5f55\u72b6\u6001\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55\u540e\u518d\u8bd5\u3002");
    }

    private boolean isPlatformAdmin(AuthUserSession user) {
        return "PLATFORM_ADMIN".equals(user.roleCode());
    }

    private boolean isEnterpriseAdmin(AuthUserSession user) {
        return "ENTERPRISE_ADMIN".equals(user.roleCode());
    }

    private boolean isRegulator(AuthUserSession user) {
        return "REGULATOR".equals(user.roleCode());
    }

    private String normalize(String value, String fallback) {
        String text = normalizeBlank(value);
        return text == null ? fallback : text;
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String formatDisplayTime(LocalDateTime time) {
        return time == null ? "" : DISPLAY_TIME_FORMATTER.format(time);
    }

    private record BatchSnapshot(
            Long companyId,
            String companyName,
            String productName,
            String batchNo,
            String traceCode
    ) {
    }

    private static final class PublicFeedbackRecord {
        private final Long id;
        private final String productName;
        private final String batchNo;
        private final String traceCode;
        private final String feedbackType;
        private final String contact;
        private final String content;
        private final String createdAt;
        private final String receivedAt;
        private final Long companyId;
        private final String companyName;
        private final String ip;
        private final String userAgent;
        private String status;
        private String handleResult;
        private String handlerName;
        private String handledAt;

        private PublicFeedbackRecord(
                Long id,
                String productName,
                String batchNo,
                String traceCode,
                String feedbackType,
                String contact,
                String content,
                String createdAt,
                String receivedAt,
                Long companyId,
                String companyName,
                String ip,
                String userAgent,
                String status,
                String handleResult,
                String handlerName,
                String handledAt
        ) {
            this.id = id;
            this.productName = productName;
            this.batchNo = batchNo;
            this.traceCode = traceCode;
            this.feedbackType = feedbackType;
            this.contact = contact;
            this.content = content;
            this.createdAt = createdAt;
            this.receivedAt = receivedAt;
            this.companyId = companyId;
            this.companyName = companyName;
            this.ip = ip;
            this.userAgent = userAgent;
            this.status = status;
            this.handleResult = handleResult;
            this.handlerName = handlerName;
            this.handledAt = handledAt;
        }

        public Long id() { return id; }
        public String productName() { return productName; }
        public String batchNo() { return batchNo; }
        public String traceCode() { return traceCode; }
        public String feedbackType() { return feedbackType; }
        public String contact() { return contact; }
        public String content() { return content; }
        public String createdAt() { return createdAt; }
        public String receivedAt() { return receivedAt; }
        public Long companyId() { return companyId; }
        public String companyName() { return companyName; }
        public String status() { return status; }
        public String handleResult() { return handleResult; }
        public String handlerName() { return handlerName; }
        public String handledAt() { return handledAt; }

        public void setStatus(String status) { this.status = status; }
        public void setHandleResult(String handleResult) { this.handleResult = handleResult; }
        public void setHandlerName(String handlerName) { this.handlerName = handlerName; }
        public void setHandledAt(String handledAt) { this.handledAt = handledAt; }

        @Override
        public String toString() {
            return "PublicFeedbackRecord{" +
                    "id=" + id +
                    ", productName='" + productName + '\'' +
                    ", batchNo='" + batchNo + '\'' +
                    ", traceCode='" + traceCode + '\'' +
                    ", feedbackType='" + feedbackType + '\'' +
                    ", content='" + content + '\'' +
                    ", status='" + status + '\'' +
                    '}';
        }
    }
}
