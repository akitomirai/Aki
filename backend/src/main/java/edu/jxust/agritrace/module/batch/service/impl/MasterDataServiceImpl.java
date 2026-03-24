package edu.jxust.agritrace.module.batch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    public MasterDataServiceImpl(
            OrgCompanyMapper orgCompanyMapper,
            BaseProductMapper baseProductMapper,
            TraceBatchMapper traceBatchMapper
    ) {
        this.orgCompanyMapper = orgCompanyMapper;
        this.baseProductMapper = baseProductMapper;
        this.traceBatchMapper = traceBatchMapper;
    }

    @Override
    public List<CompanyAdminVO> listCompanies(CompanyListQueryRequest request) {
        LambdaQueryWrapper<OrgCompanyPO> wrapper = new LambdaQueryWrapper<OrgCompanyPO>()
                .orderByAsc(OrgCompanyPO::getName)
                .orderByAsc(OrgCompanyPO::getId);
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
        return toCompanyAdminVO(findCompanyRequired(companyId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyAdminVO createCompany(CompanySaveRequest request) {
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
        OrgCompanyPO companyPO = findCompanyRequired(companyId);
        ensureCompanyNameUnique(request.name(), companyId);
        ensureLicenseUnique(request.licenseNo(), companyId);

        companyPO.setName(request.name().trim());
        companyPO.setLicenseNo(trimToNull(request.licenseNo()));
        companyPO.setContact(request.contactPerson().trim());
        companyPO.setPhone(request.contactPhone().trim());
        companyPO.setAddress(request.address().trim());
        companyPO.setStatus(normalizeStatus(request.status()).name());
        orgCompanyMapper.updateById(companyPO);
        return toCompanyAdminVO(findCompanyRequired(companyId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyAdminVO updateCompanyStatus(Long companyId, StatusUpdateRequest request) {
        OrgCompanyPO companyPO = findCompanyRequired(companyId);
        companyPO.setStatus(normalizeStatus(request.status()).name());
        orgCompanyMapper.updateById(companyPO);
        return toCompanyAdminVO(findCompanyRequired(companyId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCompany(Long companyId) {
        OrgCompanyPO companyPO = findCompanyRequired(companyId);
        long productCount = countProductsByCompany(companyId);
        long batchCount = countBatchesByCompany(companyId);
        if (productCount > 0 || batchCount > 0) {
            throw new IllegalArgumentException(
                    "company cannot be deleted because it is still referenced by "
                            + productCount + " products and " + batchCount + " batches"
            );
        }
        orgCompanyMapper.deleteById(companyPO.getId());
    }

    @Override
    public List<CompanyOptionVO> listCompanyOptions(String keyword) {
        LambdaQueryWrapper<OrgCompanyPO> wrapper = new LambdaQueryWrapper<OrgCompanyPO>()
                .eq(OrgCompanyPO::getStatus, MasterDataStatus.ENABLED.name())
                .orderByAsc(OrgCompanyPO::getName)
                .orderByAsc(OrgCompanyPO::getId);
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
        LambdaQueryWrapper<BaseProductPO> wrapper = new LambdaQueryWrapper<BaseProductPO>()
                .orderByAsc(BaseProductPO::getName)
                .orderByAsc(BaseProductPO::getId);
        if (request != null) {
            if (request.getCompanyId() != null) {
                wrapper.eq(BaseProductPO::getCompanyId, request.getCompanyId());
            }
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
        return toProductAdminVO(findProductRequired(productId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductAdminVO createProduct(ProductSaveRequest request) {
        OrgCompanyPO company = findCompanyRequired(request.companyId());
        if (!normalizeStatus(company.getStatus()).selectable()) {
            throw new IllegalArgumentException("selected company is not available");
        }
        ensureProductUnique(request.companyId(), request.productName(), request.productCode(), null);

        BaseProductPO productPO = new BaseProductPO();
        productPO.setCompanyId(request.companyId());
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
        BaseProductPO productPO = findProductRequired(productId);
        OrgCompanyPO company = findCompanyRequired(request.companyId());
        if (!normalizeStatus(company.getStatus()).selectable()) {
            throw new IllegalArgumentException("selected company is not available");
        }
        ensureProductUnique(request.companyId(), request.productName(), request.productCode(), productId);

        productPO.setCompanyId(request.companyId());
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
        BaseProductPO productPO = findProductRequired(productId);
        productPO.setStatus(normalizeStatus(request.status()).name());
        baseProductMapper.updateById(productPO);
        return toProductAdminVO(findProductRequired(productId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        BaseProductPO productPO = findProductRequired(productId);
        long batchCount = countBatchesByProduct(productId);
        if (batchCount > 0) {
            throw new IllegalArgumentException(
                    "product cannot be deleted because it is still referenced by " + batchCount + " batches"
            );
        }
        baseProductMapper.deleteById(productPO.getId());
    }

    @Override
    public List<ProductOptionVO> listProductOptions(Long companyId, String keyword) {
        LambdaQueryWrapper<BaseProductPO> wrapper = new LambdaQueryWrapper<BaseProductPO>()
                .eq(BaseProductPO::getStatus, MasterDataStatus.ENABLED.name())
                .orderByAsc(BaseProductPO::getName)
                .orderByAsc(BaseProductPO::getId);
        if (companyId != null) {
            wrapper.eq(BaseProductPO::getCompanyId, companyId);
        }
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

    private OrgCompanyPO findCompanyRequired(Long companyId) {
        OrgCompanyPO companyPO = orgCompanyMapper.selectById(companyId);
        if (companyPO == null) {
            throw new IllegalArgumentException("companyId does not exist");
        }
        return companyPO;
    }

    private BaseProductPO findProductRequired(Long productId) {
        BaseProductPO productPO = baseProductMapper.selectById(productId);
        if (productPO == null) {
            throw new IllegalArgumentException("productId does not exist");
        }
        return productPO;
    }

    private void ensureCompanyNameUnique(String name, Long ignoredId) {
        OrgCompanyPO existing = orgCompanyMapper.selectOne(new LambdaQueryWrapper<OrgCompanyPO>()
                .eq(OrgCompanyPO::getName, name.trim())
                .orderByAsc(OrgCompanyPO::getId)
                .last("limit 1"));
        if (existing != null && !Objects.equals(existing.getId(), ignoredId)) {
            throw new IllegalArgumentException("company name already exists");
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
            throw new IllegalArgumentException("licenseNo already exists");
        }
    }

    private void ensureProductUnique(Long companyId, String productName, String productCode, Long ignoredId) {
        BaseProductPO sameName = baseProductMapper.selectOne(new LambdaQueryWrapper<BaseProductPO>()
                .eq(BaseProductPO::getCompanyId, companyId)
                .eq(BaseProductPO::getName, productName.trim())
                .orderByAsc(BaseProductPO::getId)
                .last("limit 1"));
        if (sameName != null && !Objects.equals(sameName.getId(), ignoredId)) {
            throw new IllegalArgumentException("product name already exists in the selected company");
        }
        if (notBlank(productCode)) {
            BaseProductPO sameCode = baseProductMapper.selectOne(new LambdaQueryWrapper<BaseProductPO>()
                    .eq(BaseProductPO::getCompanyId, companyId)
                    .eq(BaseProductPO::getProductCode, productCode.trim())
                    .orderByAsc(BaseProductPO::getId)
                    .last("limit 1"));
            if (sameCode != null && !Objects.equals(sameCode.getId(), ignoredId)) {
                throw new IllegalArgumentException("productCode already exists in the selected company");
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
