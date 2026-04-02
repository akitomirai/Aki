package edu.jxust.agritrace.module.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.common.exception.ForbiddenException;
import edu.jxust.agritrace.common.exception.UnauthorizedException;
import edu.jxust.agritrace.module.auth.model.AuthUserSession;
import edu.jxust.agritrace.module.batch.mapper.OrgCompanyMapper;
import edu.jxust.agritrace.module.batch.mapper.po.OrgCompanyPO;
import edu.jxust.agritrace.module.log.dto.OperationLogQueryRequest;
import edu.jxust.agritrace.module.log.dto.OperationLogRecord;
import edu.jxust.agritrace.module.log.mapper.OperationAuditLogMapper;
import edu.jxust.agritrace.module.log.mapper.po.OperationAuditLogPO;
import edu.jxust.agritrace.module.log.service.OperationLogService;
import edu.jxust.agritrace.module.log.service.support.OperationLogLabels;
import edu.jxust.agritrace.module.log.vo.OperationLogPageVO;
import edu.jxust.agritrace.module.log.vo.OperationLogVO;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class OperationLogServiceImpl implements OperationLogService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final OperationAuditLogMapper operationAuditLogMapper;
    private final OrgCompanyMapper orgCompanyMapper;
    private final ObjectProvider<OperationLogService> operationLogServiceProvider;

    public OperationLogServiceImpl(
            OperationAuditLogMapper operationAuditLogMapper,
            OrgCompanyMapper orgCompanyMapper,
            ObjectProvider<OperationLogService> operationLogServiceProvider
    ) {
        this.operationAuditLogMapper = operationAuditLogMapper;
        this.orgCompanyMapper = orgCompanyMapper;
        this.operationLogServiceProvider = operationLogServiceProvider;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void record(OperationLogRecord record) {
        if (record == null || !notBlank(record.actionType()) || !notBlank(record.result())) {
            return;
        }
        OperationAuditLogPO logPO = new OperationAuditLogPO();
        logPO.setOperatorUserId(record.operatorUserId());
        logPO.setOperatorName(trimToNull(record.operatorName()));
        logPO.setRoleCode(trimToNull(normalizeUpper(record.roleCode())));
        logPO.setCompanyId(record.companyId());
        logPO.setActionType(normalizeUpper(record.actionType()));
        logPO.setTargetType(defaultValue(normalizeUpper(record.targetType()), "SYSTEM"));
        logPO.setTargetId(record.targetId());
        logPO.setTargetName(trimToNull(record.targetName()));
        logPO.setResult(defaultValue(normalizeUpper(record.result()), "SUCCESS"));
        logPO.setSummary(trimToNull(record.summary()));
        logPO.setCreatedAt(LocalDateTime.now());
        operationAuditLogMapper.insert(logPO);
    }

    @Override
    public OperationLogPageVO listLogs(OperationLogQueryRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureLogReader(currentUser);

        Long effectiveCompanyId = normalizeCompanyFilter(currentUser, request == null ? null : request.getCompanyId());
        String actionType = normalizeUpper(request == null ? null : request.getActionType());
        String roleCode = normalizeUpper(request == null ? null : request.getRoleCode());
        String result = normalizeUpper(request == null ? null : request.getResult());
        LocalDateTime dateFrom = parseDateStart(request == null ? null : request.getDateFrom());
        LocalDateTime dateToExclusive = parseDateEndExclusive(request == null ? null : request.getDateTo());
        if (dateFrom != null && dateToExclusive != null && !dateFrom.isBefore(dateToExclusive)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期。");
        }

        LambdaQueryWrapper<OperationAuditLogPO> queryWrapper = new LambdaQueryWrapper<OperationAuditLogPO>()
                .eq(notBlank(actionType), OperationAuditLogPO::getActionType, actionType)
                .eq(notBlank(roleCode), OperationAuditLogPO::getRoleCode, roleCode)
                .eq(effectiveCompanyId != null, OperationAuditLogPO::getCompanyId, effectiveCompanyId)
                .eq(notBlank(result), OperationAuditLogPO::getResult, result)
                .ge(dateFrom != null, OperationAuditLogPO::getCreatedAt, dateFrom)
                .lt(dateToExclusive != null, OperationAuditLogPO::getCreatedAt, dateToExclusive)
                .orderByDesc(OperationAuditLogPO::getCreatedAt)
                .orderByDesc(OperationAuditLogPO::getId);

        if (notBlank(request == null ? null : request.getOperatorKeyword())) {
            queryWrapper.like(OperationAuditLogPO::getOperatorName, request.getOperatorKeyword().trim());
        }

        List<OperationAuditLogPO> allLogs = operationAuditLogMapper.selectList(queryWrapper);
        int page = normalizePage(request == null ? null : request.getPage());
        int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(allLogs.size(), fromIndex + pageSize);
        List<OperationAuditLogPO> pageItems = fromIndex >= allLogs.size() ? List.of() : allLogs.subList(fromIndex, toIndex);

        Map<Long, String> companyNameMap = buildCompanyNameMap(pageItems);
        List<OperationLogVO> items = pageItems.stream()
                .map(item -> toLogVO(item, companyNameMap))
                .toList();

        return new OperationLogPageVO(items, allLogs.size(), page, pageSize);
    }

    private OperationLogVO toLogVO(OperationAuditLogPO logPO, Map<Long, String> companyNameMap) {
        if (logPO == null) {
            return null;
        }
        String targetTypeLabel = OperationLogLabels.targetTypeLabel(logPO.getTargetType());
        return new OperationLogVO(
                logPO.getId(),
                formatDateTime(logPO.getCreatedAt()),
                logPO.getOperatorUserId(),
                defaultValue(logPO.getOperatorName(), "系统用户"),
                defaultValue(logPO.getRoleCode(), ""),
                OperationLogLabels.roleName(logPO.getRoleCode()),
                logPO.getCompanyId(),
                companyNameMap.getOrDefault(logPO.getCompanyId(), logPO.getCompanyId() == null ? "平台主管范围" : ""),
                defaultValue(logPO.getActionType(), ""),
                OperationLogLabels.actionLabel(logPO.getActionType()),
                defaultValue(logPO.getTargetType(), ""),
                targetTypeLabel,
                logPO.getTargetId(),
                defaultValue(logPO.getTargetName(), ""),
                buildTargetDisplay(targetTypeLabel, logPO.getTargetName(), logPO.getTargetId()),
                defaultValue(logPO.getResult(), "SUCCESS"),
                OperationLogLabels.resultLabel(logPO.getResult()),
                defaultValue(logPO.getSummary(), "")
        );
    }

    private Map<Long, String> buildCompanyNameMap(List<OperationAuditLogPO> logs) {
        Map<Long, String> companyNameMap = new LinkedHashMap<>();
        for (OperationAuditLogPO logPO : logs) {
            if (logPO == null || logPO.getCompanyId() == null || companyNameMap.containsKey(logPO.getCompanyId())) {
                continue;
            }
            OrgCompanyPO companyPO = orgCompanyMapper.selectById(logPO.getCompanyId());
            companyNameMap.put(logPO.getCompanyId(), companyPO == null ? "" : defaultValue(companyPO.getName(), ""));
        }
        return companyNameMap;
    }

    private String buildTargetDisplay(String targetTypeLabel, String targetName, Long targetId) {
        String normalizedTargetName = defaultValue(targetName, "");
        if (notBlank(normalizedTargetName) && targetId != null) {
            return targetTypeLabel + "：" + normalizedTargetName + "（ID " + targetId + "）";
        }
        if (notBlank(normalizedTargetName)) {
            return targetTypeLabel + "：" + normalizedTargetName;
        }
        if (targetId != null) {
            return targetTypeLabel + " ID " + targetId;
        }
        return targetTypeLabel;
    }

    private AuthUserSession requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserSession userSession) {
            return userSession;
        }
        throw new UnauthorizedException("当前登录状态已失效，请重新登录后再试。");
    }

    private void ensureLogReader(AuthUserSession currentUser) {
        if (isPlatformAdmin(currentUser) || isEnterpriseAdmin(currentUser)) {
            return;
        }
        denyLogAccess(currentUser, "你没有查看操作日志的权限。");
    }

    private Long normalizeCompanyFilter(AuthUserSession currentUser, Long companyId) {
        if (!isEnterpriseAdmin(currentUser)) {
            return companyId;
        }
        if (companyId != null && !Objects.equals(companyId, currentUser.companyId())) {
            denyLogAccess(currentUser, "你只能查看本企业日志。");
        }
        return currentUser.companyId();
    }

    private void denyLogAccess(AuthUserSession currentUser, String message) {
        if (currentUser != null) {
            OperationLogService operationLogService = operationLogServiceProvider.getIfAvailable();
            if (operationLogService != null) {
                operationLogService.record(new OperationLogRecord(
                        currentUser.userId(),
                        defaultValue(currentUser.realName(), defaultValue(currentUser.username(), "系统用户")),
                        currentUser.roleCode(),
                        currentUser.companyId(),
                        "LOG_ACCESS_DENIED",
                        "LOG",
                        null,
                        null,
                        "FAILED",
                        message
                ));
            }
        }
        throw new ForbiddenException(message);
    }

    private LocalDateTime parseDateStart(String value) {
        if (!notBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER).atStartOfDay();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("开始日期格式不正确，请使用 yyyy-MM-dd。");
        }
    }

    private LocalDateTime parseDateEndExclusive(String value) {
        if (!notBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER).plusDays(1).atStartOfDay();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("结束日期格式不正确，请使用 yyyy-MM-dd。");
        }
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private boolean isPlatformAdmin(AuthUserSession currentUser) {
        return currentUser != null && "PLATFORM_ADMIN".equalsIgnoreCase(currentUser.roleCode());
    }

    private boolean isEnterpriseAdmin(AuthUserSession currentUser) {
        return currentUser != null && "ENTERPRISE_ADMIN".equalsIgnoreCase(currentUser.roleCode());
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeUpper(String value) {
        return notBlank(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String trimToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }

    private String defaultValue(String value, String fallback) {
        return notBlank(value) ? value.trim() : fallback;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }
}
