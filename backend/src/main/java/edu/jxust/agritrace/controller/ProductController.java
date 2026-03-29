package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.api.ApiResponse;
import edu.jxust.agritrace.module.batch.dto.ProductListQueryRequest;
import edu.jxust.agritrace.module.batch.dto.ProductSaveRequest;
import edu.jxust.agritrace.module.batch.dto.StatusUpdateRequest;
import edu.jxust.agritrace.module.batch.service.MasterDataService;
import edu.jxust.agritrace.module.batch.vo.ProductAdminVO;
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
@RequestMapping("/api/products")
public class ProductController {

    private final MasterDataService masterDataService;

    public ProductController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping
    public ApiResponse<List<ProductAdminVO>> listProducts(@ModelAttribute ProductListQueryRequest request) {
        return ApiResponse.ok(masterDataService.listProducts(request));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductAdminVO> getProduct(@PathVariable Long productId) {
        return ApiResponse.ok(masterDataService.getProduct(productId));
    }

    @PostMapping
    public ApiResponse<ProductAdminVO> createProduct(@Valid @RequestBody ProductSaveRequest request) {
        return ApiResponse.ok("Product created.", masterDataService.createProduct(request));
    }

    @PatchMapping("/{productId}")
    public ApiResponse<ProductAdminVO> updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductSaveRequest request) {
        return ApiResponse.ok("Product updated.", masterDataService.updateProduct(productId, request));
    }

    @PostMapping("/{productId}/status")
    public ApiResponse<ProductAdminVO> updateProductStatus(@PathVariable Long productId, @Valid @RequestBody StatusUpdateRequest request) {
        return ApiResponse.ok("Product status updated.", masterDataService.updateProductStatus(productId, request));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long productId) {
        masterDataService.deleteProduct(productId);
        return ApiResponse.ok("Product deleted.", null);
    }
}
