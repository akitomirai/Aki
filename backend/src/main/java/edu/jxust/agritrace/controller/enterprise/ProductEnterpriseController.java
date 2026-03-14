package edu.jxust.agritrace.controller.enterprise;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.product.service.ProductService;
import edu.jxust.agritrace.module.product.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 企业端-产品查询接口（最小兼容）
 */
@Tag(name = "企业端-产品管理")
@RestController
@RequestMapping("/api/enterprise/product")
@PreAuthorize("hasRole('ENTERPRISE_ADMIN') or hasRole('ADMIN')")
public class ProductEnterpriseController {

    private final ProductService productService;

    public ProductEnterpriseController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "查询产品列表")
    @GetMapping("/list")
    public Result<List<ProductVO>> list() {
        return Result.ok(productService.list());
    }

    @Operation(summary = "查询产品详情")
    @GetMapping("/detail/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }
}

