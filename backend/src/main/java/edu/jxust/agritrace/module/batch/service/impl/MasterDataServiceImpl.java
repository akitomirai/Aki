package edu.jxust.agritrace.module.batch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.common.exception.ForbiddenException;
import edu.jxust.agritrace.common.exception.UnauthorizedException;
import edu.jxust.agritrace.module.auth.model.AuthUserSession;
import edu.jxust.agritrace.module.batch.dto.CompanyListQueryRequest;
import edu.jxust.agritrace.module.batch.dto.CompanySaveRequest;
import edu.jxust.agritrace.module.batch.dto.ProductListQueryRequest;
import edu.jxust.agritrace.module.batch.dto.ProductSaveRequest;
import edu.jxust.agritrace.module.batch.dto.StatusUpdateRequest;
import edu.jxust.agritrace.module.batch.entity.MasterDataStatus;
import edu.jxust.agritrace.module.batch.mapper.BaseProductMapper;
import edu.jxust.agritrace.module.batch.mapper.OrgCompanyMapper;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.batch.mapper.po.BaseProductPO;
import edu.jxust.agritrace.module.batch.mapper.po.OrgCompanyPO;
import edu.jxust.agritrace.module.batch.mapper.po.TraceBatchPO;
import edu.jxust.agritrace.module.batch.service.MasterDataService;
import edu.jxust.agritrace.module.batch.vo.CompanyAdminVO;
import edu.jxust.agritrace.module.batch.vo.CompanyOptionVO;
import edu.jxust.agritrace.module.batch.vo.ProductAdminVO;
import edu.jxust.agritrace.module.batch.vo.ProductOptionVO;
import edu.jxust.agritrace.module.log.dto.OperationLogRecord;
import edu.jxust.agritrace.module.log.service.OperationLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class MasterDataServiceImpl implements MasterDataService {

    private final OrgCompanyMapper orgCompanyMapper;
    private final BaseProductMapper baseProductMapper;
    private final TraceBatchMapper traceBatchMapper;
    private final OperationLogService operationLogService;

    public MasterDataServiceImpl(
            OrgCompanyMapper orgCompanyMapper,
            BaseProductMapper baseProductMapper,
            TraceBatchMapper traceBatchMapper,
            OperationLogService operationLogService
    ) {
        this.orgCompanyMapper = orgCompanyMapper;
        this.baseProductMapper = baseProductMapper;
        this.traceBatchMapper = traceBatchMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<CompanyAdminVO> listCompanies(CompanyListQueryRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureCompanyReader(currentUser);

        LambdaQueryWrapper<OrgCompanyPO> wrapper = new LambdaQueryWrapper<OrgCompanyPO>()
                .orderByAsc(OrgCompanyPO::getName)
                .orderByAsc(OrgCompanyPO::getId);

        if (isEnterpriseAdmin(currentUser)) {
            wrapper.eq(OrgCompanyPO::getId, requireEnterpriseCompanyId(currentUser, "当前账号未绑定企业，不能查看企业资料。"));
        }

        if (request != null) {
            if (notBlank(request.getKeyword())) {
                String keyword = request.getKeyword().trim();
                wrapper.and(item -> item
                        .like(OrgCompanyPO::getName, keyword)
                        .or()
                        .like(OrgCompanyPO::getContact, keyword)
                        .or()
                        .like(OrgCompanyPO::getPhone, keyword)
                        .or()
                        .like(OrgCompanyPO::getAddress, keyword));
            }
            if (notBlank(request.getStatus())) {
                wrapper.eq(OrgCompanyPO::getStatus, normalizeStatus(request.getStatus()).name());
            }
        }

        return orgCompanyMapper.selectList(wrapper).stream()
                .map(this::toCompanyAdminVO)
                .toList();
    }

    @Override
    public CompanyAdminVO getCompany(Long companyId) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureCompanyReader(currentUser);

        OrgCompanyPO companyPO = findCompanyRequired(companyId);
        ensureCompanyScope(currentUser, companyPO, "你只能查看和维护本企业资料。");
        return toCompanyAdminVO(companyPO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyAdminVO createCompany(CompanySaveRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensurePlatformCompanyWriter(currentUser, "企业管理员不能新建企业资料。");

        ensureCompanyNameUnique(request.name(), null);
        ensureLicenseUnique(request.licenseNo(), null);

        OrgCompanyPO companyPO = new OrgCompanyPO();
        companyPO.setName(request.name().trim());
        companyPO.setLicenseNo(trimToNull(request.licenseNo()));
        companyPO.setContact(request.contactPerson().trim());
        companyPO.setPhone(request.contactPhone().trim());
        companyPO.setAddress(request.address().trim());
        companyPO.setStatus(normalizeStatus(request.status()).name());
        orgCompanyMapper.insert(companyPO);
        return toCompanyAdminVO(findCompanyRequired(companyPO.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyAdminVO updateCompany(Long companyId, CompanySaveRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        OrgCompanyPO companyPO = findCompanyRequired(companyId);
        ensureCompanyEditor(currentUser, companyPO);

        MasterDataStatus currentStatus = normalizeStatus(companyPO.getStatus());
        MasterDataStatus nextStatus = currentStatus;
        if (isPlatformAdmin(currentUser)) {
            nextStatus = normalizeStatus(request.status());
        } else if (notBlank(request.status()) && normalizeStatus(request.status()) != currentStatus) {
            denyCompanyAccess(currentUser, companyPO, "企业管理员不能修改企业状态。");
        }

        ensureCompanyNameUnique(request.name(), companyId);
        ensureLicenseUnique(request.licenseNo(), companyId);

        companyPO.setName(request.name().trim());
        companyPO.setLicenseNo(trimToNull(request.licenseNo()));
        companyPO.setContact(request.contactPerson().trim());
        companyPO.setPhone(request.contactPhone().trim());
        companyPO.setAddress(request.address().trim());
        companyPO.setStatus(nextStatus.name());
        orgCompanyMapper.updateById(companyPO);
        return toCompanyAdminVO(findCompanyRequired(companyId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyAdminVO updateCompanyStatus(Long companyId, StatusUpdateRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        OrgCompanyPO companyPO = findCompanyRequired(companyId);
        ensurePlatformCompanyWriter(currentUser, "企业管理员不能修改企业状态。");

        companyPO.setStatus(normalizeStatus(request.status()).name());
        orgCompanyMapper.updateById(companyPO);
        return toCompanyAdminVO(findCompanyRequired(companyId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCompany(Long companyId) {
        AuthUserSession currentUser = requireCurrentUser();
        OrgCompanyPO companyPO = findCompanyRequired(companyId);
        ensurePlatformCompanyWriter(currentUser, "企业管理员不能删除企业资料。");

        long productCount = countProductsByCompany(companyId);
        long batchCount = countBatchesByCompany(companyId);
        if (productCount > 0 || batchCount > 0) {
            throw new IllegalArgumentException("该企业已关联产品或批次，暂不能删除。");
        }
        orgCompanyMapper.deleteById(companyPO.getId());
    }

    @Override
    public List<CompanyOptionVO> listCompanyOptions(String keyword) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureCompanyReader(currentUser);

        LambdaQueryWrapper<OrgCompanyPO> wrapper = new LambdaQueryWrapper<OrgCompanyPO>()
                .eq(OrgCompanyPO::getStatus, MasterDataStatus.ENABLED.name())
                .orderByAsc(OrgCompanyPO::getName)
                .orderByAsc(OrgCompanyPO::getId);

        if (isEnterpriseAdmin(currentUser)) {
            wrapper.eq(OrgCompanyPO::getId, requireEnterpriseCompanyId(currentUser, "当前账号未绑定企业，不能查看企业资料。"));
        }

        if (notBlank(keyword)) {
            String value = keyword.trim();
            wrapper.and(item -> item
                    .like(OrgCompanyPO::getName, value)
                    .or()
                    .like(OrgCompanyPO::getLicenseNo, value));
        }

        return orgCompanyMapper.selectList(wrapper).stream()
                .map(company -> new CompanyOptionVO(
                        company.getId(),
                        company.getName(),
                        company.getLicenseNo(),
                        company.getAddress()
                ))
                .toList();
    }

    @Override
    public List<ProductAdminVO> listProducts(ProductListQueryRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureProductReader(currentUser);

        Long effectiveCompanyId = normalizeProductCompanyFilter(currentUser, request == null ? null : request.getCompanyId());

        LambdaQueryWrapper<BaseProductPO> wrapper = new LambdaQueryWrapper<BaseProductPO>()
                .eq(effectiveCompanyId != null, BaseProductPO::getCompanyId, effectiveCompanyId)
                .orderByAsc(BaseProductPO::getName)
                .orderByAsc(BaseProductPO::getId);

        if (request != null) {
            if (notBlank(request.getKeyword())) {
                String keyword = request.getKeyword().trim();
                wrapper.and(item -> item
                        .like(BaseProductPO::getName, keyword)
                        .or()
                        .like(BaseProductPO::getProductCode, keyword)
                        .or()
                        .like(BaseProductPO::getCategory, keyword)
                        .or()
                        .like(BaseProductPO::getOriginPlace, keyword));
            }
            if (notBlank(request.getStatus())) {
                wrapper.eq(BaseProductPO::getStatus, normalizeStatus(request.getStatus()).name());
            }
        }

        return baseProductMapper.selectList(wrapper).stream()
                .map(this::toProductAdminVO)
                .toList();
    }

    @Override
    public ProductAdminVO getProduct(Long productId) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureProductReader(currentUser);

        BaseProductPO productPO = findProductRequired(productId);
        ensureProductScope(currentUser, productPO, "你只能查看本企业产品。");
        return toProductAdminVO(productPO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductAdminVO createProduct(ProductSaveRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureProductWriter(currentUser);

        Long targetCompanyId = normalizeWritableProductCompanyId(currentUser, request.companyId(), "你只能创建本企业产品，不能挂到其他企业。");
        OrgCompanyPO companyPO = findCompanyRequired(targetCompanyId);
        if (!normalizeStatus(companyPO.getStatus()).selectable()) {
            throw new IllegalArgumentException("当前企业状态不可用，暂不能挂载产品。");
        }

        ensureProductUnique(targetCompanyId, request.productName(), request.productCode(), null);

        BaseProductPO productPO = new BaseProductPO();
        productPO.setCompanyId(targetCompanyId);
        productPO.setProductCode(trimToNull(request.productCode()));
        productPO.setName(request.productName().trim());
        productPO.setCategory(request.category().trim());
        productPO.setOriginPlace(request.originPlace().trim());
        productPO.setSpec(defaultValue(request.specification(), null));
        productPO.setUnit(defaultValue(request.unit(), null));
        productPO.setImageUrl(defaultValue(request.coverImage(), resolveProductImage(request.productName(), request.category())));
        productPO.setStatus(normalizeStatus(request.status()).name());
        baseProductMapper.insert(productPO);
        return toProductAdminVO(findProductRequired(productPO.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductAdminVO updateProduct(Long productId, ProductSaveRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureProductWriter(currentUser);

        BaseProductPO productPO = findProductRequired(productId);
        ensureProductScope(currentUser, productPO, "你只能维护本企业产品。");

        Long targetCompanyId = normalizeWritableProductCompanyId(currentUser, request.companyId(), "你只能维护本企业产品，不能改挂到其他企业。");
        if (isEnterpriseAdmin(currentUser) && !Objects.equals(targetCompanyId, productPO.getCompanyId())) {
            denyProductAccess(currentUser, productPO, "你只能维护本企业产品，不能改挂到其他企业。");
        }

        OrgCompanyPO companyPO = findCompanyRequired(targetCompanyId);
        if (!normalizeStatus(companyPO.getStatus()).selectable()) {
            throw new IllegalArgumentException("当前企业状态不可用，暂不能挂载产品。");
        }

        ensureProductUnique(targetCompanyId, request.productName(), request.productCode(), productId);

        productPO.setCompanyId(targetCompanyId);
        productPO.setProductCode(trimToNull(request.productCode()));
        productPO.setName(request.productName().trim());
        productPO.setCategory(request.category().trim());
        productPO.setOriginPlace(request.originPlace().trim());
        productPO.setSpec(defaultValue(request.specification(), null));
        productPO.setUnit(defaultValue(request.unit(), null));
        productPO.setImageUrl(defaultValue(request.coverImage(), resolveProductImage(request.productName(), request.category())));
        productPO.setStatus(normalizeStatus(request.status()).name());
        baseProductMapper.updateById(productPO);
        return toProductAdminVO(findProductRequired(productId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductAdminVO updateProductStatus(Long productId, StatusUpdateRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureProductWriter(currentUser);

        BaseProductPO productPO = findProductRequired(productId);
        ensureProductScope(currentUser, productPO, "你只能维护本企业产品。");

        productPO.setStatus(normalizeStatus(request.status()).name());
        baseProductMapper.updateById(productPO);
        return toProductAdminVO(findProductRequired(productId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureProductWriter(currentUser);

        BaseProductPO productPO = findProductRequired(productId);
        ensureProductScope(currentUser, productPO, "你只能维护本企业产品。");

        long batchCount = countBatchesByProduct(productId);
        if (batchCount > 0) {
            throw new IllegalArgumentException("该产品已关联批次，暂不能删除。");
        }
        baseProductMapper.deleteById(productPO.getId());
    }

    @Override
    public List<ProductOptionVO> listProductOptions(Long companyId, String keyword) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureProductReader(currentUser);

        Long effectiveCompanyId = normalizeProductCompanyFilter(currentUser, companyId);

        LambdaQueryWrapper<BaseProductPO> wrapper = new LambdaQueryWrapper<BaseProductPO>()
                .eq(BaseProductPO::getStatus, MasterDataStatus.ENABLED.name())
                .eq(effectiveCompanyId != null, BaseProductPO::getCompanyId, effectiveCompanyId)
                .orderByAsc(BaseProductPO::getName)
                .orderByAsc(BaseProductPO::getId);

        if (notBlank(keyword)) {
            String value = keyword.trim();
            wrapper.and(item -> item
                    .like(BaseProductPO::getName, value)
                    .or()
                    .like(BaseProductPO::getCategory, value)
                    .or()
                    .like(BaseProductPO::getProductCode, value));
        }

        return baseProductMapper.selectList(wrapper).stream()
                .map(product -> new ProductOptionVO(
                        product.getId(),
                        product.getCompanyId(),
                        product.getName(),
                        product.getCategory(),
                        product.getOriginPlace(),
                        defaultValue(product.getSpec(), null),
                        defaultValue(product.getUnit(), null),
                        defaultValue(product.getImageUrl(), resolveProductImage(product.getName(), product.getCategory()))
                ))
                .toList();
    }

    private CompanyAdminVO toCompanyAdminVO(OrgCompanyPO companyPO) {
        MasterDataStatus status = normalizeStatus(companyPO.getStatus());
        long productCount = countProductsByCompany(companyPO.getId());
        long batchCount = countBatchesByCompany(companyPO.getId());
        return new CompanyAdminVO(
                companyPO.getId(),
                companyPO.getName(),
                companyPO.getLicenseNo(),
                companyPO.getContact(),
                companyPO.getPhone(),
                companyPO.getAddress(),
                status.name(),
                status.label(),
                productCount,
                batchCount,
                productCount == 0 && batchCount == 0
        );
    }

    private ProductAdminVO toProductAdminVO(BaseProductPO productPO) {
        OrgCompanyPO companyPO = findCompanyRequired(productPO.getCompanyId());
        MasterDataStatus status = normalizeStatus(productPO.getStatus());
        long batchCount = countBatchesByProduct(productPO.getId());
        return new ProductAdminVO(
                productPO.getId(),
                productPO.getCompanyId(),
                companyPO.getName(),
                productPO.getName(),
                productPO.getProductCode(),
                productPO.getCategory(),
                productPO.getOriginPlace(),
                defaultValue(productPO.getImageUrl(), resolveProductImage(productPO.getName(), productPO.getCategory())),
                productPO.getSpec(),
                productPO.getUnit(),
                status.name(),
                status.label(),
                batchCount,
                batchCount == 0
        );
    }

    private AuthUserSession requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUserSession userSession) {
            return userSession;
        }
        throw new UnauthorizedException("当前登录状态已失效，请重新登录后再试。");
    }

    private void ensureCompanyReader(AuthUserSession currentUser) {
        if (isPlatformAdmin(currentUser) || isEnterpriseAdmin(currentUser)) {
            return;
        }
        denyCompanyAccess(currentUser, null, "你没有查看企业资料的权限。");
    }

    private void ensureCompanyEditor(AuthUserSession currentUser, OrgCompanyPO companyPO) {
        if (isPlatformAdmin(currentUser)) {
            return;
        }
        if (isEnterpriseAdmin(currentUser)) {
            ensureCompanyScope(currentUser, companyPO, "你只能查看和维护本企业资料。");
            return;
        }
        denyCompanyAccess(currentUser, companyPO, "你没有维护企业资料的权限。");
    }

    private void ensurePlatformCompanyWriter(AuthUserSession currentUser, String message) {
        if (isPlatformAdmin(currentUser)) {
            return;
        }
        denyCompanyAccess(currentUser, null, message);
    }

    private void ensureCompanyScope(AuthUserSession currentUser, OrgCompanyPO companyPO, String message) {
        if (!isEnterpriseAdmin(currentUser)) {
            return;
        }
        Long currentCompanyId = requireEnterpriseCompanyId(currentUser, "当前账号未绑定企业，不能查看企业资料。");
        if (!Objects.equals(currentCompanyId, companyPO.getId())) {
            denyCompanyAccess(currentUser, companyPO, message);
        }
    }

    private void ensureProductReader(AuthUserSession currentUser) {
        if (isPlatformAdmin(currentUser) || isEnterpriseAdmin(currentUser)) {
            return;
        }
        denyProductAccess(currentUser, null, "你没有查看产品资料的权限。");
    }

    private void ensureProductWriter(AuthUserSession currentUser) {
        if (isPlatformAdmin(currentUser) || isEnterpriseAdmin(currentUser)) {
            return;
        }
        denyProductAccess(currentUser, null, "你没有维护产品资料的权限。");
    }

    private void ensureProductScope(AuthUserSession currentUser, BaseProductPO productPO, String message) {
        if (!isEnterpriseAdmin(currentUser)) {
            return;
        }
        Long currentCompanyId = requireEnterpriseCompanyId(currentUser, "当前账号未绑定企业，不能维护产品资料。");
        if (!Objects.equals(currentCompanyId, productPO.getCompanyId())) {
            denyProductAccess(currentUser, productPO, message);
        }
    }

    private Long normalizeProductCompanyFilter(AuthUserSession currentUser, Long requestedCompanyId) {
        if (!isEnterpriseAdmin(currentUser)) {
            return requestedCompanyId;
        }
        Long currentCompanyId = requireEnterpriseCompanyId(currentUser, "当前账号未绑定企业，不能查看产品资料。");
        if (requestedCompanyId != null && !Objects.equals(requestedCompanyId, currentCompanyId)) {
            denyProductAccess(currentUser, null, "你只能查看本企业产品。");
        }
        return currentCompanyId;
    }

    private Long normalizeWritableProductCompanyId(AuthUserSession currentUser, Long requestedCompanyId, String message) {
        if (!isEnterpriseAdmin(currentUser)) {
            return requestedCompanyId;
        }
        Long currentCompanyId = requireEnterpriseCompanyId(currentUser, "当前账号未绑定企业，不能维护产品资料。");
        if (requestedCompanyId == null || !Objects.equals(requestedCompanyId, currentCompanyId)) {
            denyProductAccess(currentUser, null, message);
        }
        return currentCompanyId;
    }

    private Long requireEnterpriseCompanyId(AuthUserSession currentUser, String message) {
        if (currentUser.companyId() != null) {
            return currentUser.companyId();
        }
        denyCompanyAccess(currentUser, null, message);
        return null;
    }

    private OrgCompanyPO findCompanyRequired(Long companyId) {
        OrgCompanyPO companyPO = orgCompanyMapper.selectById(companyId);
        if (companyPO == null) {
            throw new IllegalArgumentException("企业资料不存在或已被删除。");
        }
        return companyPO;
    }

    private BaseProductPO findProductRequired(Long productId) {
        BaseProductPO productPO = baseProductMapper.selectById(productId);
        if (productPO == null) {
            throw new IllegalArgumentException("产品资料不存在或已被删除。");
        }
        return productPO;
    }

    private void ensureCompanyNameUnique(String name, Long ignoredId) {
        OrgCompanyPO existing = orgCompanyMapper.selectOne(new LambdaQueryWrapper<OrgCompanyPO>()
                .eq(OrgCompanyPO::getName, name.trim())
                .orderByAsc(OrgCompanyPO::getId)
                .last("limit 1"));
        if (existing != null && !Objects.equals(existing.getId(), ignoredId)) {
            throw new IllegalArgumentException("企业名称已存在，请检查后重试。");
        }
    }

    private void ensureLicenseUnique(String licenseNo, Long ignoredId) {
        if (!notBlank(licenseNo)) {
            return;
        }
        OrgCompanyPO existing = orgCompanyMapper.selectOne(new LambdaQueryWrapper<OrgCompanyPO>()
                .eq(OrgCompanyPO::getLicenseNo, licenseNo.trim())
                .orderByAsc(OrgCompanyPO::getId)
                .last("limit 1"));
        if (existing != null && !Objects.equals(existing.getId(), ignoredId)) {
            throw new IllegalArgumentException("许可证号已存在，请检查后重试。");
        }
    }

    private void ensureProductUnique(Long companyId, String productName, String productCode, Long ignoredId) {
        BaseProductPO sameName = baseProductMapper.selectOne(new LambdaQueryWrapper<BaseProductPO>()
                .eq(BaseProductPO::getCompanyId, companyId)
                .eq(BaseProductPO::getName, productName.trim())
                .orderByAsc(BaseProductPO::getId)
                .last("limit 1"));
        if (sameName != null && !Objects.equals(sameName.getId(), ignoredId)) {
            throw new IllegalArgumentException("本企业下已存在同名产品。");
        }
        if (notBlank(productCode)) {
            BaseProductPO sameCode = baseProductMapper.selectOne(new LambdaQueryWrapper<BaseProductPO>()
                    .eq(BaseProductPO::getCompanyId, companyId)
                    .eq(BaseProductPO::getProductCode, productCode.trim())
                    .orderByAsc(BaseProductPO::getId)
                    .last("limit 1"));
            if (sameCode != null && !Objects.equals(sameCode.getId(), ignoredId)) {
                throw new IllegalArgumentException("本企业下已存在相同产品编码。");
            }
        }
    }

    private MasterDataStatus normalizeStatus(String value) {
        return MasterDataStatus.fromCode(value);
    }

    private long countProductsByCompany(Long companyId) {
        return baseProductMapper.selectCount(new LambdaQueryWrapper<BaseProductPO>()
                .eq(BaseProductPO::getCompanyId, companyId));
    }

    private long countBatchesByCompany(Long companyId) {
        return traceBatchMapper.selectCount(new LambdaQueryWrapper<TraceBatchPO>()
                .eq(TraceBatchPO::getCompanyId, companyId));
    }

    private long countBatchesByProduct(Long productId) {
        return traceBatchMapper.selectCount(new LambdaQueryWrapper<TraceBatchPO>()
                .eq(TraceBatchPO::getProductId, productId));
    }

    private String resolveProductImage(String productName, String category) {
        String lowered = (defaultValue(productName, "") + " " + defaultValue(category, "")).toLowerCase(Locale.ROOT);
        if (lowered.contains("orange") || lowered.contains("fruit")) {
            return "/images/products/orange-batch.svg";
        }
        if (lowered.contains("tea")) {
            return "/images/products/green-tea-batch.svg";
        }
        return "/images/products/rice-batch.svg";
    }

    private void denyCompanyAccess(AuthUserSession currentUser, OrgCompanyPO companyPO, String message) {
        recordDeniedOperation(
                currentUser,
                "COMPANY_ACCESS_DENIED",
                "COMPANY",
                companyPO == null ? null : companyPO.getId(),
                companyPO == null ? null : companyPO.getName(),
                message
        );
        throw new ForbiddenException(message);
    }

    private void denyProductAccess(AuthUserSession currentUser, BaseProductPO productPO, String message) {
        recordDeniedOperation(
                currentUser,
                "PRODUCT_ACCESS_DENIED",
                "PRODUCT",
                productPO == null ? null : productPO.getId(),
                productPO == null ? null : productPO.getName(),
                message
        );
        throw new ForbiddenException(message);
    }

    private void recordDeniedOperation(
            AuthUserSession currentUser,
            String actionType,
            String targetType,
            Long targetId,
            String targetName,
            String summary
    ) {
        if (currentUser == null) {
            return;
        }
        operationLogService.record(new OperationLogRecord(
                currentUser.userId(),
                defaultValue(currentUser.realName(), defaultValue(currentUser.username(), "系统用户")),
                currentUser.roleCode(),
                currentUser.companyId(),
                actionType,
                targetType,
                targetId,
                targetName,
                "FAILED",
                summary
        ));
    }

    private boolean isPlatformAdmin(AuthUserSession currentUser) {
        return currentUser != null && "PLATFORM_ADMIN".equalsIgnoreCase(currentUser.roleCode());
    }

    private boolean isEnterpriseAdmin(AuthUserSession currentUser) {
        return currentUser != null && "ENTERPRISE_ADMIN".equalsIgnoreCase(currentUser.roleCode());
    }

    private String defaultValue(String value, String fallback) {
        return notBlank(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
