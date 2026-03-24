package edu.jxust.agritrace.module.batch.service;

import edu.jxust.agritrace.module.batch.dto.CompanyListQueryRequest;
import edu.jxust.agritrace.module.batch.dto.CompanySaveRequest;
import edu.jxust.agritrace.module.batch.dto.ProductListQueryRequest;
import edu.jxust.agritrace.module.batch.dto.ProductSaveRequest;
import edu.jxust.agritrace.module.batch.dto.StatusUpdateRequest;
import edu.jxust.agritrace.module.batch.vo.CompanyAdminVO;
import edu.jxust.agritrace.module.batch.vo.CompanyOptionVO;
import edu.jxust.agritrace.module.batch.vo.ProductAdminVO;
import edu.jxust.agritrace.module.batch.vo.ProductOptionVO;

import java.util.List;

public interface MasterDataService {

    List<CompanyAdminVO> listCompanies(CompanyListQueryRequest request);

    CompanyAdminVO getCompany(Long companyId);

    CompanyAdminVO createCompany(CompanySaveRequest request);

    CompanyAdminVO updateCompany(Long companyId, CompanySaveRequest request);

    CompanyAdminVO updateCompanyStatus(Long companyId, StatusUpdateRequest request);

    void deleteCompany(Long companyId);

    List<CompanyOptionVO> listCompanyOptions(String keyword);

    List<ProductAdminVO> listProducts(ProductListQueryRequest request);

    ProductAdminVO getProduct(Long productId);

    ProductAdminVO createProduct(ProductSaveRequest request);

    ProductAdminVO updateProduct(Long productId, ProductSaveRequest request);

    ProductAdminVO updateProductStatus(Long productId, StatusUpdateRequest request);

    void deleteProduct(Long productId);

    List<ProductOptionVO> listProductOptions(Long companyId, String keyword);
}
