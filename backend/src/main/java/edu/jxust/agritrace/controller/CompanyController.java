package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.api.ApiResponse;
import edu.jxust.agritrace.module.batch.dto.CompanyListQueryRequest;
import edu.jxust.agritrace.module.batch.dto.CompanySaveRequest;
import edu.jxust.agritrace.module.batch.dto.StatusUpdateRequest;
import edu.jxust.agritrace.module.batch.service.MasterDataService;
import edu.jxust.agritrace.module.batch.vo.CompanyAdminVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final MasterDataService masterDataService;

    public CompanyController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping
    public ApiResponse<List<CompanyAdminVO>> listCompanies(@ModelAttribute CompanyListQueryRequest request) {
        return ApiResponse.ok(masterDataService.listCompanies(request));
    }

    @GetMapping("/{companyId}")
    public ApiResponse<CompanyAdminVO> getCompany(@PathVariable Long companyId) {
        return ApiResponse.ok(masterDataService.getCompany(companyId));
    }

    @PostMapping
    public ApiResponse<CompanyAdminVO> createCompany(@Valid @RequestBody CompanySaveRequest request) {
        return ApiResponse.ok("Company created.", masterDataService.createCompany(request));
    }

    @PatchMapping("/{companyId}")
    public ApiResponse<CompanyAdminVO> updateCompany(@PathVariable Long companyId, @Valid @RequestBody CompanySaveRequest request) {
        return ApiResponse.ok("Company updated.", masterDataService.updateCompany(companyId, request));
    }

    @PostMapping("/{companyId}/status")
    public ApiResponse<CompanyAdminVO> updateCompanyStatus(@PathVariable Long companyId, @Valid @RequestBody StatusUpdateRequest request) {
        return ApiResponse.ok("Company status updated.", masterDataService.updateCompanyStatus(companyId, request));
    }

    @DeleteMapping("/{companyId}")
    public ApiResponse<Void> deleteCompany(@PathVariable Long companyId) {
        masterDataService.deleteCompany(companyId);
        return ApiResponse.ok("Company deleted.", null);
    }
}
