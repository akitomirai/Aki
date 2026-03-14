package edu.jxust.agritrace.controller.platform;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.module.product.dto.ProductCreateDTO;
import edu.jxust.agritrace.module.product.dto.ProductUpdateDTO;
import edu.jxust.agritrace.module.product.service.ProductService;
import edu.jxust.agritrace.module.product.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "平台端产品管理")
@RestController
@RequestMapping("/api/platform/product")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ENTERPRISE_USER')")
public class ProductPlatformController {

    private final ProductService productService;

    public ProductPlatformController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "新增产品")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @OperationLogAnnotation(
            module = "PRODUCT",
            action = "CREATE_PRODUCT",
            targetType = "BASE_PRODUCT"
    )
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductCreateDTO dto) {
        Long id = productService.create(dto);
        return Result.ok(id);
    }

    @Operation(summary = "查询产品列表")
    @GetMapping("/list")
    public Result<List<ProductVO>> list() {
        if (SecurityUtils.isEnterpriseUser()) {
            return Result.ok(productService.listByCompanyId(SecurityUtils.getCompanyId()));
        }
        return Result.ok(productService.list());
    }

    @Operation(summary = "查询产品详情")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping("/detail/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    @Operation(summary = "修改产品")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @OperationLogAnnotation(
            module = "PRODUCT",
            action = "UPDATE_PRODUCT",
            targetType = "BASE_PRODUCT"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateDTO dto) {
        productService.update(id, dto);
        return Result.ok();
    }
}
